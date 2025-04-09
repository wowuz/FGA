package io.github.lib_automata

// Define an interface for easier testing and potential swapping of LLM providers
interface Translator {
    /**
     * Translates the given text to the target language.
     * @param text The text to translate.
     * @param targetLanguage The ISO 639-1 code for the target language (e.g., "en", "ja", "es").
     * @return The translated text, or null if translation failed.
     */
    suspend fun translate(text: String, targetLanguage: String): String?
}