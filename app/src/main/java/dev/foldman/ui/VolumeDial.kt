// Foldy Streaman addition (not in the original Foldman): a round volume dial lying flat, like the
// knurled tuning dial on an old portable. White numbers 1 to 10 are printed round its face and
// turn with it; the one at the marker is bigger and glows.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Degrees of turn between one number and the next (1 to 10 takes 270 degrees). */
private const val DIAL_STEP = 30f

/**
 * The volume dial. [volume] is 0..1 (printed 1..10). Turn it clockwise with a finger to turn the
 * volume up. [marker] is the little pointer above it showing the current number.
 */
@Composable
fun VolumeDial(volume: Float, onVolume: (Float) -> Unit, modifier: Modifier = Modifier, marker: Color = Color.White, yellow: Boolean = false) {
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val measurer = rememberTextMeasurer()
    val latest by rememberUpdatedState(volume)
    val set by rememberUpdatedState(onVolume)
    // While turning, our own value leads so the dial never lags the finger.
    var dragValue by remember { mutableFloatStateOf(-1f) }
    val lastAngle = remember { floatArrayOf(0f) }
    val shown = if (dragValue >= 0f) dragValue else volume
    Canvas(
        modifier
            .semantics { contentDescription = "Volume ${(1 + shown * 9).roundToInt()} of 10" }
            .pointerInput(Unit) {
                fun angleOf(p: Offset) = atan2(p.y - size.height / 2f, p.x - size.width / 2f) * 180f / PI.toFloat()
                detectDragGestures(
                    onDragStart = { dragValue = latest; lastAngle[0] = angleOf(it) },
                    onDragEnd = { dragValue = -1f },
                    onDragCancel = { dragValue = -1f },
                ) { change, _ ->
                    change.consume()
                    val a = angleOf(change.position)
                    var d = a - lastAngle[0]
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    lastAngle[0] = a
                    val before = (dragValue * 9).roundToInt()
                    dragValue = (dragValue + d / (DIAL_STEP * 9f)).coerceIn(0f, 1f)
                    set(dragValue)
                    if ((dragValue * 9).roundToInt() != before) {
                        sounds?.keyUp()
                        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            },
    ) {
        val c = center
        val markerH = size.minDimension * 0.09f
        val r = size.minDimension / 2f - markerH
        val value = 1f + shown * 9f
        val turn = (value - 1f) * DIAL_STEP
        // Shadow under the dial, then the knurled rim: a ring of small grips that turn with it.
        drawCircle(Color.Black.copy(alpha = 0.45f), r * 1.02f, c + Offset(r * 0.04f, r * 0.08f))
        drawCircle(if (yellow) Color(0xFFB88E00) else Color(0xFF0A0A0B), r, c)
        val grips = 44
        for (i in 0 until grips) {
            val a = (i * 360f / grips + turn) * PI.toFloat() / 180f
            val dir = Offset(cos(a), sin(a))
            // Lit from the top left.
            val light = ((-dir.x - dir.y) * 0.5f + 0.5f).coerceIn(0f, 1f)
            drawLine(if (yellow) Color(0xFF3A2C00).copy(alpha = 0.55f - 0.3f * light) else Color.White.copy(alpha = 0.05f + 0.22f * light), c + dir * (r * 0.86f), c + dir * (r * 0.99f), r * 0.045f)
        }
        // The face.
        drawCircle(Brush.radialGradient(if (yellow) listOf(Color(0xFFFFEC7A), Color(0xFFFFD21A), Color(0xFFE0AE00)) else listOf(Color(0xFF3A3A3E), Color(0xFF1B1B1E), Color(0xFF0D0D0F)), c - Offset(r * 0.3f, r * 0.35f), r * 1.5f), r * 0.84f, c)
        drawCircle(if (yellow) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f), r * 0.84f, c, style = Stroke(1.2f))
        // The numbers, printed round the face and turning with it. Clockwise is louder, so the
        // numbers climb anticlockwise and come round to the marker at the top.
        val current = value.roundToInt()
        val textR = r * 0.6f
        for (n in 1..10) {
            val ang = (value - n) * DIAL_STEP
            val on = n == current
            // The current number swells as it reaches the marker.
            val near = (1f - abs(value - n)).coerceIn(0f, 1f)
            val style = TextStyle(
                color = (if (yellow) Color(0xFF17150C) else Color.White).copy(alpha = if (on) 1f else 0.7f),
                fontSize = (9f + 5f * near).sp,
                fontWeight = if (yellow && on) FontWeight.Black else FontWeight.Bold,
                shadow = if (on && !yellow) Shadow(Color.White.copy(alpha = 0.9f), Offset.Zero, 14f) else null,
            )
            val layout = measurer.measure(n.toString(), style)
            rotate(ang, pivot = c) {
                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - textR - layout.size.height / 2f))
            }
        }
        // The hub in the middle.
        drawCircle(Brush.radialGradient(if (yellow) listOf(Color(0xFFE0AE00), Color(0xFF9C7600)) else listOf(Color(0xFF2C2C30), Color(0xFF0A0A0B)), c, r * 0.22f), r * 0.2f, c)
        // The marker on the body, pointing down at the current number.
        val tip = Path().apply {
            moveTo(c.x, c.y - r + markerH * 0.2f)
            lineTo(c.x - markerH * 0.7f, c.y - r - markerH * 0.9f)
            lineTo(c.x + markerH * 0.7f, c.y - r - markerH * 0.9f)
            close()
        }
        drawPath(tip, marker)
    }
}
