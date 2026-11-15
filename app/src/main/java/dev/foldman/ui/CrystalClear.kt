// Foldy Streaman addition (not in the original Foldman): the "Crystal Clear" theme for the front
// screen, after a see-through player from the 90s: a clear plastic cover showing the whole tape and
// the silver mechanism round it, big frosted lettering up the side, a yellow warning sticker, and a
// brushed-silver side strip with silver keys set in white.
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asComposePath
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

/** How far below centre the tape window sits on the cover. */
private val WindowDrop = 16.dp

/** Crystal Clear palette. */
object CrystalClear {
    val Metal = Color(0xFF8E9498)
    val MetalHi = Color(0xFFD8DCDF)
    val MetalLo = Color(0xFF4E5458)
    val Warning = Color(0xFFF2C83A)
    val Ink = Color(0xFF16171A)
}


/** Brushed silver: fine lengthwise brushing over a soft metal gradient. */
internal fun Modifier.brushedSilver(): Modifier = drawWithCache {
    val rnd = Random(21)
    val lines = List((size.width / 1.6f).toInt().coerceAtLeast(1)) { rnd.nextFloat() * size.width to rnd.nextFloat() }
    val body = Brush.horizontalGradient(listOf(Color(0xFFB9BEC2), Color(0xFFE7EAEC), Color(0xFFC3C8CB), Color(0xFF9FA5A9)))
    onDrawBehind {
        drawRect(body)
        lines.forEach { (x, a) -> drawLine(if (a > 0.5f) Color.White.copy(alpha = 0.22f * a) else Color.Black.copy(alpha = 0.08f * a), Offset(x, 0f), Offset(x, size.height), 0.8f) }
    }
}

/** The cover: matte silver plastic, a soft sheen from the top left and a fine grain. */
private fun Modifier.matteSilver(): Modifier = drawWithCache {
    val rnd = Random(33)
    val grain = List((size.width * size.height / 220f).toInt().coerceAtMost(14000)) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) to rnd.nextFloat() }
    val body = Brush.linearGradient(listOf(Color(0xFFD3D7DA), Color(0xFFBEC3C7), Color(0xFFA9AFB3)), Offset.Zero, Offset(size.width * 0.6f, size.height))
    val sheen = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.08f), size.width * 0.9f)
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
        grain.forEach { (p, a) -> drawCircle(if (a > 0.5f) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f), 0.7f, p) }
    }
}

/** The silver mechanism seen through the cover: a perforated metal plate with a few screws. */
private fun Modifier.mechanism(): Modifier = drawWithCache {
    val step = 5.dp.toPx()
    val base = Brush.linearGradient(listOf(Color(0xFF6E7478), Color(0xFF4C5256), Color(0xFF5F6569)), Offset.Zero, Offset(size.width, size.height))
    onDrawBehind {
        drawRect(base)
        var y = step / 2f
        var row = 0
        while (y < size.height) {
            var x = step / 2f + if (row % 2 == 0) 0f else step / 2f
            while (x < size.width) {
                drawCircle(Color.Black.copy(alpha = 0.35f), step * 0.18f, Offset(x, y))
                drawCircle(Color.White.copy(alpha = 0.10f), step * 0.18f, Offset(x, y + 0.8f))
                x += step
            }
            y += step * 0.87f
            row++
        }
    }
}

/** The folded phone in Crystal Clear. */
@Composable
internal fun CoverCrystalClear(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    val ink = TextStyle(fontFamily = FontFamily.SansSerif, color = CrystalClear.Ink, fontWeight = FontWeight.Bold)
    Row(modifier.background(Color(0xFF2A2D30))) {
        Box(Modifier.weight(0.79f).fillMaxHeight().matteSilver()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                    .padding(start = 12.dp, end = 10.dp, top = 18.dp, bottom = 14.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp)) {
                    if (ui.frontShelf) {
                        // Centred between FOLDY (about 26dp in) and the cover's right-hand edge.
                        val centre = (26.dp + maxWidth + 10.dp) / 2
                        TapesButton(Color(0xFFD9DDE0), CrystalClear.Ink, onTapes, Modifier.align(Alignment.CenterStart).offset(x = centre - 33.dp))
                    } else {
                        Text(changeTapesHint(ui), style = ink.copy(fontSize = 7.sp, letterSpacing = 2.sp, color = CrystalClear.Ink.copy(alpha = 0.7f)), modifier = Modifier.align(Alignment.CenterStart))
                    }
                    // A small serial-number tag, like the one on the real thing.
                    Column(
                        Modifier.align(Alignment.CenterEnd).background(Color(0xFFF4F4EF), RoundedCornerShape(2.dp)).border(0.8.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text("SERIAL", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 6.sp, color = CrystalClear.Ink))
                        Text("0026", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CrystalClear.Ink))
                    }
                }
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    // The window: just bigger than the whole tape, stood on end.
                    val rim = 8.dp
                    // As wide as the cover allows (it runs under STREAMAN), and nearly the full height.
                    val tapeW = minOf(maxWidth - rim * 2 - 6.dp, (maxHeight * 0.86f - rim * 2) * 64f / 100f)
                    val tapeH = tapeW * 100f / 64f
                    val winShape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(y = WindowDrop)
                            .requiredSize(tapeW + rim * 2, tapeH + rim * 2)
                            .drawBehind {
                                // Set into the cover: a shadow under its top edge, a light edge below.
                                val r = CornerRadius(14.dp.toPx())
                                drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(0f, 2f), size, r)
                                drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(0f, -2f), size, r)
                            }
                            .clip(winShape)
                            .mechanism(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.requiredSize(tapeW, tapeH)) {
                            if (ui.empty) EmptyDeckFront(Modifier.fillMaxSize(), metal = Color(0xFFA7ADB1))
                            else FullCassette(ui, anim, Modifier.fillMaxSize(), vertical = true)
                        }
                        // The glass: a faint tint, a long glare across it, and a brighter streak at the top.
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color(0xFF0E1216).copy(alpha = 0.16f))
                            drawPath(
                                Path().apply {
                                    moveTo(size.width * 0.5f, 0f); lineTo(size.width * 0.86f, 0f)
                                    lineTo(size.width * 0.26f, size.height); lineTo(0f, size.height); lineTo(0f, size.height * 0.82f); close()
                                },
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent), 0f, size.width * 0.86f),
                            )
                            drawPath(
                                Path().apply {
                                    moveTo(size.width * 0.9f, 0f); lineTo(size.width * 0.96f, 0f)
                                    lineTo(size.width * 0.5f, size.height); lineTo(size.width * 0.44f, size.height); close()
                                },
                                Color.White.copy(alpha = 0.07f),
                            )
                            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), 0f, size.height * 0.18f))
                            drawRoundRect(Color.Black.copy(alpha = 0.55f), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(3.dp.toPx()))
                            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), CornerRadius(11.dp.toPx()), style = Stroke(1.dp.toPx()))
                        }
                        NoticeOverlay(NoticeStyle.TOP, Modifier.fillMaxSize(), share = 0.3f)
                    }
                    // FOLDY up the left, where the maker's name goes.
                    // In the same frosted lettering as STREAMAN.
                    // Centred up and down in the gap between the top line's lettering (about 20dp above this box)
                    // and the window's top edge.
                    val winTop = (maxHeight - tapeH - rim * 2) / 2 + WindowDrop
                    FillLettering("FOLDY", Modifier.align(Alignment.TopStart).offset(x = (-2).dp, y = (winTop - 20.dp) / 2 - 41.dp).size(28.dp, 82.dp))
                    // STREAMAN, big and frosted, printed up the right-hand side and over the window's edge.
                    FillLettering(
                        "STREAMAN",
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(maxWidth * 0.32f),
                    )
                    // Locked: a white padlock on the glass, over the top of the tape.
                    LockBadgeHere(ui, Color.White, Modifier.align(Alignment.TopCenter).offset(x = (-24).dp, y = 14.dp))
                    // The warning sticker, stuck on the cover at the bottom left.
                    WarningSticker(Modifier.align(Alignment.BottomStart).padding(bottom = 6.dp).width(maxWidth * 0.66f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ")
                        .ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }
                        .let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                    style = ink.copy(fontSize = 8.sp, letterSpacing = 1.5.sp, color = CrystalClear.Ink.copy(alpha = 0.8f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (ui.status == SourceStatus.NeedsAccess) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "TAP: TURN ON NOTIFICATION ACCESS",
                        style = ink.copy(fontSize = 9.sp, color = CrystalClear.Warning),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable(onClick = onFixAccess)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
        ClearKeyStrip(ui, deck, Modifier.weight(0.21f).fillMaxHeight())
    }
}

/**
 * Lettering that fills its box, reading up the side, in Lilita One (a free, heavy rounded font; licence in
 * assets/font-licenses): the letters' own outlines are scaled to run
 * the box's full height and stretched (a little at most) to fill its width. Frosted white, with a
 * soft shadow so it shows on the silver.
 */
@Composable
private fun FillLettering(text: String, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shape = remember(text) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, dev.foldman.R.font.lilita_one)
            textSize = 100f
            letterSpacing = 0.02f
        }
        val path = android.graphics.Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)
        // The font builds letters from overlapping pieces: merge them so the edge only runs round the outside.
        android.graphics.Path().let { merged -> if (merged.op(path, android.graphics.Path.Op.UNION)) path.set(merged) }
        val bounds = android.graphics.RectF()
        @Suppress("DEPRECATION") path.computeBounds(bounds, true)
        path.asComposePath() to bounds
    }
    Canvas(modifier) {
        val (path, b) = shape
        // Turned to read upwards: the word's length runs up the box, its height across it.
        val along = size.height / b.width()
        val across = minOf(size.width / b.height(), along * 1.6f)
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            rotate(-90f, pivot = Offset.Zero)
            scale(along, across, pivot = Offset.Zero)
            translate(-b.centerX(), -b.centerY())
        }) {
            drawPath(path, Color.Black.copy(alpha = 0.22f))
        }
        withTransform({
            translate(size.width / 2f - 2f, size.height / 2f)
            rotate(-90f, pivot = Offset.Zero)
            scale(along, across, pivot = Offset.Zero)
            translate(-b.centerX(), -b.centerY())
        }) {
            drawPath(path, Color.White.copy(alpha = 0.72f))
            // A thin edge, so the letters still read where they cross the silver.
            drawPath(path, Color(0xFF5A6066).copy(alpha = 0.55f), style = Stroke(1.6f / along))
        }
    }
}

/** The yellow sticker on the cover. */
@Composable
private fun WarningSticker(modifier: Modifier) {
    val ink = CrystalClear.Ink
    val small = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = ink, fontSize = 6.5.sp, lineHeight = 8.sp, letterSpacing = 0.3.sp)
    Column(
        modifier
            .background(CrystalClear.Warning, RoundedCornerShape(2.dp))
            .border(1.5.dp, ink, RoundedCornerShape(2.dp))
            .padding(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "⚠ WARNING",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = CrystalClear.Warning, fontSize = 9.sp, letterSpacing = 1.sp),
                modifier = Modifier.background(ink, RoundedCornerShape(1.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "HIGHLY CONTAGIOUS",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = ink, fontSize = 8.sp, letterSpacing = 0.5.sp),
                modifier = Modifier.border(1.dp, ink).padding(horizontal = 4.dp, vertical = 1.dp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "CONTENTS ARE HIGHLY CONTAGIOUS. MAY CAUSE HEAD NODDING, TOE TAPPING AND SUDDEN DANCING IN PUBLIC. NO KNOWN CURE. KEEP VOLUME UP.",
            style = small,
        )
        Text("FS-26", style = small.copy(fontSize = 6.sp), modifier = Modifier.align(Alignment.End))
    }
}

/** The brushed-silver side strip: round previous and next, then the keys (PLAY, PAUSE and STOP tall pills, REW and FF round). */
@Composable
private fun ClearKeyStrip(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier.brushedSilver()) {
        val keyH = ((maxHeight - 450.dp) / 3 - 6.dp).coerceIn(40.dp, 64.dp)
        val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = CrystalClear.Ink, fontSize = 7.sp, letterSpacing = 1.sp)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 8.dp, end = 7.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val t = ui.transport
            BattLed(t == Transport.PLAYING || t == Transport.REWINDING || t == Transport.FAST_FORWARDING, on = Color(0xFFFF3B2E), off = Color(0xFF6A6F73))
            Spacer(Modifier.height(2.dp))
            Text("BATT", style = caption.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ClearKey(KeySymbol.PREV, "", "Previous", false, deck.previous, 30.dp, 30.dp)
                ClearKey(KeySymbol.NEXT, "", "Next", false, deck.next, 30.dp, 30.dp)
            }
            Spacer(Modifier.height(12.dp))
            // PLAY, PAUSE and STOP are tall pills; REW and FF are round, in between.
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.End) {
                ClearKey(KeySymbol.PLAY, "PLAY", "Play", t == Transport.PLAYING || t == Transport.PAUSED, deck.play, 34.dp, keyH)
                ClearKey(KeySymbol.REW, "REW", "Rewind", t == Transport.REWINDING, deck.rewind, 34.dp, 34.dp)
                ClearKey(KeySymbol.FF, "FF", "Fast forward", t == Transport.FAST_FORWARDING, deck.fastForward, 34.dp, 34.dp)
                ClearKey(KeySymbol.PAUSE, "PAUSE", "Pause", t == Transport.PAUSED, deck.pause, 34.dp, keyH)
                ClearKey(KeySymbol.STOP, "STOP", "Stop and eject", false, deck.stop, 34.dp, keyH)
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VOLUME", style = caption)
                VolumeDial(ui.volume, deck.volume, Modifier.fillMaxWidth().aspectRatio(1f), marker = CrystalClear.Ink)
            }
            Spacer(Modifier.height(4.dp))
            Text("PHONES", style = caption.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp))
        }
        // The seam where the strip meets the clear cover.
        Box(Modifier.fillMaxHeight().width(2.dp).background(Color.Black.copy(alpha = 0.35f)))
    }
}

/**
 * A metallic silver key in a white surround, sinking when pressed or latched. A pill standing on
 * end, or round when [width] equals [height]. [label] (if any) is lettered up its side.
 */
@Composable
private fun ClearKey(symbol: KeySymbol, label: String, description: String, latched: Boolean, onPress: () -> Unit, width: Dp, height: Dp) {
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
        spring(dampingRatio = 0.82f, stiffness = 1100f), label = "clearKey",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, color = CrystalClear.Ink, fontSize = 8.sp, letterSpacing = 1.5.sp),
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
        ) { drawClearKey(symbol, depth) }
    }
}

private fun DrawScope.drawClearKey(symbol: KeySymbol, depth: Float) {
    val w = size.width
    val h = size.height
    val up = (1f - depth).coerceIn(0f, 1f)
    // The white surround, with a soft shadow inside its top edge.
    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(0f, 1.5f), Size(w, h), CornerRadius(w / 2f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE4E6E8))), cornerRadius = CornerRadius(w / 2f))
    // The well the key sits in.
    val ring = w * 0.12f
    drawRoundRect(Color(0xFF55595D), Offset(ring, ring), Size(w - ring * 2, h - ring * 2), CornerRadius(w / 2f - ring))
    // The silver cap: shadow, side, then a brushed face lit from the top left.
    val inset = ring + w * (0.03f + 0.05f * depth)
    val cw = w - inset * 2
    val ch = h - inset * 2
    val r = CornerRadius(cw / 2f)
    val drop = w * 0.07f * up
    drawRoundRect(Color.Black.copy(alpha = 0.3f * up), Offset(inset + drop * 0.5f, inset + drop * 1.3f), Size(cw, ch), r)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9EA4A8), Color(0xFF5F6569)), inset, inset + ch), Offset(inset, inset + drop * 0.6f), Size(cw, ch), r)
    val faceTop = Offset(inset, inset - drop * 0.4f)
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFFF4F6F7), Color(0xFFC1C6CA), Color(0xFF8C9296)), faceTop, faceTop + Offset(cw, ch)),
        faceTop, Size(cw, ch), r,
    )
    drawRoundRect(Color.White.copy(alpha = 0.7f), faceTop + Offset(1f, 1f), Size(cw - 2f, ch - 2f), CornerRadius(cw / 2f - 1f), style = Stroke(1.2f))
    val c = Offset(w / 2f, faceTop.y + ch / 2f)
    val turned = symbol == KeySymbol.PLAY
    rotate(if (turned) -90f else 0f, pivot = c) { drawSymbol(symbol, c, cw * 0.22f, Color(0xFF2B2E31)) }
    if (depth > 0f) drawRoundRect(Color.Black.copy(alpha = (0.18f * depth).coerceIn(0f, 1f)), faceTop, Size(cw, ch), r)
}
