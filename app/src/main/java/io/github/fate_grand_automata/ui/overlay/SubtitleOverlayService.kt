package io.github.fate_grand_automata.ui.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Keep this import
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset // Import for offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull // Example resize icon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect // Keep this import
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity // Import for pixel conversion
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.fate_grand_automata.prefs.core.PrefsCore // Import PrefsCore
import io.github.fate_grand_automata.ui.FGATheme
import io.github.fate_grand_automata.util.overlayType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class SubtitleOverlayService : Service(), SavedStateRegistryOwner {

    @Inject lateinit var windowManager: WindowManager
    @Inject lateinit var prefsCore: PrefsCore

    private lateinit var overlayView: ComposeView
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var saveJob: Job? = null

    // --- State Variables ---
    private var subtitleText by mutableStateOf("")
    private var offsetX by mutableIntStateOf(0)
    private var offsetY by mutableIntStateOf(0)
    private var isLocked by mutableStateOf(false)
    // Size state - use pixels for WindowManager
    private var overlayWidthPx by mutableIntStateOf(500) // Default width in pixels
    private var overlayHeightPx by mutableIntStateOf(150) // Default height in pixels
    private val minWidthPx = 150 // Minimum width
    private val minHeightPx = 80 // Minimum height
    // --- End State Variables ---

    private var isOverlayVisible by mutableStateOf(true) // Track visibility state

    companion object {
        const val ACTION_UPDATE_SUBTITLE = "UPDATE_SUBTITLE"
        const val EXTRA_SUBTITLE_TEXT = "EXTRA_SUBTITLE_TEXT"
        const val ACTION_HIDE_OVERLAY = "HIDE_OVERLAY"
        const val ACTION_SHOW_OVERLAY = "SHOW_OVERLAY"

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

        // Load initial state from preferences
        offsetX = prefsCore.subtitleOverlayX.get()
        offsetY = prefsCore.subtitleOverlayY.get()
        // isLocked = prefsCore.subtitleOverlayLocked.get()
        isLocked = false // TODO: the lock button will become dead after locked, make it a decoration for now
        overlayWidthPx = prefsCore.subtitleOverlayWidth.get().coerceAtLeast(minWidthPx) // Load saved width
        overlayHeightPx = prefsCore.subtitleOverlayHeight.get().coerceAtLeast(minHeightPx) // Load saved height

        layoutParams = WindowManager.LayoutParams(
            overlayWidthPx, // Use loaded width
            overlayHeightPx, // Use loaded height
            overlayType,
            calculateFlags(isLocked),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = offsetX
            y = offsetY
            // Set cutout mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SubtitleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@SubtitleOverlayService)
            setContent {
                FGATheme(darkTheme = true, background = Color.Transparent) {
                    SubtitleOverlayContent(
                        text = subtitleText,
                        isLocked = isLocked,
                        onLockToggle = ::toggleLockState,
                        onDrag = ::handleDrag,
                        onResize = ::handleResize // Pass resize handler
                    )
                }
            }
        }
        // Set initial visibility
        var visibility = if (isOverlayVisible) View.VISIBLE else View.GONE

        try {
            windowManager.addView(overlayView, layoutParams)
            Timber.i("Subtitle overlay added at ($offsetX, $offsetY), Size=(${overlayWidthPx}x$overlayHeightPx), Locked: $isLocked")
        } catch (e: Exception) {
            Timber.e(e, "Failed to add subtitle overlay view")
        }
    }

    // --- Function to Set Visibility ---
    private fun setOverlayVisibility(visible: Boolean) {
        if (!::overlayView.isInitialized) return // Guard against early calls

        isOverlayVisible = visible
        // Post to main thread to modify View property
        overlayView.post {
            overlayView.visibility = if (visible) View.VISIBLE else View.GONE
            Timber.v("Overlay visibility set to: ${if (visible) "VISIBLE" else "GONE"}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_SUBTITLE -> {
                val newText = intent.getStringExtra(EXTRA_SUBTITLE_TEXT) ?: ""
                if (newText != subtitleText) {
                    subtitleText = newText
                    // Ensure view is visible when text updates, unless explicitly hidden
                    if (isOverlayVisible) {
                        setOverlayVisibility(true)
                    }
                    Timber.d("Subtitle updated: $subtitleText")
                }
            }
            ACTION_HIDE_OVERLAY -> {
                setOverlayVisibility(false)
            }
            ACTION_SHOW_OVERLAY -> {
                setOverlayVisibility(true)
            }
        }
        return START_STICKY
    }

    private fun calculateFlags(locked: Boolean): Int {
        return if (locked) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
    }

    private fun updateLayoutParams() {
        try {
            layoutParams.flags = calculateFlags(isLocked)
            layoutParams.x = offsetX
            layoutParams.y = offsetY
            layoutParams.width = overlayWidthPx // Update width
            layoutParams.height = overlayHeightPx // Update height
            windowManager.updateViewLayout(overlayView, layoutParams)
            Timber.v("Overlay updated: Pos=($offsetX, $offsetY), Size=(${overlayWidthPx}x$overlayHeightPx), Locked=$isLocked, Flags=${layoutParams.flags}")
            saveStateDebounced()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to update overlay layout - View not attached?")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error updating overlay layout")
        }
    }

    private fun handleDrag(dragAmountX: Float, dragAmountY: Float) {
        if (!isLocked) {
            offsetX = (offsetX + dragAmountX.roundToInt())
            offsetY = (offsetY + dragAmountY.roundToInt())
            updateLayoutParams()
        }
    }

    private fun handleResize(dragAmountX: Float, dragAmountY: Float) {
        if (!isLocked) {
            overlayWidthPx = (overlayWidthPx + dragAmountX.roundToInt()).coerceAtLeast(minWidthPx)
            overlayHeightPx = (overlayHeightPx + dragAmountY.roundToInt()).coerceAtLeast(minHeightPx)
            updateLayoutParams()
        }
    }


    private fun toggleLockState() {
        isLocked = !isLocked
        // Also ensure overlay is visible when unlocking, in case it was hidden
        if (!isLocked) {
            setOverlayVisibility(true)
        }
        updateLayoutParams()
    }

    private fun saveStateDebounced() {
        saveJob?.cancel()
        saveJob = serviceScope.launch {
            delay(500)
            saveStateToPrefs()
        }
    }

    private fun saveStateToPrefs() {
        Timber.d("Saving overlay state: Pos=($offsetX, $offsetY), Size=(${overlayWidthPx}x$overlayHeightPx), Locked=$isLocked")
        prefsCore.subtitleOverlayX.set(offsetX)
        prefsCore.subtitleOverlayY.set(offsetY)
        prefsCore.subtitleOverlayLocked.set(isLocked)
        prefsCore.subtitleOverlayWidth.set(overlayWidthPx) // Save width
        prefsCore.subtitleOverlayHeight.set(overlayHeightPx) // Save height
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            windowManager.removeView(overlayView)
            Timber.i("Subtitle overlay removed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove subtitle overlay view")
        }
        saveStateToPrefs()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override val lifecycle: Lifecycle get() = lifecycleRegistry
}