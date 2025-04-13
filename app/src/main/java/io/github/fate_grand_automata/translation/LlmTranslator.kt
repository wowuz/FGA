package io.github.fate_grand_automata.translation

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.scopes.ServiceScoped
import io.github.fate_grand_automata.scripts.prefs.IPreferences
import io.github.lib_automata.Pattern
import io.github.lib_automata.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.tasks.Tasks
import io.github.fate_grand_automata.imaging.DroidCvPattern
import java.util.concurrent.TimeUnit


@ServiceScoped
class LlmTranslator @Inject constructor(
    private val prefs: IPreferences
) : Translator {

    // !!! IMPORTANT: Replace with your actual API Key management !!!
    // Never hardcode API keys directly in the source code.
    // Use secure storage mechanisms like Android Keystore, EncryptedSharedPreferences,
    // or fetch from a secure backend. Accessing via BuildConfig is shown as one
    // possibility but is generally NOT secure for production apps.
    private var apiKey = prefs.translation.apiKey
    // Choose a suitable Gemini model. "gemini-2.0-flash" is fast and capable.
    private val systemInstruction = content { text(prefs.translation.translateInstruction) }
    private val systemInstructionImage = content { text(prefs.translation.translateImageInstruction) }
    private val generativeModel = GenerativeModel(
        modelName = prefs.translation.translateModel,
        apiKey = apiKey,
        systemInstruction = systemInstruction,
            // Optional: Configure safety settings, temperature, etc.
        generationConfig = generationConfig {
            // Adjust temperature for creativity vs. fidelity (0.0 = deterministic)
            temperature = 0.2f
        }
        // safetySettings = listOf(...) // Add safety settings if needed
    )
    private val generativeImageModel = GenerativeModel(
        // TODO: make this configurable
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
        systemInstruction = systemInstructionImage,
        // Optional: Configure safety settings, temperature, etc.
        generationConfig = generationConfig {
            // Adjust temperature for creativity vs. fidelity (0.0 = deterministic)
            temperature = 0.2f
        }
        // safetySettings = listOf(...) // Add safety settings if needed
    )
    val chat = generativeModel.startChat()
    val chatImage = generativeImageModel.startChat()

    override suspend fun translate(text: String, targetLanguage: String): String? {
        if (text.isBlank()) {
            Timber.w("Translation requested for blank text.")
            return null
        }
        // Construct the prompt for the Gemini model
        val prompt = "Target language: $targetLanguage, text to be translated: \"$text\""

        if (apiKey == "YOUR_GEMINI_API_KEY" || apiKey.isBlank()) {
            // Timber.e("Gemini API Key not set. Please replace the placeholder.")
            Timber.w("Toggled translate: "+text)
            return text // Provide prompt message for debuging
        }

        // Perform network request in IO dispatcher
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Requesting Gemini translation for: '$text'")

                // Call the Gemini API
                val response: GenerateContentResponse = if (prefs.translation.chatMode) {
                    chat.sendMessage(prompt)
                } else {
                    generativeModel.generateContent(prompt)
                }

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


    override suspend fun translateImage(pattern: Pattern, targetLanguage: String): String? {
        // Construct the prompt for the Gemini model
        val prompt = "Target language: $targetLanguage"

        if (apiKey == "YOUR_GEMINI_API_KEY" || apiKey.isBlank()) {
            // Timber.e("Gemini API Key not set. Please replace the placeholder.")
            Timber.w("Toggled translate")
            return "" // Provide prompt message for debuging
        }

        // Perform network request in IO dispatcher
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Requesting Gemini translation for image")
                val bitmap = (pattern as? DroidCvPattern)?.asBitmap()
                    ?: run {
                        Timber.e("Pattern is not a DroidCvPattern or could not be converted to Bitmap")
                        return@withContext null // Return empty string if conversion fails
                    }

                if (bitmap == null) {
                    null
                }

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                // Call the Gemini API
                val response: GenerateContentResponse = if (prefs.translation.chatMode) {
                    chat.sendMessage(inputContent)
                } else {
                    generativeModel.generateContent(inputContent)
                }

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
