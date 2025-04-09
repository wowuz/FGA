package io.github.fate_grand_automata.imaging

import android.content.Context
import android.graphics.Bitmap
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

        // Use runBlocking or appropriate coroutine scope if needed,
        // but OCR might be called from background threads already.
        // Here, we'll use a simple blocking call for demonstration.
        // In a real app, handle the async nature properly.
        try {
            // ML Kit's process method returns a Task, await() makes it suspend until result.
            // Needs kotlinx-coroutines-play-services dependency.
            // For simplicity, showing a blocking approach (not ideal for UI thread).
            val result = recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                    recognizedText = visionText.text
                Timber.d("ML Kit OCR successful: ${recognizedText.lines().size} lines")
            }
                .addOnFailureListener { e ->
                    Timber.e(e, "ML Kit OCR failed")
                // Handle the error appropriately
            }

            // A crude way to block until the task finishes for this example.
            // Replace with proper coroutine handling (e.g., viewModelScope.launch).
            // @Suppress("BlockingMethodInNonBlockingContext") // Example only
            //         kotlinx.coroutines.runBlocking { kotlinx.coroutines.tasks.await(result) }
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
