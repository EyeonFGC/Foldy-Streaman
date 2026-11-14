// Foldy Streaman addition (not in the original Foldman): a thumbwheel for the volume, like the
// ribbed wheel on the side of a real portable player. Roll it up or down; the numbers 1 to 10
// printed round it turn past a little marker.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** How far round the wheel one number is. */
private const val STEP_DEG = 26f

/**
 * The volume thumbwheel. [volume] is 0..1 (shown as 1..10); dragging up turns it up. [marker] is
 * the colour of the little pointer beside the current number.
 */
@Composable
fun VolumeWheel(volume: Float, onVolume: (Float) -> Unit, modifier: Modifier = Modifier, marker: Color = Color(0xFFE5303F), dark: Boolean = false) {
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val measurer = rememberTextMeasurer()
    val latest by rememberUpdatedState(volume)
    val set by rememberUpdatedState(onVolume)
    // While dragging, our own value leads so the wheel never lags the finger.
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
                    // About one number per 30 px of roll.
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
        val r = h / 2f
        val cy = h / 2f
        val bodyW = w * 0.72f
        val left = (w - bodyW) / 2f
        val corner = CornerRadius(bodyW * 0.18f)
        // Slot in the case.
        drawRoundRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(left - 3f, -3f), size = Size(bodyW + 6f, h + 6f), cornerRadius = corner)
        // The wheel's rim: grey plastic, lit in the middle, falling away top and bottom.
        val base = if (dark) listOf(Color(0xFF1A1A1C), Color(0xFF55565B), Color(0xFF6E6F75), Color(0xFF55565B), Color(0xFF1A1A1C))
        else listOf(Color(0xFF4A4C51), Color(0xFFA9ACB2), Color(0xFFD4D6DA), Color(0xFFA9ACB2), Color(0xFF4A4C51))
        drawRoundRect(Brush.verticalGradient(base), topLeft = Offset(left, 0f), size = Size(bodyW, h), cornerRadius = corner)
        // Ribs round the wheel, crowding together towards the edges as it curves away.
        val turn = (1f + shown * 9f) * STEP_DEG
        var a = -90f
        while (a <= 90f) {
            val ang = a + (turn % 6f)
            if (abs(ang) < 88f) {
                val y = cy - sin(ang * PI / 180f).toFloat() * r
                val fade = cos(ang * PI / 180f).toFloat()
                drawLine(Color.Black.copy(alpha = 0.22f * fade), Offset(left + bodyW * 0.08f, y), Offset(left + bodyW * 0.22f, y), 1.6f)
                drawLine(Color.Black.copy(alpha = 0.22f * fade), Offset(left + bodyW * 0.78f, y), Offset(left + bodyW * 0.92f, y), 1.6f)
            }
            a += 6f
        }
        // The numbers 1..10 printed round the middle of the wheel.
        val ink = if (dark) Color(0xFFE8E8EA) else Color(0xFF1F2023)
        for (n in 1..10) {
            val ang = (n - (1f + shown * 9f)) * STEP_DEG
            if (abs(ang) >= 80f) continue
            val rad = ang * PI / 180f
            val y = cy - sin(rad).toFloat() * r
            val squash = cos(rad).toFloat()
            val layout = measurer.measure(n.toString(), TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.35f + 0.65f * squash)))
            withTransform({
                translate(w / 2f - layout.size.width / 2f, y - layout.size.height / 2f)
                scale(1f, squash, Offset(layout.size.width / 2f, layout.size.height / 2f))
            }) { drawText(layout) }
        }
        // Shine along the middle, and the pointer at the current number.
        drawRoundRect(Color.White.copy(alpha = if (dark) 0.08f else 0.18f), topLeft = Offset(left + bodyW * 0.05f, cy - h * 0.06f), size = Size(bodyW * 0.9f, h * 0.035f), cornerRadius = CornerRadius(4f))
        drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(left, 0f), size = Size(bodyW, h), cornerRadius = corner, style = Stroke(1.2f))
        val tip = Path().apply {
            moveTo(left - 2f, cy); lineTo(left - 2f - w * 0.1f, cy - w * 0.07f); lineTo(left - 2f - w * 0.1f, cy + w * 0.07f); close()
        }
        drawPath(tip, marker)
    }
}

/** Centres the wheel in the space it's given, at a sensible size. */
@Composable
fun VolumeWheelSlot(volume: Float, onVolume: (Float) -> Unit, modifier: Modifier, marker: Color = Color(0xFFE5303F), dark: Boolean = false) {
    androidx.compose.foundation.layout.Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        VolumeWheel(
            volume, onVolume,
            androidx.compose.ui.Modifier
                .fillMaxWidth(0.62f)
                .heightIn(max = 190.dp)
                .fillMaxHeight(),
            marker = marker, dark = dark,
        )
    }
}
