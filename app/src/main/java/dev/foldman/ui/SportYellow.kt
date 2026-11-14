// Foldy Streaman addition (not in the original Foldman): the "Sport Yellow" theme for the front
// screen, after a late-80s sports player: bright yellow plastic, a thick grey window surround that
// runs off the edge of the case, a radio-dial strip across the top, grey pill keys standing on end
// down a slim side strip, and big blue lettering at the bottom.
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
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

/** Sport Yellow palette. */
object SportYellow {
    val Yellow = Color(0xFFFFCF0A)
    val YellowHi = Color(0xFFFFE14F)
    val YellowLo = Color(0xFFE5B300)
    val Blue = Color(0xFF1E5AA8)
    val Ink = Color(0xFF17150C)
    /** The grey of the window surround (the same family as the Black & Gold knobs). */
    val Grey = listOf(Color(0xFF606066), Color(0xFF3F3F44), Color(0xFF2A2A2E))
}

private val RoundedHeavy = FontFamily(androidx.compose.ui.text.font.Font(dev.foldman.R.font.rounded_heavy_italic))

/** Moves this up by [by] and gives the space back to whatever is below. */
private fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, c ->
    val p = measurable.measure(c)
    val d = by.roundToPx()
    layout(p.width, (p.height - d).coerceAtLeast(0)) { p.place(0, -d) }
}

/** Bright moulded plastic: a smooth yellow with a soft sheen from the top left. */
internal fun Modifier.sportYellow(): Modifier = drawWithCache {
    val body = Brush.verticalGradient(listOf(SportYellow.YellowHi, SportYellow.Yellow, SportYellow.YellowLo))
    val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset.Zero, Offset(size.width * 0.7f, size.height * 0.35f))
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
    }
}

/** The folded phone in Sport Yellow. */
@Composable
internal fun CoverSportYellow(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val ink = TextStyle(fontFamily = FontFamily.SansSerif, color = SportYellow.Ink, fontWeight = FontWeight.Bold)
    // Nunito Black Italic: a free heavy font with fully rounded ends (licence in assets/font-licenses).
    val round = TextStyle(fontFamily = RoundedHeavy, color = SportYellow.Blue)
    Row(modifier.sportYellow()) {
        Column(
            Modifier
                .weight(0.79f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                .padding(top = 18.dp, bottom = 14.dp),
        ) {
            // The radio-dial strip across the top: a long pill with a dark line through the middle.
            DialStrip(
                top = "FM 88 92 96 100 104 108 MHz",
                bottom = if (ui.frontShelf) "TAP HERE FOR YOUR TAPES" else changeTapesHint(ui),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .then(if (ui.frontShelf) Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onTapes) else Modifier),
            )
            // Dropped so the top of the surround lines up with the top of the PLAY key.
            Spacer(Modifier.height(33.dp))
            // The grey window surround runs off the left edge of the case; the maker's name sits
            // on the yellow beside it.
            Row(Modifier.fillMaxWidth().weight(1f)) {
                WindowSurround(ui, anim, Modifier.fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 6.dp, end = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(6.dp))
                    Text("FOLDY", style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = SportYellow.Ink, fontSize = 19.sp, letterSpacing = 0.5.sp), maxLines = 1, softWrap = false)
                    Spacer(Modifier.height(10.dp))
                    // A small printed emblem: a block of orange dots.
                    Canvas(Modifier.size(30.dp, 34.dp)) {
                        val step = size.width / 6f
                        for (row in 0 until 7) for (col in 0 until 6) {
                            if ((row == 0 && col > 2) || (row == 6 && col < 3) || (row < 2 && col == 5)) continue
                            drawCircle(Color(0xFFF58A1F), step * 0.36f, Offset(step * (col + 0.5f), step * (row + 0.5f)))
                        }
                    }
                    // What's playing, lettered up the side.
                    Box(Modifier.weight(1f).padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.BottomCenter) {
                        Text(
                            listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                                .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                                .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                            style = ink.copy(fontSize = 9.sp, letterSpacing = 1.5.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.quarterTurn(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Column(Modifier.padding(start = 14.dp, end = 10.dp)) {
                // STREAMAN in the rounded font sits right on top of SPORT, starting at its S.
                Text(
                    "STREAMAN",
                    style = round.copy(color = SportYellow.Ink, fontSize = 18.sp, letterSpacing = 1.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Text(
                    "SPORT",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif, color = SportYellow.Blue, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
                        fontSize = 64.sp, letterSpacing = (-1).sp,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    ),
                    maxLines = 1, softWrap = false,
                    // Pulled up under STREAMAN (the letters have empty space above them) without
                    // squeezing the line itself, so nothing is cut off.
                    modifier = Modifier.pullUp(15.dp),
                )
            }
            if (ui.status == SourceStatus.NeedsAccess) {
                Text(
                    "TAP: TURN ON NOTIFICATION ACCESS",
                    style = ink.copy(fontSize = 9.sp, color = Color(0xFFFFE07A)),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onFixAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        SportKeyStrip(ui, deck, Modifier.weight(0.21f).fillMaxHeight())
    }
}

/**
 * The long pill at the top, like a radio dial: the words of [top] spread above its dark line,
 * [bottom] below. When a notification arrives, all of that slides down out of the pill and the
 * notification slides in from the top; a few seconds later they swap back.
 */
@Composable
private fun DialStrip(top: String, bottom: String, modifier: Modifier) {
    val ink = TextStyle(fontFamily = FontFamily.SansSerif, color = SportYellow.Ink, fontWeight = FontWeight.Bold)
    val (t, notice) = noticeSlide()
    Box(
        modifier
            .drawBehind {
                val r = CornerRadius(size.height / 2f)
                // A raised pill moulded into the body: no black outline, just a darker-yellow edge that
                // catches the light along the top and falls into shade along the bottom.
                drawRoundRect(Color(0xFF8A6800).copy(alpha = 0.35f), topLeft = Offset(0f, 2.5.dp.toPx()), size = size, cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFEE96), Color(0xFFFFDD4A))), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFD9AA00), Color(0xFFB98C00))), cornerRadius = r, style = Stroke(2.dp.toPx()))
                val inner = 2.dp.toPx()
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), 0f, size.height * 0.5f),
                    topLeft = Offset(inner, inner), size = Size(size.width - inner * 2, size.height - inner * 2), cornerRadius = CornerRadius(size.height / 2f - inner), style = Stroke(1.dp.toPx()),
                )
            }
            .padding(2.dp)
            .clip(RoundedCornerShape(50)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = t * size.height; alpha = (1f - t).coerceIn(0f, 1f) }
                .drawBehind {
                    // The dark line through the middle, with rounded ends.
                    val inset = size.height * 0.55f
                    drawLine(SportYellow.Ink, Offset(inset, size.height / 2f), Offset(size.width - inset, size.height / 2f), 4.dp.toPx(), StrokeCap.Round)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The FM band lettered along the top of the line, like a radio dial.
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                top.split(' ').forEachIndexed { i, word ->
                    val edge = i == 0 || word == "MHz"
                    Text(word, style = ink.copy(fontSize = if (edge) 8.sp else 11.sp, fontWeight = if (edge) FontWeight.Bold else FontWeight.Black), maxLines = 1)
                }
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(bottom, style = ink.copy(fontSize = 7.sp, letterSpacing = 2.sp), maxLines = 1)
            }
        }
        if (t > 0.001f && notice != null) {
            Row(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = -(1f - t) * size.height; alpha = t.coerceIn(0f, 1f) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoticeIcon(notice, 26)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        (notice.app.uppercase() + "  ·  " + notice.title).trim(),
                        style = ink.copy(fontSize = 11.sp, fontWeight = FontWeight.Black), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (notice.text.isNotBlank()) Text(notice.text, style = ink.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * The window and its thick grey surround. The surround starts at the very edge of the case, has
 * rounded corners on its free side, and its bottom edge slopes up towards the right. The window
 * itself sits tight round the tape.
 */
@Composable
internal fun WindowSurround(ui: DeckUi, anim: DeckAnimations, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        // The surround is sized round this much of the upright tape (in the tape's 64 x 100 units)...
        val frameL = 11f
        val frameR = 58f
        val frameT = 10f
        val frameB = 95f
        // ...but the window in it is cut much tighter: the reels and the name strip, nothing more.
        val showL = 13f
        val showR = 53f
        val showT = 21f
        val showB = 83f
        val aspect = (frameR - frameL) / (frameB - frameT)
        val border = 17.dp
        val slope = 44.dp
        // As big as fits, leaving room on the yellow beside it for the maker's name.
        val frameW = minOf((maxHeight - border * 2 - slope) * aspect, maxWidth - border * 2 - 88.dp)
        val frameH = frameW / aspect
        val u = frameW / (frameR - frameL)
        val winW = u * (showR - showL)
        val winH = u * (showB - showT)
        val winX = border + u * (showL - frameL)
        val winY = border + u * (showT - frameT)
        Box(
            Modifier
                .size(frameW + border * 2, frameH + border * 2 + slope)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val s = slope.toPx()
                    // Big round corners on the free side, and the two at the edge of the case just eased.
                    val big = 34.dp.toPx()
                    val small = 7.dp.toPx()
                    val shape = roundedShape(
                        listOf(Offset(0f, 0f) to small, Offset(w, 0f) to big, Offset(w, h - s) to big, Offset(0f, h) to small),
                    )
                    // A thick slab of rubber standing proud of the case: its shadow on the yellow...
                    translate(3.dp.toPx(), 5.dp.toPx()) { drawPath(shape, Color.Black.copy(alpha = 0.30f)) }
                    translate(1.dp.toPx(), 2.dp.toPx()) { drawPath(shape, Color.Black.copy(alpha = 0.35f)) }
                    drawPath(shape, Brush.linearGradient(SportYellow.Grey, Offset.Zero, Offset(w, h)))
                    // ...a rounded-over edge, lit along the top and dark along the bottom and right...
                    clipPath(shape) {
                        val bevel = 7.dp.toPx()
                        drawPath(shape, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.55f)), Offset(0f, 0f), Offset(w, h)), style = Stroke(bevel * 2))
                        drawPath(shape, Color.Black.copy(alpha = 0.6f), style = Stroke(1.2.dp.toPx()))
                    }
                    // ...and the window cut down through it: sloped walls, dark where the light can't
                    // reach (top and left) and catching it on the far side.
                    val wall = 8.dp.toPx()
                    val wx = winX.toPx()
                    val wy = winY.toPx()
                    val ww = winW.toPx()
                    val wh = winH.toPx()
                    val wr = 16.dp.toPx()
                    drawRoundRect(
                        Brush.linearGradient(listOf(Color(0xFF111113), Color(0xFF2B2B2F), Color(0xFF7C7C83)), Offset(wx - wall, wy - wall), Offset(wx + ww + wall, wy + wh + wall)),
                        Offset(wx - wall, wy - wall), Size(ww + wall * 2, wh + wall * 2), CornerRadius(wr + wall),
                    )
                    drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(wx - wall, wy - wall), Size(ww + wall * 2, wh + wall * 2), CornerRadius(wr + wall), style = Stroke(1.dp.toPx()))
                },
        ) {
            // Locked: a white padlock on the rubber, centred above the window.
            LockBadgeHere(ui, Color.White, Modifier.offset(x = winX + winW / 2 - 12.dp, y = (winY - 8.dp) / 2 - 12.dp))
            Box(
                Modifier
                    .padding(start = winX, top = winY)
                    .size(winW, winH)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131316)),
                contentAlignment = Alignment.Center,
            ) {
                val place = Modifier
                    .requiredSize(u * 64f, u * 100f)
                    .offset(x = u * (32f - (showL + showR) / 2f), y = u * (50f - (showT + showB) / 2f))
                if (ui.empty) {
                    EmptyDeckInterior(place, metal = Color(0xFF77767A))
                } else {
                    FullCassette(ui, anim, place, vertical = true)
                }
                // Clear plastic over it: a light tint, a reflection, and a shadow round the rim.
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0B0C10).copy(alpha = 0.22f))
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.5f, 0f); lineTo(size.width * 0.8f, 0f)
                            lineTo(size.width * 0.3f, size.height); lineTo(0f, size.height); close()
                        },
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent), 0f, size.width * 0.8f),
                    )
                    drawRoundRect(Color.Black.copy(alpha = 0.7f), cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(3.dp.toPx()))
                }
            }
        }
    }
}

/** A closed outline through [corners], each rounded off by its own radius. */
private fun roundedShape(corners: List<Pair<Offset, Float>>): Path = Path().apply {
    val n = corners.size
    for (i in 0 until n) {
        val (v, r) = corners[i]
        val prev = corners[(i + n - 1) % n].first
        val next = corners[(i + 1) % n].first
        fun toward(o: Offset): Offset { val d = o - v; return v + d / d.getDistance() * r }
        val a = toward(prev)
        if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
        val b = toward(next)
        quadraticTo(v.x, v.y, b.x, b.y)
    }
    close()
}

/** The slim side strip: battery light, two round skip keys, five pill keys on end, dial and socket. */
@Composable
private fun SportKeyStrip(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val keyH = ((maxHeight - 330.dp) / 5 - 8.dp).coerceIn(38.dp, 60.dp)
        val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = SportYellow.Ink, fontSize = 7.sp, letterSpacing = 1.sp)
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.03f))))
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 8.dp, end = 7.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val t = ui.transport
            BattLed(t == Transport.PLAYING || t == Transport.REWINDING || t == Transport.FAST_FORWARDING, on = Color(0xFFFF3B2E), off = Color(0xFF6B4A12))
            Spacer(Modifier.height(2.dp))
            Text("BATT", style = caption.copy(fontSize = 12.sp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PillKey(KeySymbol.PREV, "", "Previous", false, deck.previous, 30.dp, 30.dp)
                PillKey(KeySymbol.NEXT, "", "Next", false, deck.next, 30.dp, 30.dp)
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PillKey(KeySymbol.PLAY, "PLAY", "Play", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, 34.dp, keyH)
                PillKey(KeySymbol.FF, "FF", "Fast forward", t == Transport.FAST_FORWARDING, deck.fastForward, 34.dp, keyH)
                PillKey(KeySymbol.REW, "REW", "Rewind", t == Transport.REWINDING, deck.rewind, 34.dp, keyH)
                PillKey(KeySymbol.PAUSE, "PAUSE", "Pause", t == Transport.PAUSED, deck.pause, 34.dp, keyH)
                PillKey(KeySymbol.STOP, "STOP", "Stop and eject", false, deck.stop, 34.dp, keyH)
            }
            Spacer(Modifier.height(10.dp))
            // A round dial lying flat, in yellow with black numbers: turn it with a finger. Its name
            // sits just above the pointer.
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VOLUME", style = caption)
                VolumeDial(ui.volume, deck.volume, Modifier.fillMaxWidth().aspectRatio(1f), marker = SportYellow.Ink, yellow = true)
            }
            Spacer(Modifier.height(4.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp), dark = true)
        }
        // The seam where the strip meets the body.
        Box(Modifier.fillMaxHeight().width(2.dp).background(Color.Black.copy(alpha = 0.35f)))
    }
}

/**
 * A grey pill-shaped key standing on end in a dark ring, with its name lettered up its side. It
 * sinks into the ring when pressed or latched, the same way the round Black & Gold knobs do. With
 * [width] equal to [height] it is a round key.
 */
@Composable
private fun PillKey(symbol: KeySymbol, label: String, description: String, latched: Boolean, onPress: () -> Unit, width: Dp, height: Dp) {
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
        spring(dampingRatio = 0.82f, stiffness = 1100f), label = "pillKey",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = SportYellow.Ink, fontSize = 8.sp, letterSpacing = 1.5.sp),
                maxLines = 1, softWrap = false,
                modifier = Modifier.quarterTurn(),
            )
            Spacer(Modifier.width(5.dp))
        }
        Canvas(
            Modifier
                .size(width, height)
                .semantics { role = Role.Button; contentDescription = description }
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
        ) { drawPillKey(symbol, depth) }
    }
}

private fun DrawScope.drawPillKey(symbol: KeySymbol, depth: Float) {
    val w = size.width
    val h = size.height
    val up = (1f - depth).coerceIn(0f, 1f)
    // The shadow a raised key throws across the case, gone once it is pushed in.
    if (up > 0f) drawRoundRect(Color.Black.copy(alpha = 0.28f * up), Offset(w * 0.10f, w * 0.16f), Size(w, h), CornerRadius(w / 2f))
    // The dark ring it sits in.
    drawRoundRect(Color(0xFF14110A), cornerRadius = CornerRadius(w / 2f))
    drawRoundRect(Color.Black.copy(alpha = 0.5f), cornerRadius = CornerRadius(w / 2f), style = Stroke(1.5f))
    // The key is a tall plug: raised, you see its top face sitting high with its side wall
    // showing below it; pressed, the face drops and the wall disappears into the ring.
    val inset = w * 0.08f
    val cw = w - inset * 2
    val wall = w * 0.24f
    val ch = h - inset * 2 - wall
    val r = CornerRadius(cw / 2f)
    val top = inset + wall * (1f - up)
    // Side wall: from the face's bottom half down to the base, darker toward the bottom.
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF4A4A4F), Color(0xFF2A2A2D), Color(0xFF111113)), top + ch * 0.5f, inset + ch + wall),
        Offset(inset, top + wall * up), Size(cw, ch), r,
    )
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF3A3A3E), Color(0xFF55555A), Color(0xFF1C1C1E)), inset, inset + cw), Offset(inset, top + ch - cw / 2f), Size(cw, wall * up))
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF3A3A3E), Color(0xFF55555A), Color(0xFF1C1C1E)), inset, inset + cw),
        Offset(inset, top + wall * up), Size(cw, ch), r,
    )
    // The top face, domed: lit from the top left, with a bright rim on that side.
    val faceTop = Offset(inset, top)
    val faceSize = Size(cw, ch)
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFF8A8A91), Color(0xFF55555A), Color(0xFF2E2E32)), faceTop, faceTop + Offset(cw, ch)),
        faceTop, faceSize, r,
    )
    drawRoundRect(
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.35f)), faceTop, faceTop + Offset(cw, ch)),
        faceTop + Offset(1f, 1f), Size(cw - 2f, ch - 2f), CornerRadius(cw / 2f - 1f), style = Stroke(2.2f),
    )
    // The symbol, moulded into the cap. Forward points up the key and back points down it.
    val c = Offset(w / 2f, top + ch / 2f)
    val turned = symbol == KeySymbol.PLAY || symbol == KeySymbol.FF || symbol == KeySymbol.REW
    rotate(if (turned) -90f else 0f, pivot = c) { drawSymbol(symbol, c, cw * 0.2f, Color(0xFFD0D0D5)) }
    // Darker when pressed in, with the ring's shadow falling across its top.
    if (depth > 0f) {
        drawRoundRect(Color.Black.copy(alpha = (0.22f * depth).coerceIn(0f, 1f)), faceTop, faceSize, r)
        drawRoundRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = (0.5f * depth).coerceIn(0f, 1f)), Color.Transparent), top, top + ch * 0.3f), faceTop, faceSize, r)
    }
}
