// Foldy Streaman addition (not in the original Foldman): a small glowing padlock on the front
// player while it is showing over the lock screen, so you can tell the phone is still locked. Each
// time the phone wakes, the padlock's shackle swings down and clicks shut.
package dev.foldman.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.foldman.deck.DeckTheme
import dev.foldman.deck.DeckUi
import kotlinx.coroutines.delay

/** Screen previews only: pretend the phone is locked. */
val LocalPreviewLocked = staticCompositionLocalOf { false }

/**
 * 0 while the phone is unlocked. While it is locked: a number that goes up each time the screen
 * wakes, so the padlock can click shut afresh every time.
 */
@Composable
private fun rememberLockedWake(): Int {
    if (LocalPreviewLocked.current) return -1
    val context = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var wake by remember { mutableIntStateOf(0) }
    DisposableEffect(context, owner) {
        val guard = context.getSystemService(KeyguardManager::class.java)
        var count = 0
        fun check(newWake: Boolean) {
            val locked = guard?.isKeyguardLocked == true
            wake = if (!locked) 0 else {
                if (newWake || wake == 0) count++
                count
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = check(newWake = i?.action == Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
        val watch = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) check(newWake = false)
        }
        owner.lifecycle.addObserver(watch)
        check(newWake = false)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            owner.lifecycle.removeObserver(watch)
        }
    }
    return wake
}

/** Where each theme wears its padlock (as a fraction of the front screen) and in what colour. */
private class LockSpot(val x: Float, val y: Float, val color: Color)

private fun lockSpot(theme: DeckTheme): LockSpot = when (theme) {
    // On the grey rubber surround, under the window.
    DeckTheme.SPORT_YELLOW -> LockSpot(0.045f, 0.715f, Color.White)
    // On the black glass, in its clear top-right corner.
    DeckTheme.RED_CHROME -> LockSpot(0.60f, 0.165f, Color.White)
    DeckTheme.BLACK_GOLD -> LockSpot(0.05f, 0.035f, BlackGold.Gold)
    DeckTheme.CHERRY_RED -> LockSpot(0.045f, 0.105f, CherryRed.Yellow)
    DeckTheme.SILVER_BLUE -> LockSpot(0.045f, 0.10f, SilverBlue.Blue)
    DeckTheme.ORIGINAL -> LockSpot(0.05f, 0.165f, Color.White)
}

/** The padlock for the current theme, shown only while the player is up over the lock screen. */
@Composable
fun LockBadgeFor(ui: DeckUi) {
    val wake = rememberLockedWake()
    if (wake == 0) return
    val spot = lockSpot(ui.theme)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LockBadge(wake, spot.color, Modifier.offset(x = maxWidth * spot.x, y = maxHeight * spot.y))
    }
}

/** A small, simple, glowing padlock. Each new [wake] it starts open, then swings down and clicks shut. */
@Composable
fun LockBadge(wake: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val sounds = LocalMechSounds.current
    // turn: 0 = the shackle swung round to the far side, 1 = back over the body.
    // drop: 0 = popped up, 1 = pushed home.
    val turn = remember { Animatable(if (wake < 0) 1f else 0f) }
    val drop = remember { Animatable(if (wake < 0) 1f else 0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(wake) {
        if (wake < 0) return@LaunchedEffect
        turn.snapTo(0f)
        drop.snapTo(0f)
        // Held open long enough for the screen to come on and the open padlock to be seen.
        delay(1500)
        // Like a real padlock: the popped-up shackle swings round over the body...
        turn.animateTo(1f, tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        delay(60)
        // ...then is pushed down and clicks, with a flash of the glow.
        drop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 900f))
        sounds?.keyDown()
        flash.snapTo(1f)
        flash.animateTo(0f, tween(700))
    }
    Canvas(modifier.size(size).semantics { contentDescription = "The phone is locked" }) {
        drawPadlock(color, turn.value, drop.value.coerceIn(0f, 1.2f), flash.value)
    }
}

private fun DrawScope.drawPadlock(color: Color, turn: Float, drop: Float, flash: Float) {
    val w = size.width
    val h = size.height
    // Glow: a soft halo behind, brighter for a moment when it clicks.
    val glow = 0.45f + 0.55f * flash
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * glow), Color.Transparent), center, w * (0.95f + 0.35f * flash)), w * (0.95f + 0.35f * flash))
    val bodyTop = h * 0.46f
    val bodyW = w * 0.74f
    val left = (w - bodyW) / 2f
    val stroke = w * 0.12f
    val legL = left + bodyW * 0.2f
    val legR = left + bodyW * 0.8f
    val arcTop = h * 0.10f
    val radius = (legR - legL) / 2f
    // The shackle. Open, it has popped straight up and been swung round on its long (right) leg
    // to the far side; seen from the front that is the arch flipped over to the right. Swinging
    // back, it narrows to edge-on and opens out again over the body, then drops home.
    val lift = h * 0.16f * (1f - drop)
    val facing = kotlin.math.cos(Math.PI * (1f - turn)).toFloat()
    val shackle = Path().apply {
        moveTo(legL, bodyTop + stroke * 0.4f - lift)
        lineTo(legL, arcTop + radius - lift)
        arcTo(androidx.compose.ui.geometry.Rect(legL, arcTop - lift, legR, arcTop + radius * 2 - lift), 180f, 180f, false)
        lineTo(legR, bodyTop + stroke - lift)
    }
    scale(scaleX = if (kotlin.math.abs(facing) < 0.08f) 0.08f else facing, scaleY = 1f, pivot = Offset(legR, bodyTop)) {
        for (i in 3 downTo 1) drawPath(shackle, color.copy(alpha = 0.10f * glow), style = Stroke(stroke + i * w * 0.07f, cap = StrokeCap.Round))
        drawPath(shackle, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    // The body, with a keyhole.
    val r = CornerRadius(w * 0.10f)
    for (i in 3 downTo 1) {
        val g = i * w * 0.035f
        drawRoundRect(color.copy(alpha = 0.10f * glow), Offset(left - g, bodyTop - g), Size(bodyW + g * 2, h - bodyTop + g * 2), CornerRadius(r.x + g))
    }
    drawRoundRect(color, Offset(left, bodyTop), Size(bodyW, h - bodyTop), r)
    val hole = Color.Black.copy(alpha = 0.6f)
    val kc = Offset(w / 2f, bodyTop + (h - bodyTop) * 0.42f)
    drawCircle(hole, w * 0.075f, kc)
    drawLine(hole, kc, kc + Offset(0f, (h - bodyTop) * 0.28f), w * 0.075f, StrokeCap.Round)
}
