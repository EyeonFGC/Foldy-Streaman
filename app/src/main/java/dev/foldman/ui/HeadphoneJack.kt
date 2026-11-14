// Foldy Streaman addition (not in the original Foldman): a proper 3.5 mm socket that shows a plug
// and cable when the sound goes anywhere but the phone, and opens the output menu when tapped.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The headphone socket, seen face-on. [plugged] = a plug sits in it with its cable hanging down off
 * the bottom of the screen. Tapping it calls [onClick] (the "where does the sound go" menu).
 */
@Composable
fun HeadphoneJack(plugged: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    // 0 = out, 1 = pushed home. Springs in with a little overshoot.
    val seat by animateFloatAsState(if (plugged) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "plug")
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(plugged) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (plugged) sounds?.keyDown() else sounds?.keyUp()
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    Canvas(
        modifier
            .semantics { contentDescription = if (plugged) "Headphones plugged in: change where the sound goes" else "Headphone socket: change where the sound goes" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        drawSocket(dark)
        if (seat > 0.01f) drawPlug(seat)
    }
}

private fun DrawScope.drawSocket(dark: Boolean) {
    val r = size.minDimension / 2f
    val c = center
    // Shadow, then the threaded chrome nut with a knurled edge.
    drawCircle(Color.Black.copy(alpha = 0.35f), r, c + Offset(0f, r * 0.08f))
    val metal = if (dark) listOf(Color(0xFF6A6A70), Color(0xFF2A2A2D)) else listOf(Color(0xFFF2F3F5), Color(0xFF8A8D92))
    drawCircle(Brush.linearGradient(metal, c - Offset(r, r), c + Offset(r, r)), r * 0.98f, c)
    for (i in 0 until 36) {
        val a = Math.toRadians(i * 10.0)
        val o = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
        drawLine(Color.Black.copy(alpha = 0.18f), c + o * (r * 0.86f), c + o * (r * 0.97f), r * 0.04f)
    }
    // Collar, then the hole.
    drawCircle(Brush.radialGradient(listOf(Color(0xFF9A9DA2), Color(0xFF3B3D41)), c, r * 0.7f), r * 0.66f, c)
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.66f, c, style = Stroke(r * 0.04f))
    drawCircle(Color(0xFF040404), r * 0.36f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3A3D), Color.Transparent), c - Offset(r * 0.08f, r * 0.1f), r * 0.2f), r * 0.2f, c)
}

/** The plug from the front: its rubber end, with the cable hanging down off the screen. */
private fun DrawScope.drawPlug(seat: Float) {
    val r = size.minDimension / 2f
    val c = center
    val grow = 1f + (1f - seat) * 0.25f
    val pr = r * 0.82f * grow
    // The cable: out of the bottom of the plug, curving gently down and off the screen.
    val cable = Path().apply {
        moveTo(c.x, c.y + pr * 0.6f)
        cubicTo(c.x, c.y + r * 3f, c.x + r * 1.2f, c.y + r * 5f, c.x + r * 0.6f, c.y + r * 60f)
    }
    drawPath(cable, Color.Black.copy(alpha = 0.35f * seat), style = Stroke(r * 0.42f, cap = StrokeCap.Round))
    drawPath(cable, Color(0xFF151517).copy(alpha = seat), style = Stroke(r * 0.32f, cap = StrokeCap.Round))
    drawPath(cable, Color.White.copy(alpha = 0.12f * seat), style = Stroke(r * 0.06f))
    // Strain relief and the rubber cap, lit from the top left.
    drawCircle(Color.Black.copy(alpha = 0.4f * seat), pr * 1.04f, c + Offset(pr * 0.08f, pr * 0.14f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF4A4A4F), Color(0xFF1A1A1C), Color(0xFF0A0A0B)), c - Offset(pr * 0.3f, pr * 0.35f), pr * 1.4f), pr, c)
    drawCircle(Color.White.copy(alpha = 0.16f * seat), pr * 0.72f, c, style = Stroke(pr * 0.05f))
    drawCircle(Color(0xFF0D0D0E), pr * 0.34f, c)
}
