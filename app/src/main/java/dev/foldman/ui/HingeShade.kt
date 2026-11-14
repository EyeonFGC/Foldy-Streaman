// Foldy Streaman addition (not in the original Foldman): the inside screen darkens from the fold
// outwards as the phone closes, and lightens again as it opens, following the hinge.
package dev.foldman.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Fully open from here up: no shade at all. */
private const val CLEAR_FROM = 165f
/** Almost black from here down (the screen switches to the front soon after). */
private const val DARK_AT = 25f

/**
 * Darkness from the hinge angle (180 = flat, 0 = shut): none when open, rising as it closes, so
 * opening the phone plays the same thing in reverse. [verticalFold] = the fold runs top to bottom
 * (the Fold held as a book); otherwise it runs across.
 */
@Composable
fun HingeShade(verticalFold: Boolean, modifier: Modifier = Modifier, previewAngle: Float? = null) {
    val context = LocalContext.current
    var angle by remember { mutableFloatStateOf(previewAngle ?: 180f) }
    DisposableEffect(previewAngle) {
        if (previewAngle != null) return@DisposableEffect onDispose {}
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val hinge = sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { angle = event.values[0] }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (hinge != null) sensors.registerListener(listener, hinge, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
    val target = ((CLEAR_FROM - angle) / (CLEAR_FROM - DARK_AT)).coerceIn(0f, 1f)
    // A touch of smoothing so it glides with the hinge rather than stepping.
    val dark by animateFloatAsState(target, tween(if (previewAngle != null) 0 else 90), label = "hingeShade")
    if (dark <= 0.001f) return
    Canvas(modifier) {
        // Darkest at the fold, lighter towards the edges, all of it deepening as the phone closes.
        val centre = (dark * 1.45f).coerceAtMost(1f)
        val mid = (dark * 0.9f).coerceAtMost(1f)
        val edge = (dark * dark * 0.85f).coerceAtMost(1f)
        val shade = Color(0xFF07060A)
        val stops = arrayOf(
            0f to shade.copy(alpha = edge),
            0.3f to shade.copy(alpha = mid),
            0.5f to shade.copy(alpha = centre),
            0.7f to shade.copy(alpha = mid),
            1f to shade.copy(alpha = edge),
        )
        drawRect(if (verticalFold) Brush.horizontalGradient(*stops) else Brush.verticalGradient(*stops))
    }
}
