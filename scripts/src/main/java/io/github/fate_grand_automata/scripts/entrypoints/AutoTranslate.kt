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
import kotlinx.coroutines.delay
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
        subtitleNotifier.hide()
        kotlinx.coroutines.runBlocking { delay(3) }
        val fullScreenshot = try {
            screenshotService.takeScreenshot()
        } finally {
            kotlinx.coroutines.runBlocking { delay(3) }
            subtitleNotifier.show()
        }
        val dialogBox = fullScreenshot.crop(
            Region((0.12 * fullScreenshot.width).toInt(),  // x
                   (0.76 * fullScreenshot.height).toInt(), // y
                   (0.6 * fullScreenshot.width).toInt(),  // width
                   (0.24 * fullScreenshot.height).toInt()) // height
        )// TODO: move these to Image.kt
        dialogBox.tag = "dialogBox"
        val dialogCharacterName = fullScreenshot.crop(
            Region((0.10 * fullScreenshot.width).toInt(),  // x
                   (0.68 * fullScreenshot.height).toInt(), // y
                   (0.60 * fullScreenshot.width).toInt(),  // width
                   (0.09 * fullScreenshot.height).toInt()) // height
        )// TODO: move these to Image.kt
        dialogCharacterName.tag = "dialogCharacterName"

        // Could there be no dialog, only options?
        // If so, might need to adjust the option ocr region
        // for now just leave it like this
        val dialogOptions = fullScreenshot.crop(
            Region((0.1 * fullScreenshot.width).toInt(),  // x
                0, // y
                (0.7 * fullScreenshot.width).toInt(),  // width
                (0.6 * fullScreenshot.height).toInt()) // height
        )// TODO: move these to Image.kt
        dialogOptions.tag = "dialogOptions"

        var dialogBoxStr = ""
        var dialogCharacterNameStr = ""
        var dialogOptionsStr = ""

        dialogBox.use { pattern ->
            val ocrResult = ocrService.detectText(pattern).trim()
            if (ocrResult.isNotBlank()) {
                dialogBoxStr = ocrResult
            }
        }

        dialogCharacterName.use { pattern ->
            val ocrResult = ocrService.detectText(pattern).trim()
            if (ocrResult.isNotBlank()) {
                dialogCharacterNameStr = ocrResult
            }
        }

        dialogOptions.use { pattern ->
            val ocrResult = ocrService.detectText(pattern).trim()
            if (ocrResult.isNotBlank()) {
                dialogOptionsStr = ocrResult
            }
        }

        val text = buildString {
            if (dialogOptionsStr.isNotBlank()) {
                appendLine("選択肢：")
                appendLine(dialogOptionsStr)
            }
            if (dialogCharacterNameStr.isNotBlank()) {
                appendLine(dialogCharacterNameStr + "：")
            }
            if (dialogBoxStr.isNotBlank()) {
                appendLine(dialogBoxStr)
            }
        }.trim()

        // Only check dialogBoxStr for now, since it should be the most stable ocr result
        // because of the dialog box background
        if (dialogBoxStr.isNotBlank() && dialogBoxStr != previousOcrText) {
            previousOcrText = dialogBoxStr
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
        } else if (dialogBoxStr.isBlank() && dialogOptionsStr.isBlank() && previousOcrText.isNotBlank()) {
            previousOcrText = ""
            translationJob?.cancel()
            subtitleNotifier.update("")
        }
    }
}
