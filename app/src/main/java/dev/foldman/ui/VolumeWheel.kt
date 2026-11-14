// Foldy Streaman addition (not in the original Foldman): a thumbwheel for the volume, like the
// one on the side of a real portable player: a dark, finely knurled wheel sitting in a slot, the
// word VOLUME printed up the case beside it, and the numbers 1 to 10 printed next to the wheel
// that roll with it as it turns.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** How far round the wheel one number is. */
private const val STEP_DEG = 24f
/** The knurling: one rib every this many degrees. */
private const val RIB_DEG = 5f

/**
 * The volume thumbwheel. [volume] is 0..1 (printed 1..10); rolling it up turns it up. [ink] is the
 * printing (VOLUME and the numbers), [marker] the little pointer at the current number.
 */
@Composable
fun VolumeWheel(volume: Float, onVolume: (Float) -> Unit, modifier: Modifier = Modifier, ink: Color = Color(0xFF1F2023), marker: Color = Color(0xFFE5303F)) {
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val measurer = rememberTextMeasurer()
    val latest by rememberUpdatedState(volume)
    val set by rememberUpdatedState(onVolume)
    // While rolling, our own value leads so the wheel never lags the finger.
    var dragValue by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragValue >= 0f) dragValue else volume
    Canvas(
        modifier
            .semantics { contentDescription = "Volume ${(1 + shown * 9).roundToInt()} of 10" }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragValue = latest },
                    onDragEnd = { dragValue = -1f },
                    onDragCancel = { dragValue = -1f },
                ) { change, dy ->
                    change.consume()
                    val before = (dragValue * 9).roundToInt()
                    dragValue = (dragValue - dy / (size.height * 1.4f)).coerceIn(0f, 1f)
                    set(dragValue)
                    if ((dragValue * 9).roundToInt() != before) {
                        sounds?.keyUp()
                        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        // Three columns: VOLUME printed upright | the wheel in its slot | the numbers.
        val labelW = w * 0.22f
        val slotL = labelW
        val slotW = w * 0.4f
        val numL = slotL + slotW + w * 0.04f
        val slotRect = Size(slotW, h)
        val corner = CornerRadius(slotW * 0.22f)

        // "VOLUME", reading bottom to top up the case.
        val label = measurer.measure("VOLUME", TextStyle(color = ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
        rotate(-90f, pivot = Offset(labelW / 2f, cy)) {
            drawText(label, topLeft = Offset(labelW / 2f - label.size.width / 2f, cy - label.size.height / 2f))
        }

        // The slot in the case: a dark recess with a bevelled lip.
        drawRoundRect(Color(0xFF060607), topLeft = Offset(slotL, 0f), size = slotRect, cornerRadius = corner)
        drawRoundRect(Color.White.copy(alpha = 0.12f), topLeft = Offset(slotL, 0f), size = slotRect, cornerRadius = corner, style = Stroke(1.5f))

        // The wheel: a dark cylinder, lit along the middle, sitting in the slot.
        val inset = slotW * 0.12f
        val wl = slotL + inset
        val ww = slotW - inset * 2
        val wt = h * 0.03f
        val wh = h - wt * 2
        val r = wh / 2f
        clipRect(wl, wt, wl + ww, wt + wh) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF050506), Color(0xFF2C2C2F), Color(0xFF4A4A4F), Color(0xFF2C2C2F), Color(0xFF050506)), wt, wt + wh),
                topLeft = Offset(wl, wt), size = Size(ww, wh),
            )
            // Fine knurled ribs across the wheel, packed together towards the top and bottom.
            val turn = (1f + shown * 9f) * STEP_DEG
            var a = -90f
            while (a <= 90f) {
                val ang = a + (turn % RIB_DEG)
                if (abs(ang) < 89f) {
                    val rad = ang * PI / 180f
                    val y = cy - sin(rad).toFloat() * r
                    val face = cos(rad).toFloat()
                    drawLine(Color.Black.copy(alpha = 0.55f * face + 0.2f), Offset(wl, y), Offset(wl + ww, y), (1.8f * face).coerceAtLeast(0.6f))
                    drawLine(Color.White.copy(alpha = 0.12f * face), Offset(wl, y + 1.6f * face), Offset(wl + ww, y + 1.6f * face), 0.8f)
                }
                a += RIB_DEG
            }
            // Its edges curve away into shadow.
            drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.45f)), wl, wl + ww), topLeft = Offset(wl, wt), size = Size(ww, wh))
        }

        // The numbers beside the wheel, rolling with it: squashed and faded as they turn away.
        val value = 1f + shown * 9f
        for (n in 1..10) {
            val ang = (n - value) * STEP_DEG
            if (abs(ang) >= 82f) continue
            val rad = ang * PI / 180f
            val y = cy - sin(rad).toFloat() * r
            val squash = cos(rad).toFloat()
            val layout = measurer.measure(n.toString(), TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.25f + 0.75f * squash)))
            withTransform({
                translate(numL + w * 0.1f, y - layout.size.height / 2f)
                scale(1f, squash, Offset(0f, layout.size.height / 2f))
            }) { drawText(layout) }
            // A small tick from the wheel to each number.
            drawLine(ink.copy(alpha = 0.5f * squash), Offset(numL, y), Offset(numL + w * 0.07f, y), 1.2f)
        }
        // The pointer at the current number, on the edge of the slot.
        val tip = Path().apply {
            moveTo(slotL + slotW + 1f, cy); lineTo(slotL + slotW + w * 0.07f, cy - w * 0.05f); lineTo(slotL + slotW + w * 0.07f, cy + w * 0.05f); close()
        }
        drawPath(tip, marker)
    }
}

/** Places the wheel in the space it's given, at a sensible size. */
@Composable
fun VolumeWheelSlot(volume: Float, onVolume: (Float) -> Unit, modifier: Modifier, ink: Color = Color(0xFF1F2023), marker: Color = Color(0xFFE5303F)) {
    androidx.compose.foundation.layout.Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        VolumeWheel(
            volume, onVolume,
            Modifier
                .fillMaxWidth()
                .heightIn(max = 190.dp)
                .fillMaxHeight(),
            ink = ink, marker = marker,
        )
    }
}
