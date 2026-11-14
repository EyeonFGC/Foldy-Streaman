// Foldy Streaman addition (not in the original Foldman): the "Red Chrome" theme for the front
// screen, after an early-80s auto-reverse player: metallic red body, a big black glass panel with a
// white printed label, a chrome band with a green button, and a big white wordmark.
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckUi
import dev.foldman.deck.SourceStatus
import dev.foldman.deck.Transport
import kotlin.random.Random

/** Red Chrome palette. */
object RedChrome {
    val Red = Color(0xFFC81E2B)
    val RedHi = Color(0xFFE2414C)
    val RedLo = Color(0xFF8E121C)
    val Green = Color(0xFF2FB04A)
    val Glass = Color(0xFF0E0F12)
}

private val ChromeBand = listOf(Color(0xFF8C9096), Color(0xFFF7F8F9), Color(0xFFC6C9CE), Color(0xFF7B7F86))

/** Metallic red paint: a deep red with a soft sheen and fine brushing. */
internal fun Modifier.metallicRed(): Modifier = drawWithCache {
    val rnd = Random(9)
    val lines = List((size.height / 2.2f).toInt().coerceAtLeast(1)) { rnd.nextFloat() * size.height to rnd.nextFloat() }
    val body = Brush.verticalGradient(listOf(RedChrome.RedHi, RedChrome.Red, RedChrome.RedLo))
    val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset.Zero, Offset(size.width * 0.7f, size.height * 0.4f))
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
        lines.forEach { (y, a) ->
            drawLine(if (a > 0.5f) Color.White.copy(alpha = 0.05f * a) else Color.Black.copy(alpha = 0.06f * a), Offset(0f, y), Offset(size.width, y), 0.8f)
        }
    }
}

/** The folded phone in Red Chrome. */
@Composable
internal fun CoverRedChrome(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val white = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White, fontWeight = FontWeight.Bold)
    Row(modifier.metallicRed()) {
        Column(
            Modifier
                .weight(0.72f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                .padding(start = 16.dp, end = 12.dp, top = 20.dp, bottom = 18.dp),
        ) {
            // The maker's name, top left, in white serif capitals.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("FOLDY", style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp, letterSpacing = 3.sp))
                Spacer(Modifier.weight(1f))
                if (ui.frontShelf) {
                    Text(
                        "TAPES",
                        style = white.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(RedChrome.Glass)
                            .clickable(onClick = onTapes)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                } else {
                    Text(changeTapesHint(ui), style = white.copy(fontSize = 6.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.8f)))
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassPanel(ui, anim, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(10.dp))
            // The chrome band across the body, with the green button at its left end.
            Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Brush.verticalGradient(ChromeBand), RoundedCornerShape(2.dp))
                        .border(0.6.dp, Color.Black.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
                )
                val lit = ui.transport == Transport.PLAYING || ui.transport == Transport.REWINDING || ui.transport == Transport.FAST_FORWARDING
                Box(
                    Modifier
                        .padding(start = 14.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(if (lit) listOf(Color(0xFF8CFFA0), RedChrome.Green) else listOf(Color(0xFF58C96E), Color(0xFF1E7A33))))
                        .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape),
                )
            }
            Spacer(Modifier.height(6.dp))
            // The big white wordmark.
            Text(
                "STREAMAN",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = Color.White, fontSize = 38.sp, letterSpacing = 1.sp),
                maxLines = 1,
            )
            Text(
                listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                    .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                    .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                style = white.copy(fontSize = 8.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.9f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (ui.status == SourceStatus.NeedsAccess) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "TAP: TURN ON NOTIFICATION ACCESS",
                    style = white.copy(fontSize = 9.sp, color = Color(0xFFFFE07A)),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onFixAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        RedKeyStrip(ui, deck, Modifier.weight(0.28f).fillMaxHeight())
    }
}

/**
 * The big black glass panel: the tape shows faintly behind dark glass, with a white printed label
 * top left and AUTO REVERSE lettered along the bottom.
 */
@Composable
private fun GlassPanel(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val shape = RoundedCornerShape(8.dp)
        // The tape stood on end, sized to the panel.
        val tw = minOf(maxWidth * 0.92f, maxHeight * 0.6f)
        val th = tw * (100f / 64f)
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(RedChrome.Glass)
                .border(1.5.dp, Color.Black.copy(alpha = 0.6f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (ui.empty) {
                EmptyDeckInterior(Modifier.requiredSize(tw, th), metal = Color(0xFF5E5C60))
            } else {
                FullCassette(ui, anim, Modifier.requiredSize(tw, th), vertical = true)
            }
            // Dark glass over it, with a long reflection.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF0B0C10).copy(alpha = 0.72f))
                drawPath(
                    Path().apply {
                        moveTo(size.width * 0.5f, 0f); lineTo(size.width * 0.8f, 0f)
                        lineTo(size.width * 0.3f, size.height); lineTo(0f, size.height); close()
                    },
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.09f), Color.Transparent), 0f, size.width * 0.8f),
                )
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), 0f, size.height * 0.2f))
            }
            // The white printed label, top left, slightly askew like a sticker.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
                    .rotate(-4f)
                    .background(Color(0xFFF4F4F2), RoundedCornerShape(1.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text("STEREO", style = TextStyle(color = Color(0xFF16171A), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                Text("CASSETTE PLAYER", style = TextStyle(color = Color(0xFF16171A), fontSize = 6.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
            }
            Text(
                "AUTO REVERSE",
                style = TextStyle(fontFamily = FontFamily.Serif, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, letterSpacing = 1.sp, fontStyle = FontStyle.Italic),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp),
            )
        }
    }
}

/** The side strip: red body with a chrome edge, silver keys (a green one for PLAY), wheel and socket. */
@Composable
private fun RedKeyStrip(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val keyH = ((maxHeight - 400.dp) / 5 - 16.dp).coerceIn(30.dp, 50.dp)
        val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 7.sp, letterSpacing = 1.sp)
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.08f))))
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 12.dp, end = 10.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("BATT", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SilverKey(KeySymbol.PREV, "AMS", false, deck.previous, 22.dp, Modifier.weight(1f))
                SilverKey(KeySymbol.NEXT, "AMS", false, deck.next, 22.dp, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            val t = ui.transport
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SilverKey(KeySymbol.PLAY, "PLAY", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, keyH, Modifier.fillMaxWidth(), green = true)
                SilverKey(KeySymbol.REW, "REW", t == Transport.REWINDING, deck.rewind, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.FF, "FF", t == Transport.FAST_FORWARDING, deck.fastForward, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.PAUSE, "PAUSE", t == Transport.PAUSED, deck.pause, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.STOP, "STOP·EJECT", false, deck.stop, keyH, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            VolumeWheelSlot(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f), ink = Color.White, marker = RedChrome.Green)
            Spacer(Modifier.height(6.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(34.dp))
        }
        // The chrome edge where the strip meets the body.
        Box(Modifier.fillMaxHeight().width(5.dp).background(Brush.horizontalGradient(ChromeBand)))
    }
}

/** A flat silver key with a ribbed face; PLAY is green. Clicks and buzzes like the others. */
@Composable
private fun SilverKey(
    symbol: KeySymbol,
    caption: String,
    latched: Boolean,
    onPress: () -> Unit,
    height: Dp,
    modifier: Modifier,
    green: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val action by rememberUpdatedState(onPress)
    val depth by animateFloatAsState(
        when {
            pressed -> 1f
            latched -> 0.7f
            else -> 0f
        },
        spring(dampingRatio = 0.5f, stiffness = 2200f), label = "silverKey",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 7.sp, letterSpacing = 1.sp), maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { role = Role.Button; contentDescription = caption }
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
        ) { drawSilverKey(symbol, depth, green) }
    }
}

private fun DrawScope.drawSilverKey(symbol: KeySymbol, depth: Float, green: Boolean) {
    val w = size.width
    val h = size.height
    val travel = h * 0.14f
    val top = travel * depth
    val faceH = h - travel
    val r = CornerRadius(h * 0.14f)
    // The slot in the body.
    drawRoundRect(Color.Black.copy(alpha = 0.75f), topLeft = Offset(-2f, -2f), size = Size(w + 4f, h + 4f), cornerRadius = r)
    val shade = 1f - 0.14f * depth
    val cap = if (green) listOf(Color(0xFF7BE08E), RedChrome.Green, Color(0xFF1C7A31)) else listOf(Color(0xFFFFFFFF), Color(0xFFCDD0D5), Color(0xFF8F949B))
    drawRoundRect(
        Brush.verticalGradient(cap.map { Color(it.red * shade, it.green * shade, it.blue * shade) }, top, top + faceH),
        topLeft = Offset(0f, top), size = Size(w, faceH), cornerRadius = r,
    )
    // Fine ribs across the face.
    for (i in 1..5) {
        val y = top + faceH * (0.14f * i)
        drawLine(Color.Black.copy(alpha = 0.10f), Offset(w * 0.1f, y), Offset(w * 0.9f, y), 1f)
    }
    drawRoundRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(0.8f, top + 0.8f), size = Size(w - 1.6f, faceH - 1.6f), cornerRadius = r, style = Stroke(1f))
    drawSymbol(symbol, Offset(w / 2f, top + faceH * 0.5f), minOf(w, faceH) * 0.2f, if (green) Color.White else Color(0xFF24262A))
}
