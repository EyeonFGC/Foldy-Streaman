// Foldy Streaman addition (not in the original Foldman): the J-card, the paper insert of a tape.
// Tap a tape in the rack to read it and change its label, picture, font, colours and playlist; a
// small one sits under the deck showing what's playing.
package dev.foldman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.CustomTape
import dev.foldman.deck.DeckUi
import dev.foldman.deck.TapeColors
import dev.foldman.deck.TapeFont
import dev.foldman.deck.TapePhotos
import dev.foldman.deck.Transport
import dev.foldman.deck.PlaylistRef
import dev.foldman.deck.parsePlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Settings > Dark J-cards: every colour on the card follows this. */
private var jDark by mutableStateOf(false)
private val JPaper get() = if (jDark) Color(0xFF2A2622) else Color(0xFFF1E9D8)
private val JInk get() = if (jDark) Color(0xFFECE4D4) else Color(0xFF26211D)
private val JRed get() = if (jDark) Color(0xFFFF8E7E) else Color(0xFFB0372B)
private val Typed get() = TextStyle(fontFamily = FontFamily.Monospace, color = JInk, fontSize = 12.sp)

/** The ink the card's buttons are drawn in: the theme's colour, lighter on dark paper. */
private fun stampInk(theme: dev.foldman.deck.DeckTheme, dark: Boolean): Color = when (theme) {
    dev.foldman.deck.DeckTheme.ORIGINAL -> if (dark) Color(0xFF8CC4FF) else Color(0xFF1F5FA8)
    dev.foldman.deck.DeckTheme.BLACK_GOLD -> if (dark) Color(0xFFDAD5C9) else Color(0xFF3A3A3E)
    dev.foldman.deck.DeckTheme.CHERRY_RED -> if (dark) Color(0xFFFF8597) else Color(0xFFC22A44)
    dev.foldman.deck.DeckTheme.RED_CHROME -> if (dark) Color(0xFFFF7A82) else Color(0xFFB3202B)
    dev.foldman.deck.DeckTheme.SILVER_BLUE -> if (dark) Color(0xFF9DB4FF) else Color(0xFF2347C6)
}

/** Paper: a flat colour with speckles and fibres drawn over it, so it reads as a real card. */
private fun Modifier.paper(dark: Boolean): Modifier = drawWithCache {
    val rnd = kotlin.random.Random(21)
    val base = if (dark) Color(0xFF2A2622) else Color(0xFFF1E9D8)
    val speck = if (dark) Color.White else Color(0xFF5A4630)
    val dots = List(2600) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) to rnd.nextFloat() }
    val fibres = List(170) {
        val a = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        val ang = rnd.nextFloat() * 6.283f
        val len = 8f + rnd.nextFloat() * 26f
        Triple(a, a + Offset(kotlin.math.cos(ang) * len, kotlin.math.sin(ang) * len), rnd.nextFloat())
    }
    val wash = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.04f else 0.25f), Color.Transparent, Color.Black.copy(alpha = if (dark) 0.25f else 0.07f)),
        Offset.Zero, Offset(size.width, size.height),
    )
    onDrawBehind {
        drawRect(base)
        dots.forEach { (o, a) -> drawCircle(speck.copy(alpha = (if (dark) 0.05f else 0.09f) * a), 0.9f + a, o) }
        fibres.forEach { (a, b, t) -> drawLine((if (t > 0.5f) speck else Color.White).copy(alpha = (if (dark) 0.06f else 0.12f) * t), a, b, 0.9f) }
        drawRect(wash)
    }
}

/** The card's outline: square-cornered, with a small ragged tear across the top-right corner. */
@Composable
private fun tornCard(): androidx.compose.ui.graphics.Shape {
    val t = with(androidx.compose.ui.platform.LocalDensity.current) { 15.dp.toPx() }
    return remember(t) {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width - t, 0f)
            // The tear: a jagged diagonal from the top edge down to the right edge.
            val jag = floatArrayOf(0.10f, 0.06f, 0.30f, 0.16f, 0.36f, 0.40f, 0.60f, 0.46f, 0.66f, 0.72f, 0.90f, 0.80f)
            for (i in jag.indices step 2) lineTo(size.width - t + t * jag[i], t * jag[i + 1])
            lineTo(size.width, t)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

/** How wide the picture strip is for its height. */
const val PHOTO_STRIP_ASPECT = 2.4f

/** The picture, loaded off the main thread. Null while loading or when there isn't one. */
@Composable
internal fun rememberPhoto(path: String?): ImageBitmap? {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) { TapePhotos.load(path)?.asImageBitmap() }
    }
    return bmp
}

/**
 * The tape's picture in its frame, placed by [x], [y] (-1..1) and [zoom] (1 = just fills). With
 * [onMove], one finger drags it and two fingers pinch to zoom.
 */
@Composable
fun TapePhoto(
    path: String?,
    x: Float,
    y: Float,
    zoom: Float,
    modifier: Modifier = Modifier,
    onMove: ((x: Float, y: Float, zoom: Float) -> Unit)? = null,
    empty: @Composable () -> Unit = {},
) {
    val photo = rememberPhoto(path)
    var frame by remember { mutableStateOf(IntSize.Zero) }
    // Where the picture is during a gesture. Kept here (not restarting the gesture each time the
    // picture moves) so one swipe or pinch follows the fingers all the way.
    val pos = remember { floatArrayOf(x, y, zoom) }
    androidx.compose.runtime.SideEffect { pos[0] = x; pos[1] = y; pos[2] = zoom }
    val move by androidx.compose.runtime.rememberUpdatedState(onMove)
    val gestures = if (onMove == null || photo == null) Modifier else Modifier.pointerInput(photo) {
        detectTransformGestures { _, pan, z, _ ->
            val pz = (pos[2] * z).coerceIn(1f, 4f)
            val (ox, oy) = overflow(photo, frame, pz)
            val px = if (ox > 0f) (pos[0] + pan.x / ox).coerceIn(-1f, 1f) else 0f
            val py = if (oy > 0f) (pos[1] + pan.y / oy).coerceIn(-1f, 1f) else 0f
            pos[0] = px; pos[1] = py; pos[2] = pz
            move?.invoke(px, py, pz)
        }
    }
    Box(modifier.clip(RoundedCornerShape(2.dp)).background(if (jDark) Color(0xFF3B3630) else Color(0xFFD9CFBC)).then(gestures), contentAlignment = Alignment.Center) {
        if (photo == null) {
            empty()
        } else {
            Canvas(Modifier.fillMaxSize()) {
                frame = IntSize(size.width.toInt(), size.height.toInt())
                val s = maxOf(size.width / photo.width, size.height / photo.height) * zoom
                val dw = photo.width * s
                val dh = photo.height * s
                val ox = (dw - size.width) / 2f
                val oy = (dh - size.height) / 2f
                drawImage(
                    photo,
                    dstOffset = IntOffset((-ox + x * ox).roundToInt(), (-oy + y * oy).roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                )
            }
        }
    }
}

private fun overflow(photo: ImageBitmap, frame: IntSize, zoom: Float): Pair<Float, Float> {
    if (frame.width == 0 || frame.height == 0) return 0f to 0f
    val s = maxOf(frame.width.toFloat() / photo.width, frame.height.toFloat() / photo.height) * zoom
    return ((photo.width * s - frame.width) / 2f) to ((photo.height * s - frame.height) / 2f)
}

/** What the J-card can change. */
class JCardActions(
    val save: (CustomTape) -> Unit,
    val delete: (String) -> Unit,
    val pickPhoto: (String) -> Unit,
    val removePhoto: (String) -> Unit,
)

private enum class JEdit { LABEL, PHOTO_MENU, FONT, COLOURS, PLAYLIST, DELETE }

/**
 * The J-card for one of your tapes, slid out over the rack: title in its font, the picture under
 * it, what's on the tape, and the buttons to change it all.
 */
@Composable
fun JCardSheet(
    tape: CustomTape,
    ui: DeckUi,
    actions: JCardActions,
    onLoad: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var edit by remember { mutableStateOf<JEdit?>(null) }
    // Moving the picture: a working copy, saved on DONE.
    var moving by remember { mutableStateOf(false) }
    var mx by remember { mutableFloatStateOf(tape.photoX) }
    var my by remember { mutableFloatStateOf(tape.photoY) }
    var mz by remember { mutableFloatStateOf(tape.photoZoom) }
    val inDeck = ui.tape?.id == tape.id
    val font = tape.font
    if (jDark != ui.jcardDark) jDark = ui.jcardDark
    val card = tornCard()

    Column(
        modifier
            .shadow(18.dp, card)
            .clip(card)
            .paper(ui.jcardDark)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                tape.title.ifBlank { "Tap to name this tape" },
                style = TextStyle(fontFamily = font.family(), fontWeight = font.weight(), fontSize = (30 * font.sizeScale()).sp, color = if (tape.title.isBlank()) JInk.copy(alpha = 0.45f) else JInk, lineHeight = (34 * font.sizeScale()).sp),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable { edit = JEdit.LABEL },
            )
            Text("✕", style = Typed.copy(fontSize = 18.sp), modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClose).padding(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        // The picture strip under the title.
        TapePhoto(
            tape.photoPath,
            if (moving) mx else tape.photoX, if (moving) my else tape.photoY, if (moving) mz else tape.photoZoom,
            Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_STRIP_ASPECT)
                .then(if (moving) Modifier.border(2.dp, JRed, RoundedCornerShape(2.dp)) else Modifier)
                .clickable(enabled = !moving) { if (tape.photoPath == null) actions.pickPhoto(tape.id) else edit = JEdit.PHOTO_MENU },
            onMove = if (moving) { x, y, z -> mx = x; my = y; mz = z } else null,
            empty = { Text("TAP TO ADD A PICTURE", style = Typed.copy(color = JInk.copy(alpha = 0.55f), fontSize = 11.sp, letterSpacing = 2.sp)) },
        )
        if (moving) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Drag to move · pinch to zoom", style = Typed.copy(fontSize = 11.sp, color = JInk.copy(alpha = 0.7f)), modifier = Modifier.weight(1f))
                Text("CANCEL", style = Typed.copy(color = JInk.copy(alpha = 0.7f), fontWeight = FontWeight.Bold), modifier = Modifier.clickable { moving = false }.padding(8.dp))
                Text(
                    "DONE",
                    style = Typed.copy(color = JRed, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable {
                        actions.save(tape.copy(photoX = mx, photoY = my, photoZoom = mz))
                        moving = false
                    }.padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("SIDE A", style = Typed.copy(color = JRed, fontWeight = FontWeight.Bold, fontSize = 14.sp))
            if (tape.playlistId.isBlank()) {
                // A blank tape: paste its playlist link right here on the card.
                var link by remember { mutableStateOf("") }
                Text("Paste a YouTube Music or Spotify playlist link:", style = Typed.copy(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = link,
                    onValueChange = { v ->
                        link = v
                        parsePlaylist(v)?.let { ref -> actions.save(tape.copy(playlistId = ref.id, service = ref.service)) }
                    },
                    singleLine = true,
                    textStyle = Typed.copy(fontSize = 13.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(JRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, JRed, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                )
                Text(
                    if (link.isNotBlank()) "That doesn't look like a playlist link yet." else "In the music app: open the playlist → Share → Copy link.",
                    style = Typed.copy(fontSize = 10.sp, color = JInk.copy(alpha = 0.6f)),
                )
            } else {
                Text("${tape.service.label} playlist", style = Typed.copy(fontSize = 13.sp))
            }
            Text(
                when {
                    inDeck && ui.transport == Transport.PLAYING -> "In the deck · playing"
                    inDeck -> "In the deck"
                    else -> "On the rack"
                },
                style = Typed.copy(fontSize = 12.sp, color = JRed),
            )
            if (inDeck && ui.title.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("♪ ${listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString(" — ")}", style = Typed.copy(fontSize = 12.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            // The buttons, like the controls printed on the back of the card.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CardButton("✎", "LABEL", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { edit = JEdit.LABEL }
                CardButton("▣", "PHOTO", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { if (tape.photoPath == null) actions.pickPhoto(tape.id) else edit = JEdit.PHOTO_MENU }
                CardButton("Aa", "FONT", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { edit = JEdit.FONT }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CardButton("◐", "COLOURS", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { edit = JEdit.COLOURS }
                CardButton("♫", "PLAYLIST", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { edit = JEdit.PLAYLIST }
                CardButton("✕", "DELETE", Modifier.weight(1f), JButtonStyle.STAMP, ui.theme) { edit = JEdit.DELETE }
            }
            // Show the picture on the cassette itself, where the label colour normally goes.
            Spacer(Modifier.height(14.dp))
            val hasPhoto = tape.photoPath != null
            val on = tape.photoOnLabel && hasPhoto
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = hasPhoto) { actions.save(tape.copy(photoOnLabel = !tape.photoOnLabel)) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("PICTURE ON THE CASSETTE", style = Typed.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = if (hasPhoto) JInk else JInk.copy(alpha = 0.4f)))
                    Text(
                        if (hasPhoto) "Shows the picture on the tape's label, under the stripes" else "Add a picture first (PHOTO)",
                        style = Typed.copy(fontSize = 10.sp, color = JInk.copy(alpha = 0.6f)),
                    )
                }
                Box(
                    Modifier
                        .size(46.dp, 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) JRed else JInk.copy(alpha = if (hasPhoto) 0.35f else 0.15f)),
                    contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(Modifier.padding(3.dp).size(18.dp).clip(RoundedCornerShape(9.dp)).background(JPaper))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val canLoad = !inDeck && ui.empty && tape.playlistId.isNotBlank()
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .border(1.5.dp, if (canLoad) JInk else JInk.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                .clickable(enabled = canLoad, onClick = onLoad)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    inDeck -> "IN THE DECK"
                    tape.playlistId.isBlank() -> "ADD A PLAYLIST LINK FIRST"
                    !ui.empty -> "EJECT THE TAPE IN THE DECK FIRST"
                    else -> "PUT IN DECK"
                },
                style = Typed.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = if (canLoad) JInk else JInk.copy(alpha = 0.4f)),
            )
        }
    }

    when (edit) {
        JEdit.LABEL -> TextDialog("Write on the label", tape.title, onDismiss = { edit = null }) { actions.save(tape.copy(title = it.trim().take(40))) }
        JEdit.PLAYLIST -> PlaylistDialog(tape, onDismiss = { edit = null }) { ref -> actions.save(tape.copy(playlistId = ref.id, service = ref.service)) }
        JEdit.FONT -> FontDialog(tape, onDismiss = { edit = null }) { f -> actions.save(tape.copy(font = f)) }
        JEdit.COLOURS -> ColoursDialog(tape, onDismiss = { edit = null }) { s, l -> actions.save(tape.copy(shell = s, label = l)) }
        JEdit.PHOTO_MENU -> AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text("Picture") },
            text = {
                Column {
                    MenuRow("Move & zoom it") { mx = tape.photoX; my = tape.photoY; mz = tape.photoZoom; moving = true; edit = null }
                    MenuRow("Choose a different picture") { actions.pickPhoto(tape.id); edit = null }
                    MenuRow("Remove the picture") { actions.removePhoto(tape.id); edit = null }
                }
            },
            confirmButton = { TextButton(onClick = { edit = null }) { Text("Close") } },
        )
        JEdit.DELETE -> AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text("Throw this tape away?") },
            text = { Text("\"${tape.title.ifBlank { "my tape" }}\" and its J-card go for good. The playlist in YouTube Music isn't touched.") },
            confirmButton = { TextButton(onClick = { actions.delete(tape.id); edit = null; onClose() }) { Text("Delete", color = JRed) } },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Keep it") } },
        )
        null -> Unit
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Text(text, style = TextStyle(fontSize = 16.sp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp))
}

/** Looks for the J-card buttons (being chosen: see the preview pictures). */
enum class JButtonStyle { PLAIN, METAL, ENAMEL, STAMP }

/** A chunky square key printed on the card, styled to match [theme]. */
@Composable
internal fun CardButton(
    icon: String,
    label: String,
    modifier: Modifier,
    style: JButtonStyle = JButtonStyle.PLAIN,
    theme: dev.foldman.deck.DeckTheme = dev.foldman.deck.DeckTheme.ORIGINAL,
    onClick: () -> Unit,
) {
    val gold = theme == dev.foldman.deck.DeckTheme.BLACK_GOLD
    val shape = RoundedCornerShape(if (style == JButtonStyle.ENAMEL) 25.dp else 6.dp)
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by press.collectIsPressedAsState()
    val glow by androidx.compose.animation.core.animateFloatAsState(if (isPressed) 1f else 0f, androidx.compose.animation.core.tween(if (isPressed) 60 else 380), label = "glow")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val base = Modifier.fillMaxWidth().height(50.dp)
        val iconColor: Color
        val face: Modifier
        when (style) {
            JButtonStyle.PLAIN -> {
                face = base.shadow(3.dp, shape).clip(shape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF45444A), Color(0xFF2A292E))))
                iconColor = Color(0xFFEDE6D8)
            }
            // Like the keys on the front: brushed metal caps (silver, or black for Black & Gold)
            // with a coloured stripe along the bottom edge.
            JButtonStyle.METAL -> {
                val cap = if (gold) listOf(Color(0xFF3A3A3E), Color(0xFF1C1C1F), Color(0xFF0E0E10)) else listOf(Color(0xFFF7F8F9), Color(0xFFD3D5D9), Color(0xFFA6A9AE))
                val stripe = if (gold) BlackGold.Gold else accentFor(theme)
                face = base.shadow(4.dp, shape).clip(shape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(cap))
                    .drawBehind {
                        for (i in 1..3) drawLine(Color.Black.copy(alpha = 0.08f), Offset(size.width * 0.12f, size.height * 0.1f * i), Offset(size.width * 0.88f, size.height * 0.1f * i), 1f)
                        drawRoundRect(stripe, topLeft = Offset(size.width * 0.2f, size.height * 0.84f), size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.05f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
                    }
                    .border(1.dp, Color.White.copy(alpha = if (gold) 0.12f else 0.8f), shape)
                iconColor = if (gold) BlackGold.GoldHi else DeckColors.Engrave
            }
            // Pill-shaped enamel badges in the player's paint, with a chrome (or gold) rim.
            JButtonStyle.ENAMEL -> {
                val paint = if (gold) listOf(Color(0xFF2A2A2D), Color(0xFF121214)) else listOf(Color(0xFF5B92D0), Color(0xFF2F6FB5), Color(0xFF214F86))
                face = base.shadow(4.dp, shape).clip(shape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(paint))
                    .border(2.dp, androidx.compose.ui.graphics.Brush.verticalGradient(if (gold) listOf(BlackGold.GoldHi, BlackGold.GoldLo) else listOf(Color(0xFFF4F5F7), Color(0xFF8C9096))), shape)
                iconColor = if (gold) BlackGold.Gold else Color.White
            }
            // Printed onto the card like a rubber stamp: an inked outline and icon, no button body.
            JButtonStyle.STAMP -> {
                // Drawn straight onto the card in the theme's ink. Pressed, it glows in that colour
                // (a dark ink glows white instead, so the glow always shows).
                val ink = stampInk(theme, jDark)
                val lum = 0.2126f * ink.red + 0.7152f * ink.green + 0.0722f * ink.blue
                val glowColor = if (lum < 0.25f) Color.White else ink
                val stamp = RoundedCornerShape(4.dp)
                face = base
                    .shadow((14 * glow).dp, stamp, ambientColor = glowColor, spotColor = glowColor)
                    .background(glowColor.copy(alpha = (if (lum < 0.25f) 0.85f else 0.35f) * glow), stamp)
                    .border(2.dp, ink, stamp).padding(3.dp).border(0.8.dp, ink.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                iconColor = ink
            }
        }
        Box(face.clickable(interactionSource = press, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
            Text(icon, style = TextStyle(color = iconColor, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = Typed.copy(fontSize = 10.sp, letterSpacing = 1.sp, color = if (style == JButtonStyle.STAMP) stampInk(theme, jDark) else JInk))
    }
}

/** Screen previews only: one row of buttons in a given style and theme, on J-card paper. */
@Composable
internal fun CardButtonSample(style: JButtonStyle, theme: dev.foldman.deck.DeckTheme, modifier: Modifier = Modifier) {
    Column(modifier.background(JPaper).padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardButton("✎", "LABEL", Modifier.weight(1f), style, theme) {}
            CardButton("▣", "PHOTO", Modifier.weight(1f), style, theme) {}
            CardButton("Aa", "FONT", Modifier.weight(1f), style, theme) {}
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardButton("◐", "COLOURS", Modifier.weight(1f), style, theme) {}
            CardButton("♫", "PLAYLIST", Modifier.weight(1f), style, theme) {}
            CardButton("✕", "DELETE", Modifier.weight(1f), style, theme) {}
        }
    }
}

@Composable
private fun TextDialog(title: String, start: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(start) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onSave(text); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlaylistDialog(tape: CustomTape, onDismiss: () -> Unit, onSave: (PlaylistRef) -> Unit) {
    var link by remember { mutableStateOf(tape.shareUrl) }
    val id = parsePlaylist(link)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playlist") },
        text = {
            OutlinedTextField(
                link, { link = it }, singleLine = true, isError = id == null,
                label = { Text("YouTube Music or Spotify playlist link") },
                supportingText = { Text(if (id == null) "That doesn't look like a YouTube Music or Spotify playlist link." else "${id.service.label} playlist found ✓") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = id != null, onClick = { id?.let(onSave); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Each font written out with the tape's own title, so you see it before you pick. */
@Composable
private fun FontDialog(tape: CustomTape, onDismiss: () -> Unit, onPick: (TapeFont) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Writing") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TapeFont.entries.forEach { f ->
                    val on = f == tape.font
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (on) JPaper else Color.Transparent)
                            .border(if (on) 2.dp else 0.dp, if (on) JRed else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { onPick(f); onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            tape.title.ifBlank { "my tape" },
                            style = TextStyle(fontFamily = f.family(), fontWeight = f.weight(), fontSize = (24 * f.sizeScale()).sp, color = if (on) JInk else Color.Unspecified),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(f.label.uppercase(), style = TextStyle(fontSize = 10.sp, letterSpacing = 1.5.sp, color = if (on) JRed else Color.Gray))
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ColoursDialog(tape: CustomTape, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    var shell by remember { mutableLongStateOf(tape.shell) }
    var label by remember { mutableLongStateOf(tape.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colours") },
        text = {
            Column {
                TapeSpineView(tape.title.ifBlank { "my tape" }, Color(shell), Color(label), trailing = null, font = tape.font)
                Spacer(Modifier.height(12.dp))
                Text("Shell", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                ColourSwatches(TapeColors.shells, shell) { shell = it }
                Text("Label", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                ColourSwatches(TapeColors.labels, label) { label = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(shell, label); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColourSwatches(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.chunked(5).let { rows ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            val on = c == selected
                            Canvas(Modifier.size(30.dp).clickable { onPick(c) }) {
                                drawCircle(Color(c))
                                drawCircle(if (on) JRed else Color.Black.copy(alpha = 0.25f), style = Stroke(if (on) 3.dp.toPx() else 1.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The small J-card under the deck: the tape's title in its writing, its picture strip, and the
 * song playing. [compact] for short screens (a phone on its side): picture beside the words.
 */
@Composable
fun MiniJCard(ui: DeckUi, title: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val tape = ui.tape
    val font = tape?.font ?: TapeFont.CLASSIC
    val song = listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { if (ui.tapeCued) "Close the lid and press PLAY" else "—" }
    val titleStyle = TextStyle(fontFamily = font.family(), fontWeight = font.weight(), color = JInk, fontSize = ((if (compact) 17 else 20) * font.sizeScale()).sp)
    Box(
        modifier
            .shadow(6.dp, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(JPaper)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tape?.photoPath != null) {
                    TapePhoto(tape.photoPath, tape.photoX, tape.photoY, tape.photoZoom, Modifier.fillMaxHeight().aspectRatio(PHOTO_STRIP_ASPECT))
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("♪ $song", style = Typed.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Column {
                Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tape?.photoPath != null) {
                    Spacer(Modifier.height(4.dp))
                    TapePhoto(tape.photoPath, tape.photoX, tape.photoY, tape.photoZoom, Modifier.fillMaxWidth().aspectRatio(PHOTO_STRIP_ASPECT * 2.2f))
                }
                Spacer(Modifier.height(4.dp))
                Text("♪ $song", style = Typed.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
            }
        }
    }
}
