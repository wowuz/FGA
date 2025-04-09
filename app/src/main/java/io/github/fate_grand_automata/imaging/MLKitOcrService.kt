package io.github.fate_grand_automata.imaging

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.text
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.lib_automata.OcrService
import io.github.lib_automata.Pattern
import io.github.lib_automata.dagger.ScriptScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

/**
 * OCR Service implementation using ML Kit Text Recognition.
 * More accurate for general text compared to Tesseract.
 */
@ScriptScope
class MLKitOcrService @Inject constructor(
        @ApplicationContext private val context: Context
) : OcrService {

    // Initialize the Text Recognizer
    // private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    // When using Japanese script library
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    override fun detectText(pattern: Pattern): String {
        // ML Kit works with Bitmaps. DroidCvPattern needs conversion.
        val bitmap = (pattern as? DroidCvPattern)?.asBitmap()
                ?: run {
            Timber.e("Pattern is not a DroidCvPattern or could not be converted to Bitmap")
            return "" // Return empty string if conversion fails
        }

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var recognizedText = ""
        try {
            // WARNING: This uses the BLOCKING Tasks.await().
            // Ensure this detectText method is called from a background thread
            // to avoid blocking the main UI thread.
            val resultTask = recognizer.process(inputImage)

            // Block the current thread until the task completes (or times out)
            // Adjust timeout as needed (e.g., 5 seconds)
            val visionText = Tasks.await(resultTask, 5, TimeUnit.SECONDS)

            recognizedText = visionText.text
            Timber.d("ML Kit OCR successful: ${recognizedText}")
        } catch (e: Exception) {
            Timber.e(e, "Error during ML Kit OCR processing")
        } finally {
            // Clean up the bitmap if it's not needed anymore outside this function
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }

        return recognizedText
    }

    // Consider adding a close() method if the recognizer needs cleanup,
    // though usually ML Kit manages its lifecycle.
    // fun close() { recognizer.close() }
}
