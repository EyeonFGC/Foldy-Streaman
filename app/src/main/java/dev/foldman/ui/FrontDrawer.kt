// Foldy Streaman addition (not in the original Foldman): the front-screen tape drawer's sliding
// sheet. Only the handle strip along its top drags it: the tapes and the track list inside scroll on
// their own and never move the drawer. Tapping above it, or pressing back, closes it too.
package dev.foldman.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A sheet [height] tall that slides up from the bottom while [open]. [onClose] is called when it's
 * dragged down by its handle, the dimmed area above it is tapped, or back is pressed.
 */
@Composable
fun FrontDrawer(open: Boolean, onClose: () -> Unit, color: Color, height: Dp, content: @Composable () -> Unit) {
    // 0 = fully up, 1 = tucked away below the screen.
    val shut = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            shown = true
            shut.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f))
        } else if (shown) {
            shut.animateTo(1f, tween(220, easing = FastOutLinearInEasing))
            shown = false
        }
    }
    if (!shown) return
    BackHandler(enabled = open) { onClose() }
    val scope = rememberCoroutineScope()
    var sheetPx by remember { mutableFloatStateOf(1f) }
    Column(Modifier.fillMaxSize()) {
        // The dimmed player above the drawer: tap it to close.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f * (1f - shut.value)))
                .pointerInput(Unit) { detectTapGestures { onClose() } },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .height(height)
                .onSizeChanged { sheetPx = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = shut.value * size.height }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(color),
        ) {
            // The handle strip: the only place that drags the drawer.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .pointerInput(Unit) {
                        val flick = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { flick.resetTracking() },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                flick.addPosition(change.uptimeMillis, change.position)
                                scope.launch { shut.snapTo((shut.value + dy / sheetPx).coerceIn(0f, 1f)) }
                            },
                            onDragEnd = {
                                val speed = flick.calculateVelocity().y
                                scope.launch {
                                    if (shut.value > 0.3f || speed > 1200f) onClose()
                                    else shut.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f))
                                }
                            },
                            onDragCancel = { scope.launch { shut.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f)) } },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val bar = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(bar))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}
