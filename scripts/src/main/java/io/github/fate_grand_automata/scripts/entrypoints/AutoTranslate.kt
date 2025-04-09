package io.github.fate_grand_automata.scripts.entrypoints

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.ISubtitleNotifier
import io.github.fate_grand_automata.scripts.prefs.IPreferences
import io.github.lib_automata.EntryPoint
import io.github.lib_automata.ExitManager
import io.github.lib_automata.OcrService
import io.github.lib_automata.Translator
import io.github.lib_automata.Region
// Removed: import io.github.lib_automata.ScreenshotManager
import io.github.lib_automata.ScreenshotService // Import ScreenshotService
import io.github.lib_automata.ScriptAbortException
import io.github.lib_automata.Transformer
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
    // Dependencies required by EntryPoint and IFgoAutomataApi
    exitManager: ExitManager,
    api: IFgoAutomataApi,
    // Specific dependencies for this script
    private val ocrService: OcrService,
    private val translator: Translator,
    private val subtitleNotifier: ISubtitleNotifier,
    // Explicitly inject ScreenshotService and Transformer
    private val screenshotService: ScreenshotService, // Inject ScreenshotService directly
    private val transformer: Transformer
) : EntryPoint(exitManager), IFgoAutomataApi by api { // Implement IFgoAutomataApi

    // --- Read Configuration from IPreferences ---
    private val ocrRegion = prefs.translation.getOcrRegion()
    private val targetLanguage = prefs.translation.targetLanguage
    private val checkInterval = 500.milliseconds
    // --- End Configuration ---

    private var translationJob: Job? = null
    private var previousOcrText: String = ""
    private val scriptScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    class ExitException(override val message: String) : Exception(message)

    override fun script(): Nothing {
        subtitleNotifier.start()
        try {
            if (prefs.translation.apiKey.isBlank()) {
                throw ExitException("Gemini API Key is not set in More Options -> Advanced.")
            }

            while (scriptScope.isActive) {
                exitManager.checkExitRequested()
                checkAndTranslateOcrRegion()
                checkInterval.wait(applyMultiplier = false)
            }
        } catch (e: ScriptAbortException) {
            throw ExitException("Script aborted")
        } catch (e: ExitException) {
            throw e
        } catch (e: Exception) {
            throw ExitException("Unexpected error: ${e.message}")
        } finally {
            translationJob?.cancel()
            subtitleNotifier.stop()
            scriptScope.cancel()
        }
        throw ScriptAbortException()
    }

    private fun checkAndTranslateOcrRegion() {
        // --- Using screenshotService directly ---
        // WARNING: This gets the RAW screenshot. The transformer might expect
        // coordinates relative to the scaled/cropped image usually provided
        // by ScreenshotManager. This might lead to incorrect cropping.
        // val currentRawScreenshot = screenshotService.takeScreenshot()

        // Transform the script region to image coordinates (intended for the scaled image)
        // val imageRegion = transformer.toImage(ocrRegion)

        // Crop the RAW screenshot using coordinates meant for the scaled image
        // val regionOfInterestPattern = currentRawScreenshot.crop(imageRegion)

        val regionOfInterestPattern = screenshotService.takeScreenshot()
        regionOfInterestPattern.tag = "OCR_Translate_Raw_Service"
        // --- End direct screenshotService usage ---

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
