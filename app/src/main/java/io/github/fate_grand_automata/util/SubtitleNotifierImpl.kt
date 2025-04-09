package io.github.fate_grand_automata.util // Or another appropriate package in the 'app' module

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import io.github.fate_grand_automata.scripts.ISubtitleNotifier
import io.github.fate_grand_automata.ui.overlay.SubtitleOverlayService
import javax.inject.Inject

/**
 * Implementation of ISubtitleNotifier within the 'app' module.
 * This class acts as a bridge between the script logic and the UI overlay service.
 */
@ServiceScoped // Scope this to the service running the script
class SubtitleNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ISubtitleNotifier {

    override fun start() {
        // Use the static method to start the overlay service
        SubtitleOverlayService.start(context)
    }

    override fun stop() {
        // Use the static method to stop the overlay service
        SubtitleOverlayService.stop(context)
    }

    override fun update(text: String) {
        // Use the static method to update the subtitle text
        SubtitleOverlayService.updateSubtitle(context, text)
    }
}