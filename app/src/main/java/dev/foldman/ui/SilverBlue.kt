// Foldy Streaman addition (not in the original Foldman): the "Silver & Blue" theme for the front
// screen, after an early-80s aluminium player: brushed silver all over, a long pill-shaped clear
// window in a thick royal-blue frame, and slanted outlined silver keys.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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

/** Silver & Blue palette. */
object SilverBlue {
    val Blue = Color(0xFF2347C6)
    val BlueHi = Color(0xFF4C6EE6)
    val BlueLo = Color(0xFF15308F)
    val Green = Color(0xFF2FA35A)
    val Ink = Color(0xFF1E2A66)
}

/** The folded phone in Silver & Blue. */
@Composable
internal fun CoverSilverBlue(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val blue = TextStyle(fontFamily = FontFamily.SansSerif, color = SilverBlue.Ink, fontWeight = FontWeight.Bold)
    Row(modifier.chassis(corner = 0.dp, screws = false)) {
        Column(
            Modifier
                .weight(0.72f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                .padding(start = 18.dp, end = 14.dp, top = 22.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("STEREO CASSETTE PLAYER", style = blue.copy(fontSize = 7.sp, letterSpacing = 1.5.sp))
                Spacer(Modifier.weight(1f))
                if (ui.frontShelf) {
                    TapesButton(SilverBlue.Blue, Color.White, onTapes)
                } else {
                    Text(changeTapesHint(ui), style = blue.copy(fontSize = 6.sp, letterSpacing = 1.sp, color = SilverBlue.Ink.copy(alpha = 0.75f)))
                }
            }
            Spacer(Modifier.height(10.dp))
            // A thin blue rule, like the printed lines on the case.
            Box(Modifier.fillMaxWidth().height(2.dp).background(SilverBlue.Blue))
            Spacer(Modifier.height(14.dp))
            PillWindow(ui, anim, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(14.dp))
            Text(
                listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                    .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                    .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                style = blue.copy(fontSize = 8.sp, letterSpacing = 1.5.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (ui.status == SourceStatus.NeedsAccess) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "TAP: TURN ON NOTIFICATION ACCESS",
                    style = blue.copy(fontSize = 9.sp, color = Color.White),
                    modifier = Modifier
                        .background(SilverBlue.Blue, RoundedCornerShape(4.dp))
                        .clickable(onClick = onFixAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(SilverBlue.Blue))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("streaman", style = blue.copy(color = SilverBlue.Blue, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp))
                Spacer(Modifier.weight(1f))
                Text("AUTO SHUT-OFF", style = blue.copy(fontSize = 7.sp, letterSpacing = 1.sp))
            }
        }
        SlantKeyStrip(ui, deck, Modifier.weight(0.28f).fillMaxHeight())
    }
}

/**
 * The window stood on end: a blue pill-shaped frame, and inside it a clear window with flat sides
 * and softly rounded corners that sits tight round the tape (its title and the reels), leaving
 * the blue thicker at the ends.
 */
@Composable
private fun PillWindow(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The part of the upright tape the window shows, in the tape's own 64 x 100 units:
        // from just left of the title to the right edge of the tape window, top to bottom of the label.
        val showL = 10.5f
        val showR = 43.5f
        val showT = 13f
        val showB = 87f
        val aspect = (showR - showL) / (showB - showT)
        val sideFrame = 1.28f
        val endFrame = 0.62f
        // Fit the frame (window plus blue) in the space available.
        val winW = minOf(maxWidth * 0.8f / sideFrame, maxHeight / (1f / aspect + sideFrame * endFrame))
        val winH = winW / aspect
        val frameW = winW * sideFrame
        val frameH = winH + frameW * endFrame
        Box(
            Modifier
                .size(frameW, frameH)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(SilverBlue.BlueLo, SilverBlue.BlueHi, SilverBlue.Blue, SilverBlue.BlueLo))),
            contentAlignment = Alignment.Center,
        ) {
            val glass = RoundedCornerShape(winW * 0.12f)
            Box(
                Modifier
                    .size(winW, winH)
                    .clip(glass)
                    .background(Color(0xFFD9DDE3)),
                contentAlignment = Alignment.Center,
            ) {
                // Scale the tape so the shown part fills the window, then centre that part.
                val u = winW / (showR - showL)
                val tw = u * 64f
                val th = u * 100f
                val dx = u * (32f - (showL + showR) / 2f)
                val dy = u * (50f - (showT + showB) / 2f)
                val place = Modifier.requiredSize(tw, th).offset(x = dx, y = dy)
                if (ui.empty) {
                    EmptyDeckInterior(place, metal = Color(0xFF9A9EA6))
                } else {
                    FullCassette(ui, anim, place, vertical = true)
                }
                // Clear glass: barely tinted, with fine reflections.
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFBFD3FF).copy(alpha = 0.08f))
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.15f, 0f); lineTo(size.width * 0.38f, 0f)
                            lineTo(size.width * 0.12f, size.height); lineTo(0f, size.height * 0.9f); close()
                        },
                        Color.White.copy(alpha = 0.14f),
                    )
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(size.width * 0.75f, size.height * 0.04f), Offset(size.width * 0.55f, size.height * 0.96f), 1.5f)
                }
                // A notification, when one arrives, slides in from the right, lettered up the window.
                NoticeOverlay(NoticeStyle.SIDEWAYS, Modifier.fillMaxSize())
            }
            // Thin chrome edge where the glass meets the blue frame.
            Canvas(Modifier.size(winW + 4.dp, winH + 4.dp)) {
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFF8E939B), Color.White)),
                    cornerRadius = CornerRadius((winW * 0.12f).toPx() + 2.dp.toPx()), style = Stroke(2.dp.toPx()),
                )
            }
        }
    }
}

/** The side strip: brushed silver with a blue edge line, and the slanted keys. */
@Composable
private fun SlantKeyStrip(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val keyH = ((maxHeight - 400.dp) / 5 - 16.dp).coerceIn(30.dp, 50.dp)
        val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = SilverBlue.Ink, fontSize = 7.sp, letterSpacing = 1.sp)
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF9EA2A9), Color(0xFFD7D9DD), Color(0xFFBFC2C7))))
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 10.dp, end = 10.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val lit = ui.transport == Transport.PLAYING || ui.transport == Transport.REWINDING || ui.transport == Transport.FAST_FORWARDING
            BattLed(lit, on = Color(0xFFFF4A3A), off = Color(0xFF5E4440))
            Spacer(Modifier.height(2.dp))
            Text("BATT", style = caption.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlantKey(KeySymbol.PREV, "AMS", false, deck.previous, 22.dp, Modifier.weight(1f))
                SlantKey(KeySymbol.NEXT, "AMS", false, deck.next, 22.dp, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            val t = ui.transport
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SlantKey(KeySymbol.PLAY, "PLAY", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, keyH, Modifier.fillMaxWidth(), edge = SilverBlue.Green)
                SlantKey(KeySymbol.REW, "REW", t == Transport.REWINDING, deck.rewind, keyH, Modifier.fillMaxWidth())
                SlantKey(KeySymbol.FF, "FF", t == Transport.FAST_FORWARDING, deck.fastForward, keyH, Modifier.fillMaxWidth())
                SlantKey(KeySymbol.PAUSE, "PAUSE", t == Transport.PAUSED, deck.pause, keyH, Modifier.fillMaxWidth())
                SlantKey(KeySymbol.STOP, "STOP·EJECT", false, deck.stop, keyH, Modifier.fillMaxWidth(), edge = SilverBlue.Blue)
            }
            Spacer(Modifier.height(12.dp))
            VolumeWheelSlot(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f), ink = SilverBlue.Ink, marker = SilverBlue.Blue)
            Spacer(Modifier.height(6.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp))
        }
        // The blue edge line down the strip, where it meets the body.
        Box(Modifier.fillMaxHeight().width(3.dp).background(SilverBlue.Blue))
    }
}

/**
 * A slanted silver key: its face leans back like the keys along the top of the original, with a
 * dark outline in its own shape (green for PLAY, blue for STOP). Click and buzz as usual.
 */
@Composable
private fun SlantKey(
    symbol: KeySymbol,
    caption: String,
    latched: Boolean,
    onPress: () -> Unit,
    height: Dp,
    modifier: Modifier,
    edge: Color? = null,
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
        spring(dampingRatio = 0.82f, stiffness = 1100f), label = "slantKey",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = SilverBlue.Ink, fontSize = 7.sp, letterSpacing = 1.sp), maxLines = 1)
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
        ) { drawSlantKey(symbol, depth, edge) }
    }
}

private fun DrawScope.drawSlantKey(symbol: KeySymbol, depth: Float, edge: Color?) {
    val w = size.width
    val h = size.height
    // Small keys (the skip pair) get a proportionally thinner outline.
    val k = (h / 130f).coerceIn(0.5f, 1f)
    val pad = 18f * k
    val lean = w * 0.1f
    val wellH = h - pad * 2
    // The slanted outline of the well: a parallelogram leaning back to the right.
    fun xL(y: Float) = pad + lean * (1f - (y - pad) / wellH)
    fun xR(y: Float) = w - pad - lean * ((y - pad) / wellH)
    fun slab(y0: Float, y1: Float) = Path().apply { moveTo(xL(y0), y0); lineTo(xR(y0), y0); lineTo(xR(y1), y1); lineTo(xL(y1), y1); close() }
    val well = slab(pad, h - pad)
    // The border, twice as thick as before: dark for most keys, coloured for PLAY (green) and
    // STOP·EJECT (blue). Inside it is the dark well the key sinks into.
    drawPath(well, edge ?: Color(0xFF3B3E44), style = Stroke((if (edge == null) 32f else 22f) * k, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawPath(well, Color(0xFF121214))
    // These keys are flatter: raised, the cap has a thin front edge and just a sliver of the
    // dark well under it. Pressed, it slides down into that dark (its bottom tucking under the
    // border), the dark shows above it, and the border's shadow falls across its top.
    val lip = wellH * 0.07f
    val sliver = wellH * 0.08f
    val shift = (lip + sliver + 2f) * depth
    val top = pad + shift
    val faceBottom = top + (wellH - lip - sliver)
    val shade = 0.2f * depth
    clipPath(well) {
        drawPath(slab(faceBottom - 1f, faceBottom + lip), Brush.verticalGradient(listOf(Color(0xFF8B9097), Color(0xFF4E5258)), faceBottom, faceBottom + lip))
        drawPath(
            slab(top, faceBottom),
            Brush.verticalGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFFD5D8DD), Color(0xFF9CA1A8), Color(0xFFCDD0D5)).map { Color(it.red * (1 - shade), it.green * (1 - shade), it.blue * (1 - shade)) },
                top, faceBottom,
            ),
        )
        // A bright top edge while raised, and fine ridges across the face for grip.
        drawLine(Color.White.copy(alpha = (0.9f * (1f - depth)).coerceIn(0f, 1f)), Offset(xL(pad) + 2f, top + 1.5f), Offset(xR(pad) - 2f, top + 1.5f), 2.5f)
        for (i in 1..3) {
            val y = top + (faceBottom - top) * (0.18f * i)
            drawLine(Color.Black.copy(alpha = 0.10f), Offset(xL(pad) + w * 0.08f, y), Offset(xR(h - pad) - w * 0.08f, y), 1f)
        }
        if (depth > 0.01f) {
            val reach = (faceBottom - top) * 0.4f
            drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = (0.55f * depth).coerceIn(0f, 1f)), Color.Transparent), top, top + reach), topLeft = Offset(0f, top), size = Size(w, reach))
        }
    }
    drawSymbol(symbol, Offset(w / 2f, top + (faceBottom - top) * 0.55f), minOf(w, faceBottom - top) * 0.24f, Color(0xFF2A2D33))
}
