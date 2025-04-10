package io.github.fate_grand_automata.scripts // Or a more suitable shared package if needed

/**
 * Interface for notifying the UI layer to display subtitles.
 * Used to decouple script logic from the Android Overlay Service implementation.
 */
interface ISubtitleNotifier {
    /**
     * Signals that the subtitle overlay should be started/shown.
     */
    fun start()

    /**
     * Signals that the subtitle overlay should be stopped/hidden.
     */
    fun stop()

    /**
     * Sends new text to be displayed on the subtitle overlay.
     * An empty string can be used to clear the overlay.
     *
     * @param text The text to display.
     */
    fun update(text: String)

    /**
     * Temporarily hides the overlay view (e.g., for taking a screenshot).
     * The overlay service itself remains running.
     */
    fun hide()

    /**
     * Makes the overlay view visible again after being hidden temporarily.
     */
    fun show()
}