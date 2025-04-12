package io.github.fate_grand_automata.scripts.prefs

import io.github.lib_automata.Region

/**
 * Interface defining preferences specific to the AutoTranslate feature.
 */
interface ITranslationPreferences {
    val apiKey: String
    val targetLanguage: String
    val targetImageLanguage: String
    val ocrRegionX: Int
    val ocrRegionY: Int
    val ocrRegionWidth: Int
    val ocrRegionHeight: Int

    val translateModel: String
    val chatMode: Boolean

    /**
     * Convenience function to get the OCR region as a Region object.
     */
    fun getOcrRegion(): Region = Region(
        x = ocrRegionX,
        y = ocrRegionY,
        width = ocrRegionWidth,
        height = ocrRegionHeight
    )
}