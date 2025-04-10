package io.github.fate_grand_automata.ui.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.OpenInFull // Example resize icon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SubtitleOverlayContent(
    text: String,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit // Callback for resize
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) { onDispose { } }

    val dragModifier = if (!isLocked) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x, dragAmount.y)
            }
        }
    } else Modifier

    // Main Box for positioning elements including resize handle
    Box(modifier = dragModifier) {
        Surface(
            modifier = Modifier.padding(10.dp), // Padding for the main surface
            color = Color.Black.copy(alpha = if (text.isBlank() && isLocked) 0f else 0.7f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                } else {
                    if (!isLocked) Spacer(modifier = Modifier.weight(1f))
                }

                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    IconButton(
                        onClick = onLockToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Unlock Overlay" else "Lock Overlay",
                        )
                    }
                }
            }
        }

        // Resize Handle (Bottom Right Corner) - Visible only when unlocked
        if (!isLocked) {
            val handleSize = 24.dp // Size of the touch target for resize handle
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // Offset slightly inward to be visually distinct
                    .offset { IntOffset(-(handleSize / 3).roundToPx(), -(handleSize / 3).roundToPx()) }
                    .size(handleSize)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape) // Visual indicator
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y) // Call resize handler
                        }
                    }
            ) {
                Icon(
                    Icons.Filled.OpenInFull, // Example resize icon
                    contentDescription = "Resize",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center).size(16.dp)
                )
            }
        }
    }
}
