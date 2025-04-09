package io.github.fate_grand_automata.scripts.entrypoints

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.ISubtitleNotifier
import io.github.lib_automata.EntryPoint
import io.github.lib_automata.ExitManager
import io.github.lib_automata.OcrService
import io.github.lib_automata.Translator
import io.github.lib_automata.Region
import io.github.lib_automata.ScriptAbortException
import io.github.lib_automata.dagger.ScriptScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ScriptScope
class AutoTranslate @Inject constructor(
    exitManager: ExitManager,
    api: IFgoAutomataApi,
    private val ocrService: OcrService,
    private val translator: Translator,
    private val subtitleNotifier: ISubtitleNotifier
) : EntryPoint(exitManager), IFgoAutomataApi by api { // Implement IFgoAutomataApi

    // --- Configuration (Needs to be moved to Prefs/UI) ---
    private val ocrRegion = Region(100, 50, 2360, 200)
    private val targetLanguage = "en"
    private val checkInterval = 500.milliseconds
    // --- End Configuration ---

    private var translationJob: Job? = null
    private var previousOcrText: String = ""

    private val scriptScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    class ExitException(override val message: String) : Exception(message)

    override fun script(): Nothing {
        subtitleNotifier.start()
        try {
            while (scriptScope.isActive) {
                exitManager.checkExitRequested()
                checkAndTranslateOcrRegion()
                checkInterval.wait(applyMultiplier = false)
            }
        } catch (e: ScriptAbortException) {
            throw ExitException("Script aborted")
        } catch (e: Exception) {
            // Log exception if logger was injected
            throw ExitException("Unexpected error: ${e.message}")
        } finally {
            translationJob?.cancel()
            subtitleNotifier.stop()
            scriptScope.cancel()
        }
        throw ScriptAbortException()
    }

    private fun checkAndTranslateOcrRegion() {
        // Use the getPattern() extension function available via IFgoAutomataApi
        // This handles getting the screenshot and cropping in image coordinates correctly.
        val regionOfInterestPattern = ocrRegion.getPattern(tag = "OCR_Translate") // [source: 2262, 2380]

        regionOfInterestPattern.use { pattern -> // Ensure the pattern is closed
            val text = ocrService.detectText(pattern).trim()

            if (text.isNotBlank() && text != previousOcrText) {
                previousOcrText = text
                translationJob?.cancel()
                translationJob = scriptScope.launch {
                    try {
                        val translatedText = translator.translate(text, targetLanguage)
                        if (translatedText != null && isActive) {
                            subtitleNotifier.update(translatedText)
                        } else if (isActive) {
                            subtitleNotifier.update("[Translation Failed]")
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            subtitleNotifier.update("[Error]")
                        }
                    }
                }
            } else if (text.isBlank() && previousOcrText.isNotBlank()) {
                previousOcrText = ""
                translationJob?.cancel()
                subtitleNotifier.update("")
            }
        }
    }
}