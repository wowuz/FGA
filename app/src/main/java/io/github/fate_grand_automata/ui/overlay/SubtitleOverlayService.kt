package io.github.fate_grand_automata.ui.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.fate_grand_automata.ui.FGATheme
import io.github.fate_grand_automata.util.overlayType
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SubtitleOverlayService : Service(), SavedStateRegistryOwner {

    @Inject lateinit var windowManager: WindowManager

    private lateinit var overlayView: ComposeView

    // Manage lifecycle for Compose composition
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // State for the subtitle text
    private var subtitleText by mutableStateOf("")

    companion object {
        const val ACTION_UPDATE_SUBTITLE = "UPDATE_SUBTITLE"
        const val EXTRA_SUBTITLE_TEXT = "EXTRA_SUBTITLE_TEXT"

        fun start(context: Context) {
            val intent = Intent(context, SubtitleOverlayService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SubtitleOverlayService::class.java)
            context.stopService(intent)
        }

        fun updateSubtitle(context: Context, text: String) {
            val intent = Intent(context, SubtitleOverlayService::class.java).apply {
                action = ACTION_UPDATE_SUBTITLE
                putExtra(EXTRA_SUBTITLE_TEXT, text)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        overlayView = ComposeView(this).apply {
            // Set lifecycle owners for Compose
            setViewTreeLifecycleOwner(this@SubtitleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@SubtitleOverlayService)

            setContent {
                // Use FGATheme for consistency, or define your own
                FGATheme(darkTheme = true, background = Color.Transparent) {
                    SubtitleOverlayContent(text = subtitleText)
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType, // Ensure this is compatible with Android versions
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 150 // Adjust vertical position as needed
            // Set cutout mode for notches if needed (requires API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
            Timber.i("Subtitle overlay added")
        } catch (e: Exception) {
            Timber.e(e, "Failed to add subtitle overlay view")
            // Handle error (e.g., missing permission)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_SUBTITLE) {
            val newText = intent.getStringExtra(EXTRA_SUBTITLE_TEXT) ?: ""
            if (newText != subtitleText) {
                subtitleText = newText
                Timber.d("Subtitle updated: $subtitleText")
                // The Compose view will automatically recompose due to state change
            }
        }
        return START_STICKY // Or START_NOT_STICKY depending on desired behavior
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(overlayView)
            Timber.i("Subtitle overlay removed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove subtitle overlay view")
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        // Note: ViewModelStoreOwner is not implemented here, add if needed
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used for started services usually
        return null
    }

    // Implement SavedStateRegistryOwner
    override val lifecycle: Lifecycle get() = lifecycleRegistry
}

@Composable
fun SubtitleOverlayContent(text: String) {
    // Keep track of lifecycle for DisposableEffect
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        // Required for Compose lifecycle integration with the Service
        onDispose { }
    }

    if (text.isNotBlank()) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f), // Semi-transparent background
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}