// Foldy Streaman addition (not in the original Foldman): the battery light. It works as the running
// light it always was, and pulses green while the phone is charging.
package dev.foldman.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Screen previews only: pretend the phone is (or isn't) on charge. */
val LocalPreviewCharging = staticCompositionLocalOf<Boolean?> { null }

private val ChargeGreen = Color(0xFF35E05A)

/** True while the phone is plugged in (cable or wireless pad). Follows it live. */
@Composable
fun rememberCharging(): Boolean {
    LocalPreviewCharging.current?.let { return it }
    val context = LocalContext.current
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        fun read(i: Intent?) {
            if (i != null) charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = read(i)
        }
        // The battery report is "sticky": registering hands back the latest one straight away.
        read(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return charging
}

/**
 * A round light. Off the charger it is [on] or [off] depending on [lit]; while the phone charges
 * it breathes green instead, with a soft halo.
 */
@Composable
fun BattLed(lit: Boolean, on: Color, off: Color, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    val charging = rememberCharging()
    val pulse = if (charging) {
        val t = rememberInfiniteTransition(label = "charge")
        val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
        p
    } else 0f
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        if (charging) {
            drawCircle(Brush.radialGradient(listOf(ChargeGreen.copy(alpha = 0.75f * pulse), Color.Transparent), center, r * 2.2f), r * 2.2f)
            drawCircle(lerp(Color(0xFF123D1C), ChargeGreen, 0.15f + 0.85f * pulse), r)
            drawCircle(Color.White.copy(alpha = 0.15f + 0.5f * pulse), r * 0.3f, center - Offset(r * 0.3f, r * 0.3f))
        } else {
            if (lit) drawCircle(Brush.radialGradient(listOf(on.copy(alpha = 0.5f), Color.Transparent), center, r * 1.8f), r * 1.8f)
            drawCircle(if (lit) on else off, r)
            if (lit) drawCircle(Color.White.copy(alpha = 0.5f), r * 0.3f, center - Offset(r * 0.3f, r * 0.3f))
        }
    }
}
