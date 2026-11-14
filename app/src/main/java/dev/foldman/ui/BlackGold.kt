// Foldy Streaman addition (not in the original Foldman): the "Black & Gold" theme for the front
// screen, and the empty deck interior shared by both themes.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckUi
import dev.foldman.deck.SourceStatus
import dev.foldman.deck.Transport
import kotlin.random.Random

/** Black & Gold palette. */
object BlackGold {
    val Body = Color(0xFF1C1C1F)
    val BodyLo = Color(0xFF121214)
    val Gold = Color(0xFFF2B51E)
    val GoldHi = Color(0xFFFFD45A)
    val GoldLo = Color(0xFFB98310)
    val Ink = Color(0xFF15130F)
    val Print = Color(0xFFE9E7E2)
}

/** Matte black anodised metal with a faint soft-touch grain. */
fun Modifier.matteBlack(): Modifier = drawWithCache {
    val rnd = Random(11)
    val dots = List(1800) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) to rnd.nextFloat() }
    val body = Brush.verticalGradient(listOf(Color(0xFF262629), BlackGold.Body, BlackGold.BodyLo))
    onDrawBehind {
        drawRect(body)
        dots.forEach { (o, a) -> drawCircle(Color.White.copy(alpha = 0.025f * a), 0.8f, o) }
    }
}

/** Brushed yellow-gold metal: a soft sheen across, with fine vertical brushing. */
fun Modifier.brushedGold(): Modifier = drawWithCache {
    val rnd = Random(5)
    val lines = List((size.width / 1.2f).toInt().coerceAtLeast(1)) { rnd.nextFloat() * size.width to rnd.nextFloat() }
    val sheen = Brush.horizontalGradient(listOf(BlackGold.GoldLo, BlackGold.Gold, BlackGold.GoldHi, BlackGold.Gold, Color(0xFFD99A14)))
    onDrawBehind {
        drawRect(sheen)
        lines.forEach { (x, a) ->
            drawLine(if (a > 0.5f) Color.White.copy(alpha = 0.06f * a) else Color.Black.copy(alpha = 0.05f * a), Offset(x, 0f), Offset(x, size.height), 0.8f)
        }
        // Bevelled edge where the band meets the black body.
        drawLine(Color.Black.copy(alpha = 0.45f), Offset(0f, 0f), Offset(0f, size.height), 2f)
        drawLine(Color.White.copy(alpha = 0.35f), Offset(2f, 0f), Offset(2f, size.height), 1f)
    }
}

/**
 * The folded phone in Black & Gold: a matte black body with the tape seen through a round
 * window, and a brushed-gold band down the side with round black knobs (a gold one for PLAY).
 */
@Composable
internal fun CoverBlackGold(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val print = TextStyle(fontFamily = FontFamily.SansSerif, color = BlackGold.Print.copy(alpha = 0.7f), fontSize = 8.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
    Row(modifier.matteBlack()) {
        Column(
            Modifier
                .weight(0.72f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                .padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ui.frontShelf) {
                // Settings > Tape shelf on the front screen: a small gold-edged TAPES button.
                Text(
                    "TAPES",
                    style = print.copy(color = BlackGold.Gold, fontSize = 10.sp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, BlackGold.Gold, RoundedCornerShape(4.dp))
                        .clickable(onClick = onTapes)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            } else {
                Text(changeTapesHint(ui), style = print.copy(fontSize = 7.sp))
            }
            Spacer(Modifier.weight(1f))
            Porthole(ui, anim, Modifier.fillMaxWidth(0.94f))
            Spacer(Modifier.height(22.dp))
            Text(
                listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                    .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                    .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                style = print.copy(color = BlackGold.Print, fontSize = 9.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (ui.status == SourceStatus.NeedsAccess) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "TAP: TURN ON NOTIFICATION ACCESS",
                    style = print.copy(color = BlackGold.Gold, fontSize = 9.sp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onFixAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("STEREO CASSETTE PLAYER", style = print.copy(fontSize = 7.sp))
        }
        GoldBand(ui, deck, Modifier.weight(0.28f).fillMaxHeight())
    }
}

/** The round window: a close-up of the tape and its reels behind glass. */
@Composable
private fun Porthole(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val d = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2A2A2D), Color(0xFF09090A)))),
            contentAlignment = Alignment.Center,
        ) {
            // The tape stood on end, a little bigger than the window so the reels fill it.
            val w = d * 1.02f
            val h = w * (100f / 64f)
            if (ui.empty) {
                EmptyDeckInterior(Modifier.requiredSize(w, h), metal = Color(0xFF6B6B70))
            } else {
                FullCassette(ui, anim, Modifier.requiredSize(w, h), vertical = true)
            }
        }
        // Glass and rim.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), c, r), r)
            drawArc(
                Color.White.copy(alpha = 0.16f), startAngle = 200f, sweepAngle = 70f, useCenter = false,
                topLeft = Offset(c.x - r * 0.82f, c.y - r * 0.82f), size = Size(r * 1.64f, r * 1.64f),
                style = Stroke(r * 0.07f),
            )
            drawCircle(Color.Black, r - 1.5f, c, style = Stroke(5.dp.toPx()))
            drawCircle(Brush.sweepGradient(listOf(Color(0xFF5A5A60), Color(0xFF151517), Color(0xFF5A5A60), Color(0xFF151517), Color(0xFF5A5A60)), c), r - 2.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            drawCircle(BlackGold.Gold.copy(alpha = 0.55f), r - 4.dp.toPx(), c, style = Stroke(0.8.dp.toPx()))
        }
    }
}

/** The brushed-gold band: lettering, light, skip knobs, the five keys and volume. */
@Composable
private fun GoldBand(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val knob = ((maxHeight - 400.dp) / 5 - 10.dp).coerceIn(34.dp, 58.dp)
        Column(
            Modifier
                .fillMaxSize()
                .brushedGold()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 8.dp, end = 10.dp, top = 30.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("streaman", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = BlackGold.Ink, fontSize = 15.sp, letterSpacing = 0.5.sp))
            Spacer(Modifier.height(10.dp))
            // A small white dot that lights while the tape runs.
            val lit = ui.transport == Transport.PLAYING || ui.transport == Transport.REWINDING || ui.transport == Transport.FAST_FORWARDING
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (lit) Color.White else Color(0xFF3A3326)),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KnobKey(KeySymbol.PREV, "Previous", deck.previous, 26.dp, latched = false, showSymbol = false)
                KnobKey(KeySymbol.NEXT, "Next", deck.next, 26.dp, latched = false, showSymbol = false)
            }
            Spacer(Modifier.height(14.dp))
            val t = ui.transport
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                KnobKey(KeySymbol.PLAY, "Play", deck.play, knob, latched = t == Transport.PLAYING || t == Transport.PAUSED, gold = true)
                KnobKey(KeySymbol.REW, "Rewind", deck.rewind, knob, latched = t == Transport.REWINDING)
                KnobKey(KeySymbol.FF, "Fast forward", deck.fastForward, knob, latched = t == Transport.FAST_FORWARDING)
                KnobKey(KeySymbol.PAUSE, "Pause", deck.pause, knob, latched = t == Transport.PAUSED)
                KnobKey(KeySymbol.STOP, "Stop", deck.stop, knob, latched = false)
            }
            Spacer(Modifier.height(14.dp))
            Text("VOL", style = EngravedCaps.copy(color = BlackGold.Ink, fontSize = 7.sp))
            VolumeSlider(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(8.dp))
            Text("PHONES", style = EngravedCaps.copy(color = BlackGold.Ink, fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(34.dp), dark = true)
        }
    }
}

/**
 * A round knob key seen from the front: a black cylinder that sinks when pressed or latched, with
 * its symbol printed beside it. Clicks and buzzes like the other keys.
 */
@Composable
private fun KnobKey(
    symbol: KeySymbol,
    label: String,
    onPress: () -> Unit,
    diameter: Dp,
    latched: Boolean,
    gold: Boolean = false,
    showSymbol: Boolean = true,
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val action by rememberUpdatedState(onPress)
    val depth by animateFloatAsState(
        when {
            pressed -> 1f
            latched -> 0.92f
            else -> 0f
        },
        spring(dampingRatio = 0.8f, stiffness = 420f), label = "knob",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showSymbol) {
            Canvas(Modifier.size(14.dp)) { drawSymbol(symbol, center, size.minDimension * 0.42f, BlackGold.Ink) }
            Spacer(Modifier.width(6.dp))
        }
        Canvas(
            Modifier
                .size(diameter)
                .semantics { role = Role.Button; contentDescription = label }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        pressed = true
                        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        sounds?.keyDown()
                        action()
                        tryAwaitRelease()
                        pressed = false
                        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                        sounds?.keyUp()
                    })
                },
        ) { drawKnob(depth, gold) }
    }
}

private fun DrawScope.drawKnob(depth: Float, gold: Boolean) {
    // The knob sits in a dark well in the gold band and sinks into it when pressed.
    drawCircle(Color(0xFF14110A), size.minDimension / 2f * 0.98f, center)
    drawCircle(Color.Black.copy(alpha = 0.5f), size.minDimension / 2f * 0.98f, center, style = Stroke(1.5f))
    val r = size.minDimension / 2f * (0.93f - 0.14f * depth)
    val c = center
    val drop = r * 0.18f * (1f - depth * 0.8f)
    // Shadow on the band, shorter as the knob goes in.
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 1.02f, c + Offset(drop * 0.6f, drop * 1.4f))
    // The cylinder's side, then its top face.
    val side = if (gold) listOf(Color(0xFF9C6E0C), Color(0xFF6E4C06)) else listOf(Color(0xFF3A3A3E), Color(0xFF18181A))
    drawCircle(Brush.verticalGradient(side, c.y - r, c.y + r + drop), r, c + Offset(0f, drop))
    val face = if (gold) {
        Brush.radialGradient(listOf(BlackGold.GoldHi, BlackGold.Gold, BlackGold.GoldLo), c - Offset(r * 0.35f, r * 0.4f), r * 1.5f)
    } else {
        // A slightly lighter black than the ring round it, so the knob stands out.
        Brush.radialGradient(listOf(Color(0xFF606066), Color(0xFF36363A), Color(0xFF232326)), c - Offset(r * 0.35f, r * 0.4f), r * 1.5f)
    }
    drawCircle(face, r * 0.94f, c)
    drawCircle(Color.White.copy(alpha = if (gold) 0.35f else 0.12f), r * 0.94f, c, style = Stroke(1.2f))
    // Darker when pressed in.
    if (depth > 0f) drawCircle(Color.Black.copy(alpha = 0.18f * depth), r * 0.94f, c)
}

/**
 * The inside of an empty deck, simply drawn: the outline where the tape sits, the two toothed
 * spindles its reels go on, and a screw between them. Sized like a tape stood on end (64 x 100).
 */
@Composable
fun EmptyDeckInterior(modifier: Modifier = Modifier, metal: Color = Color(0xFF8A8A90)) {
    Canvas(modifier) {
        val u = size.width / 64f
        // Where the tape goes: a faint moulded outline.
        drawRoundRect(
            Color.White.copy(alpha = 0.05f), topLeft = Offset(2f * u, 2f * u), size = Size(60f * u, 96f * u),
            cornerRadius = CornerRadius(3f * u), style = Stroke(0.6f * u),
        )
        // Spindles where the reels sit (the tape's hubs, stood on end).
        drawSpindle(Offset(32f * u, 28.75f * u), u, metal)
        drawSpindle(Offset(32f * u, 71.25f * u), u, metal)
        // A screw holding the mechanism plate, and two small guide pins.
        drawScrewHead(Offset(32f * u, 50f * u), 2.1f * u, metal)
        drawCircle(metal.copy(alpha = 0.7f), 1.1f * u, Offset(12f * u, 50f * u))
        drawCircle(metal.copy(alpha = 0.7f), 1.1f * u, Offset(52f * u, 50f * u))
    }
}

private fun DrawScope.drawSpindle(c: Offset, u: Float, metal: Color) {
    // Base collar.
    drawCircle(Color.Black.copy(alpha = 0.5f), 7.2f * u, c + Offset(0f, 0.5f * u))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3A3E), Color(0xFF1A1A1C)), c, 7f * u), 7f * u, c)
    drawCircle(Color.White.copy(alpha = 0.06f), 7f * u, c, style = Stroke(0.4f * u))
    // Toothed hub.
    drawCircle(Brush.radialGradient(listOf(metal, metal.copy(alpha = 0.6f)), c, 3.4f * u), 3.2f * u, c)
    for (i in 0 until 6) {
        rotate(i * 60f, pivot = c) {
            drawRoundRect(metal, topLeft = Offset(c.x - 0.55f * u, c.y - 4.4f * u), size = Size(1.1f * u, 1.6f * u), cornerRadius = CornerRadius(0.3f * u))
        }
    }
    drawCircle(Color(0xFF111112), 1.1f * u, c)
}

private fun DrawScope.drawScrewHead(c: Offset, r: Float, metal: Color) {
    drawCircle(Brush.radialGradient(listOf(metal, metal.copy(alpha = 0.55f)), c - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, c)
    val slot = Path().apply {
        moveTo(c.x - r * 0.7f, c.y); lineTo(c.x + r * 0.7f, c.y)
        moveTo(c.x, c.y - r * 0.7f); lineTo(c.x, c.y + r * 0.7f)
    }
    drawPath(slot, Color(0xFF1A1A1C), style = Stroke(r * 0.22f))
}

/** The hint at the top of the front screen: unfold on a foldable, EJECT on a normal phone. */
fun changeTapesHint(ui: DeckUi): String = if (ui.phoneMode) "PRESS EJECT TO CHANGE TAPES" else "UNFOLD TO CHANGE TAPES"
