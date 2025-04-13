package io.github.fate_grand_automata.scripts.entrypoints

// Removed: import io.github.lib_automata.ScreenshotManager
import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.ISubtitleNotifier
import io.github.lib_automata.EntryPoint
import io.github.lib_automata.ExitManager
import io.github.lib_automata.OcrService
import io.github.lib_automata.Pattern
import io.github.lib_automata.Region
import io.github.lib_automata.ScreenshotService
import io.github.lib_automata.ScriptAbortException
import io.github.lib_automata.Transformer
import io.github.lib_automata.Translator
import io.github.lib_automata.dagger.ScriptScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds


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
    // private val ocrRegion = prefs.translation.getOcrRegion()
    private val checkInterval = 500.milliseconds
    // --- End Configuration ---

    private var translationJob: Job? = null
    private var previousOcrText: String = ""
    private var previousTranslatedText: String = ""
    private var previousPattern: Pattern = screenshotService.takeScreenshot()
    // TODO: move these to Image.kt
    private val findRegion = Region(
        (0.13 * previousPattern.width).toInt(),  // x
        (0.60 * previousPattern.height).toInt(), // y
        (0.30 * previousPattern.width).toInt(),  // width
        (0.30 * previousPattern.height).toInt()) // height
    private val compareRegion = Region(
        (0.14 * previousPattern.width).toInt(),  // x
        (0.69 * previousPattern.height).toInt(), // y
        (0.25 * previousPattern.width).toInt(),  // width
        (0.25 * previousPattern.height).toInt()) // height

    private val dialogBoxRegion = Region(
        (0.12 * previousPattern.width).toInt(),  // x
        (0.76 * previousPattern.height).toInt(), // y
        (0.58 * previousPattern.width).toInt(),  // width
        (0.23 * previousPattern.height).toInt()) // height
    private val dialogCharacterNameRegion = Region(
        (0.09 * previousPattern.width).toInt(),  // x
        (0.67 * previousPattern.height).toInt(), // y
        (0.60 * previousPattern.width).toInt(),  // width
        (0.10 * previousPattern.height).toInt()) // height
    private val dialogOptionsRegion = Region(
        (0.12 * previousPattern.width).toInt(),  // x
        0, // y
        (0.58 * previousPattern.width).toInt(),  // width
        (0.66 * previousPattern.height).toInt()) // height

    private val noSubtitleRegion = Region(
        (0.10 * previousPattern.width).toInt(),  // x
        (0.10 * previousPattern.height).toInt(), // y
        (0.60 * previousPattern.width).toInt(),  // width
        (0.90 * previousPattern.height).toInt()) // height

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
                // TODO: If no touch, don't do the ocr
                checkAndTranslateOcrRegion()
                // checkInterval.wait(applyMultiplier = false)
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

    private fun editDistance(s1: String, s2: String): Int {
        var s1 = s1
        var s2 = s2
        s1 = s1.lowercase(Locale.getDefault())
        s2 = s2.lowercase(Locale.getDefault())

        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) costs[j] = j
                else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1]) newValue = (min(
                            min(newValue.toDouble(), lastValue.toDouble()),
                            costs[j].toDouble()
                        ) + 1).toInt()
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

    private fun similarity(s1: String, s2: String): Double {
        var longer = s1
        var shorter = s2
        if (s1.length < s2.length) { // longer should always have greater length
            longer = s2
            shorter = s1
        }
        val longerLength = longer.length
        if (longerLength == 0) {
            return 1.0 /* both strings are zero length */
        }
        /* // If you have StringUtils, you can use it to calculate the edit distance:
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                                                             (double) longerLength; */
        return (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
    }

    private fun checkAndTranslateOcrRegion() {
        var fullScreenshot = screenshotService.takeScreenshot()
        var patternToBeCompared = fullScreenshot.crop(
            compareRegion
        )
        if (previousPattern in findRegion) {
            runBlocking { delay(100) }
        } else {
            // Causes flicker, any better approch?
            // subtitleNotifier.hide()
            // runBlocking { delay(60) }
            previousPattern = patternToBeCompared
            // fullScreenshot = try {
            //     screenshotService.takeScreenshot()
            // } finally {
            //     runBlocking { delay(150) }
            //     subtitleNotifier.show()
            // }
            val dialogBox = fullScreenshot.crop(
                dialogBoxRegion
            )
            // dialogBox.tag = "dialogBox"

            val dialogCharacterName = fullScreenshot.crop(
                dialogCharacterNameRegion
            )
            // dialogCharacterName.tag = "dialogCharacterName"

            // Could there be no dialog, only options?
            // If so, might need to adjust the option ocr region
            // for now just leave it like this
            val dialogOptions = fullScreenshot.crop(
                dialogOptionsRegion
            )
            // dialogOptions.tag = "dialogOptions"

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
                if (dialogOptionsStr.length > 4) {
                    appendLine("選択肢：{")
                    appendLine(dialogOptionsStr+"}")
                }
                if (dialogCharacterNameStr.isNotBlank()) {
                    appendLine(dialogCharacterNameStr + "：")
                }
                if (dialogBoxStr.isNotBlank()) {
                    appendLine(dialogBoxStr)
                }
            }.trim()

            val similarity = similarity(text, previousOcrText)
            if (text.isNotBlank() && similarity < 0.85) { // 0.85 is not tested
                if (text.isNotBlank()) {
                    previousOcrText = text
                }
                if (translationJob != null && !translationJob!!.isCompleted) {
                    subtitleNotifier.update(previousTranslatedText)
                    runBlocking { delay(300) } // prevent asking for translate too fast
                    translationJob?.cancel()
                }
                translationJob = scriptScope.launch {
                    try {
                        var translatedText = ""
                        if (prefs.translation.imageInputSwitch) {
                            translatedText = translator.translateImage(fullScreenshot.crop(noSubtitleRegion), prefs.translation.targetLanguage)?: ""
                        } else {
                            translatedText = translator.translate(text, prefs.translation.targetLanguage)?: ""
                        }

                        if (translatedText == "Null") {
                            subtitleNotifier.update(previousTranslatedText)
                        } else if (translatedText == "Null Quota") {
                            subtitleNotifier.update(previousTranslatedText+
                                    System.lineSeparator()+"Gemini Quota Exceeded, retrying in 3 seconds...")
                            runBlocking { delay(3000) } // prevent asking for translate too fast
                        } else if (isActive) {
                            subtitleNotifier.update(translatedText)
                            previousTranslatedText = translatedText
                        } else {
                            subtitleNotifier.update("[Translation Failed]")
                        }
                    } catch (e: CancellationException) {
                        subtitleNotifier.update(previousTranslatedText)
                    } catch (e: Exception) {
                        if (isActive) {
                            subtitleNotifier.update("[Error]")
                        }
                    }
                }
            } else if (text.isBlank() && previousOcrText != "Null") { // Uses two cycle to stop subtitle
                previousOcrText = "Null"
                translationJob?.cancel()
                // Uses two cycle to stop subtitle
                // subtitleNotifier.update("")
            } else if (text.isBlank() && previousOcrText == "Null") {
                previousOcrText = ""
                translationJob?.cancel()
                subtitleNotifier.update("")
            }
            // TODO: make this configurable
            runBlocking { delay(100) }
        }
    }
}
