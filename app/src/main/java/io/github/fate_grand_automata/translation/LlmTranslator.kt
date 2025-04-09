package io.github.fate_grand_automata.translation

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.scopes.ServiceScoped
import io.github.lib_automata.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException // Generic IO Exception
import javax.inject.Inject


@ServiceScoped
class LlmTranslator @Inject constructor(
    // You might inject the GenerativeModel directly via Hilt if preferred
) : Translator {

    // !!! IMPORTANT: Replace with your actual API Key management !!!
    // Never hardcode API keys directly in the source code.
    // Use secure storage mechanisms like Android Keystore, EncryptedSharedPreferences,
    // or fetch from a secure backend. Accessing via BuildConfig is shown as one
    // possibility but is generally NOT secure for production apps.
    private val apiKey = "YOUR_GEMINI_API_KEY" // Placeholder - Replace securely!

    // Choose a suitable Gemini model. "gemini-2.0-flash" is fast and capable.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
        // Optional: Configure safety settings, temperature, etc.
        generationConfig = generationConfig {
            // Adjust temperature for creativity vs. fidelity (0.0 = deterministic)
            temperature = 0.2f
        }
        // safetySettings = listOf(...) // Add safety settings if needed
    )

    override suspend fun translate(text: String, targetLanguage: String): String? {
        if (text.isBlank()) {
            Timber.w("Translation requested for blank text.")
            return null
        }
        // Construct the prompt for the Gemini model
        val prompt = "Translate the following text to $targetLanguage: \"$text\""

        if (apiKey == "YOUR_GEMINI_API_KEY") {
            // Timber.e("Gemini API Key not set. Please replace the placeholder.")
            return prompt // Provide prompt message for debuging
        }

        // Perform network request in IO dispatcher
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Requesting Gemini translation for: '$text' to '$targetLanguage'")

                // Call the Gemini API
                val response: GenerateContentResponse = generativeModel.generateContent(prompt)

                // Extract the translated text from the response
                val translatedText = response.text

                if (translatedText != null) {
                    Timber.d("Gemini translation successful: '$translatedText'")
                    translatedText.trim() // Trim potential leading/trailing whitespace
                } else {
                    // Log potential safety blocks or other reasons for null text
                    Timber.w("Gemini response text was null. Response: $response")
                    null
                }

            } catch (e: IOException) {
                Timber.e(e, "Network error during Gemini translation request.")
                null // Handle network errors
            } catch (e: Exception) {
                // Catch more specific exceptions from the Gemini SDK if possible
                Timber.e(e, "Error during Gemini translation.")
                null // Handle other potential errors (API errors, etc.)
            }
        }
    }
}
