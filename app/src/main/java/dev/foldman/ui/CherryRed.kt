// Foldy Streaman addition (not in the original Foldman): the "Cherry Red" theme for the front
// screen, after an early-80s auto-reverse player: glossy coral-red plastic, a dark smoked window,
// big outlined lettering, and small chrome keys with grip slits in a black panel.
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

/** Cherry Red palette. */
object CherryRed {
    val Body = Color(0xFFE23A55)
    val BodyHi = Color(0xFFEA4C64)
    val BodyLo = Color(0xFFB8243D)
    val Trim = Color(0xFF151416)
    val Yellow = Color(0xFFFFC93C)
}

/** Glossy injection-moulded plastic: a bright sheen up top, a black trim band along the bottom. */
internal fun Modifier.glossyRed(trim: Dp): Modifier = drawWithCache {
    val body = Brush.verticalGradient(listOf(CherryRed.BodyHi, CherryRed.Body, CherryRed.BodyLo))
    val sheen = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
        start = Offset(0f, 0f), end = Offset(size.width * 0.6f, size.height * 0.35f),
    )
    val t = trim.toPx()
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
        // The black trim band, with a crisp edge where the red shell sits on it.
        drawRect(CherryRed.Trim, topLeft = Offset(0f, size.height - t), size = Size(size.width, t))
        drawLine(Color.Black.copy(alpha = 0.5f), Offset(0f, size.height - t), Offset(size.width, size.height - t), 2f)
        drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, size.height - t - 2f), Offset(size.width, size.height - t - 2f), 1f)
    }
}

/** The folded phone in Cherry Red. */
@Composable
internal fun CoverCherryRed(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val white = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White, fontWeight = FontWeight.Bold)
    Row(modifier.glossyRed(trim = 34.dp)) {
        Column(
            Modifier
                .weight(0.72f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                .padding(start = 18.dp, end = 14.dp, top = 22.dp, bottom = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top label: a small printed panel, like the badge beside the window.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(14.dp, 9.dp).background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(1.dp)))
                Spacer(Modifier.width(6.dp))
                Text("TYPE I · NORMAL", style = white.copy(fontSize = 7.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.85f)))
                Spacer(Modifier.weight(1f))
                if (ui.frontShelf) {
                    Text(
                        "TAPES",
                        style = white.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CherryRed.Trim)
                            .clickable(onClick = onTapes)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                } else {
                    Text(changeTapesHint(ui), style = white.copy(fontSize = 6.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.8f)))
                }
            }
            Spacer(Modifier.height(14.dp))
            SmokedWindow(ui, anim, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(14.dp))
            // The big outlined lettering across the front.
            Text(
                "AUTO REVERSE",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontStyle = FontStyle.Normal,
                    fontSize = 35.sp, letterSpacing = 0.5.sp, color = Color.White,
                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.6f),
                ),
                maxLines = 1, softWrap = false,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                        .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                        .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                    style = white.copy(fontSize = 8.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.9f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text("STEREO", style = white.copy(color = CherryRed.Yellow, fontSize = 11.sp, letterSpacing = 1.sp, fontStyle = FontStyle.Italic))
            }
            if (ui.status == SourceStatus.NeedsAccess) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "TAP: TURN ON NOTIFICATION ACCESS",
                    style = white.copy(fontSize = 9.sp, color = CherryRed.Yellow),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onFixAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        KeyPanel(ui, deck, Modifier.weight(0.28f).fillMaxHeight())
    }
}

/** The dark smoked window: tinted glass with only a faint view of the tape behind it. */
@Composable
private fun SmokedWindow(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // A rounded rectangle a little taller than wide, as big as fits.
        val w = minOf(maxWidth, maxHeight * 0.86f)
        val h = w / 0.86f
        val shape = RoundedCornerShape(10.dp)
        Box(
            Modifier
                .size(w, h)
                .clip(shape)
                .background(Color(0xFF1C1A1D))
                .border(2.dp, Color.Black.copy(alpha = 0.55f), shape),
            contentAlignment = Alignment.Center,
        ) {
            // The tape, stood on end, filling the window so the reels sit in the middle.
            val tw = w * 1.04f
            val th = tw * (100f / 64f)
            if (ui.empty) {
                EmptyDeckInterior(Modifier.requiredSize(tw, th), metal = Color(0xFF5E5C60))
            } else {
                FullCassette(ui, anim, Modifier.requiredSize(tw, th), vertical = true)
            }
            // Smoke: a heavy dark tint, a soft diagonal reflection and a glare at the top.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF141216).copy(alpha = 0.74f))
                drawPath(
                    Path().apply {
                        moveTo(size.width * 0.55f, 0f); lineTo(size.width * 0.82f, 0f)
                        lineTo(size.width * 0.32f, size.height); lineTo(size.width * 0.05f, size.height); close()
                    },
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent), size.width * 0.05f, size.width * 0.82f),
                )
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), 0f, size.height * 0.25f))
            }
            // A notification, when one arrives, comes in over the darkened window.
            NoticeOverlay(NoticeStyle.WINDOW, Modifier.fillMaxSize())
        }
    }
}

/** The black control panel down the side: chrome keys, volume and the phones socket. */
@Composable
private fun KeyPanel(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val keyH = ((maxHeight - 400.dp) / 5 - 16.dp).coerceIn(30.dp, 50.dp)
        val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color(0xFFD9D9DC), fontSize = 7.sp, letterSpacing = 1.sp)
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF0B0B0C), CherryRed.Trim, Color(0xFF232225))))
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 10.dp, end = 10.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // A red running light, like the small LED on the original.
            val lit = ui.transport == Transport.PLAYING || ui.transport == Transport.REWINDING || ui.transport == Transport.FAST_FORWARDING
            BattLed(lit, on = Color(0xFFFF3B3B), off = Color(0xFF3A1D20))
            Spacer(Modifier.height(2.dp))
            Text("BATT", style = caption.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChromeKey(KeySymbol.PREV, "AMS", false, deck.previous, 22.dp, Modifier.weight(1f))
                ChromeKey(KeySymbol.NEXT, "AMS", false, deck.next, 22.dp, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            val t = ui.transport
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChromeKey(KeySymbol.PLAY, "PLAY", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, keyH, Modifier.fillMaxWidth(), play = true)
                ChromeKey(KeySymbol.REW, "REW", t == Transport.REWINDING, deck.rewind, keyH, Modifier.fillMaxWidth())
                ChromeKey(KeySymbol.FF, "FF", t == Transport.FAST_FORWARDING, deck.fastForward, keyH, Modifier.fillMaxWidth())
                ChromeKey(KeySymbol.PAUSE, "PAUSE", t == Transport.PAUSED, deck.pause, keyH, Modifier.fillMaxWidth())
                ChromeKey(KeySymbol.STOP, "STOP·EJ", false, deck.stop, keyH, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            VolumeWheelSlot(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f), ink = Color(0xFFD9D9DC), marker = CherryRed.Yellow)
            Spacer(Modifier.height(6.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp), dark = true)
        }
    }
}

/**
 * A small chrome key with black grip slits, printed caption above it. Sinks when pressed or
 * latched, with the usual click and buzz. [play] adds a little red dot, the PLAY key's marking.
 */
@Composable
private fun ChromeKey(
    symbol: KeySymbol,
    caption: String,
    latched: Boolean,
    onPress: () -> Unit,
    height: Dp,
    modifier: Modifier,
    play: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val sounds = LocalMechSounds.current
    val haptics = LocalHaptics.current
    val action by rememberUpdatedState(onPress)
    val depth by animateFloatAsState(
        when {
            pressed -> 1.14f
            latched -> 0.9f
            else -> 0f
        },
        spring(dampingRatio = 0.82f, stiffness = 1100f), label = "chromeKey",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color(0xFFD9D9DC), fontSize = 7.sp, letterSpacing = 1.sp), maxLines = 1)
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
        ) { keyWell(depth, CornerRadius(size.height * 0.12f)) { drawChromeKey(symbol, depth, play) } }
    }
}

private fun DrawScope.drawChromeKey(symbol: KeySymbol, depth: Float, play: Boolean) {
    val w = size.width
    val h = size.height
    val box = keyBox(depth)
    val top = box.top
    val faceH = box.faceH
    val r = CornerRadius(h * 0.12f)
    // The well in the black panel, and the cap's front edge while it stands proud.
    drawKeyLip(box, r, listOf(Color(0xFF7B7F86), Color(0xFF3C3F44)))
    // Chrome cap: bright band, dark band, bright again, like polished metal.
    val shade = 0.2f * depth
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFC9CCD1), Color(0xFF7D8187), Color(0xFFD7DADE), Color(0xFF9DA1A7)).map { lerpBlack(it, shade) },
            top, top + faceH,
        ),
        topLeft = Offset(0f, top), size = Size(w, faceH), cornerRadius = r,
    )
    // Black grip slits along the right of the key.
    val slitL = w * 0.5f
    val slitR = w * 0.9f
    for (i in 1..4) {
        val y = top + faceH * (0.18f + 0.16f * i)
        drawLine(Color(0xFF151515), Offset(slitL, y), Offset(slitR, y), faceH * 0.06f)
    }
    drawRoundRect(Color.White.copy(alpha = 0.7f), topLeft = Offset(0.8f, top + 0.8f), size = Size(w - 1.6f, faceH - 1.6f), cornerRadius = r, style = Stroke(1f))
    // The symbol, engraved on the left; PLAY gets its red dot.
    val c = Offset(w * 0.25f, top + faceH * 0.5f)
    drawSymbol(symbol, c, minOf(w, faceH) * 0.2f, Color(0xFF1C1C1E))
    if (play) drawCircle(Color(0xFFE5303F), faceH * 0.08f, Offset(w * 0.25f, top + faceH * 0.84f))
}

private fun lerpBlack(c: Color, t: Float) = Color(c.red * (1 - t), c.green * (1 - t), c.blue * (1 - t), c.alpha)
