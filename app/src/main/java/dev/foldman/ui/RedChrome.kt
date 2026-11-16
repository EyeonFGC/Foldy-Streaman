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
import androidx.compose.foundation.layout.aspectRatio
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

/** Anton: a free font in the style of Impact (licence in assets/font-licenses). */
private val ImpactStyle = FontFamily(androidx.compose.ui.text.font.Font(dev.foldman.R.font.impact_style))

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
                .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 18.dp),
        ) {
            // Top line: the hint (or TAPES button) on the left, the maker's name on the right.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (ui.frontShelf) {
                    TapesButton(Color(0xFF26272C), Color.White, onTapes)
                } else {
                    Text(changeTapesHint(ui), style = white.copy(fontSize = 6.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.8f)))
                }
                Spacer(Modifier.weight(1f))
                Text("FOLDY", style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp, letterSpacing = 3.sp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                // The wordmark runs up the side, the song title beside it (under it, read sideways).
                // They start at the bottom, level with where the black panel's corner begins to curve.
                Box(Modifier.fillMaxHeight().padding(bottom = 8.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(Modifier.quarterTurn()) {
                        Text(
                            "STREAMAN",
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = Color.White, fontSize = 44.sp, letterSpacing = 2.sp),
                            maxLines = 1, softWrap = false,
                        )
                        Text(
                            listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                                .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                                .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                            style = white.copy(fontSize = 8.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.9f)),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(250.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                GlassPanel(ui, anim, Modifier.weight(1f).fillMaxHeight())
            }
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
 * The black panel with its window: the window sits tight round the tape (title strip to the far
 * edge), a white printed label above it, AUTO REVERSE below, and a white and a black triangle
 * lined up with the two sides of the tape's own window.
 */
@Composable
private fun GlassPanel(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val shape = RoundedCornerShape(8.dp)
        // The part of the upright tape the window shows, in the tape's own 64 x 100 units.
        val showL = 11f
        // The right edge comes in past the tape's label, cutting off its outer part.
        val showR = 58f
        val showT = 10f
        val showB = 95f
        val aspect = (showR - showL) / (showB - showT)
        val winW = minOf(maxWidth - 16.dp, (maxHeight - 96.dp) * aspect)
        val winH = winW / aspect
        val u = winW / (showR - showL)
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF1B1C21), RedChrome.Glass, Color(0xFF07080A))))
                .border(1.5.dp, Color.Black.copy(alpha = 0.6f), shape)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // A white wedge with a thin blue edge above the window, hugging the top-left corner.
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawWedge(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close() })
                }
                // Locked: a white padlock in the middle of the black triangle left beside the wedge
                // (the centre of the biggest circle that fits in it).
                val r = (maxWidth + maxHeight - kotlin.math.hypot(maxWidth.value, maxHeight.value).dp) / 2
                val lock = minOf(24.dp, r * 1.25f)
                LockBadgeHere(ui, Color.White, Modifier.align(Alignment.BottomEnd).offset(x = -(r - lock / 2), y = -(r - lock / 2)), size = lock)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .size(winW, winH)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF17181C))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val place = Modifier
                    .requiredSize(u * 64f, u * 100f)
                    .offset(x = u * (32f - (showL + showR) / 2f), y = u * (50f - (showT + showB) / 2f))
                if (ui.empty) {
                    EmptyDeckFront(place, metal = Color(0xFF5E5C60))
                } else {
                    FullCassette(ui, anim, place, vertical = true)
                }
                // Tinted glass over it, with a long reflection.
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0B0C10).copy(alpha = 0.5f))
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.5f, 0f); lineTo(size.width * 0.8f, 0f)
                            lineTo(size.width * 0.3f, size.height); lineTo(0f, size.height); close()
                        },
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.09f), Color.Transparent), 0f, size.width * 0.8f),
                    )
                }
                // A notification, when one arrives, rises into the bottom of the window.
                NoticeOverlay(NoticeStyle.BOTTOM, Modifier.fillMaxSize(), share = 0.30f)
            }
            Spacer(Modifier.height(6.dp))
            // The matching wedge below, hugging the bottom-right corner, with AUTO REVERSE lettered
            // along its slanted edge, right by the tape.
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                val slope = Math.toDegrees(kotlin.math.atan2(maxHeight.value.toDouble(), maxWidth.value.toDouble())).toFloat()
                Canvas(Modifier.fillMaxSize()) {
                    drawWedge(Path().apply { moveTo(size.width, size.height); lineTo(0f, size.height); lineTo(size.width, 0f); close() })
                }
                Text(
                    "AUTO REVERSE",
                    // A heavy Impact-style face (Anton), slanted, drawn as a white outline only.
                    style = TextStyle(
                        fontFamily = ImpactStyle, fontStyle = FontStyle.Italic, color = Color.White,
                        fontSize = 22.sp, letterSpacing = 1.5.sp,
                        drawStyle = Stroke(width = 2.6f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
                    ),
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.offset(x = (-36).dp, y = 0.dp).rotate(-slope),
                )
            }
        }
    }
}

/** One of the white printed wedges on the glass: off-white with a thin light-blue edge. */
private fun DrawScope.drawWedge(path: Path) {
    drawPath(path, Brush.linearGradient(listOf(Color(0xFFFDFDFC), Color(0xFFDDE3E8)), Offset.Zero, Offset(size.width, size.height)))
    drawPath(path, Color(0xFF6FB2DC), style = Stroke(2.2f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
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
            val running = ui.transport == Transport.PLAYING || ui.transport == Transport.REWINDING || ui.transport == Transport.FAST_FORWARDING
            BattLed(running, on = Color(0xFFFF4A3A), off = Color(0xFF4A1512))
            Spacer(Modifier.height(2.dp))
            Text("BATT", style = caption.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SilverKey(KeySymbol.PREV, "AMS", false, deck.previous, 22.dp, Modifier.weight(1f))
                SilverKey(KeySymbol.NEXT, "AMS", false, deck.next, 22.dp, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            val t = ui.transport
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SilverKey(KeySymbol.PLAY, "PLAY", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, keyH, Modifier.fillMaxWidth(), green = true)
                SilverKey(KeySymbol.FF, "FF", t == Transport.FAST_FORWARDING, deck.fastForward, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.REW, "REW", t == Transport.REWINDING, deck.rewind, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.PAUSE, "PAUSE", t == Transport.PAUSED, deck.pause, keyH, Modifier.fillMaxWidth())
                SilverKey(KeySymbol.STOP, "STOP·EJECT", false, deck.stop, keyH, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            Text("VOLUME", style = caption)
            // A round dial lying flat: turn it with a finger.
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                VolumeDial(ui.volume, deck.volume, Modifier.fillMaxWidth().aspectRatio(1f), marker = Color.White)
            }
            Spacer(Modifier.height(6.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp))
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
            pressed -> 1.14f
            latched -> 0.9f
            else -> 0f
        },
        spring(dampingRatio = 0.82f, stiffness = 1100f), label = "silverKey",
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
        ) { keyWell(depth, CornerRadius(size.height * 0.14f)) { drawSilverKey(symbol, depth, green) } }
    }
}

private fun DrawScope.drawSilverKey(symbol: KeySymbol, depth: Float, green: Boolean) {
    val w = size.width
    val h = size.height
    val box = keyBox(depth)
    val top = box.top
    val faceH = box.faceH
    val r = CornerRadius(h * 0.14f)
    // The well in the body, and the cap's front edge while it stands proud.
    drawKeyLip(box, r, if (green) listOf(Color(0xFF1C7A31), Color(0xFF0F4A1C)) else listOf(Color(0xFF8F949B), Color(0xFF4A4E54)))
    val shade = 1f - 0.2f * depth
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
