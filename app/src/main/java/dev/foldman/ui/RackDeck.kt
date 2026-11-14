// Foldy Streaman addition (not in the original Foldman): the unfolded phone as a tape rack beside
// an open deck. Hold a tape to drag it into the deck, then close the phone and press PLAY.
package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import dev.foldman.deck.DeckTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import dev.foldman.deck.LabelPalette
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.CustomTape
import dev.foldman.deck.DeckUi
import dev.foldman.deck.Source
import dev.foldman.deck.SourceStatus
import dev.foldman.deck.Transport
import kotlin.math.roundToInt

/** Something on the rack: one of the built-in sources, or a tape you made. */
private sealed interface RackItem {
    val key: String

    data class Src(val source: Source) : RackItem {
        override val key get() = "src:${source.name}"
    }

    data class Mine(val tape: CustomTape) : RackItem {
        override val key get() = "tape:${tape.id}"
    }
}

private fun RackItem.title(): String = when (this) {
    is RackItem.Src -> when (source) {
        Source.LOCAL -> "My music files"
        Source.YT_MUSIC -> "YT Music adapter"
        Source.APPLE_MUSIC -> "Apple Music adapter"
    }
    is RackItem.Mine -> tape.title.ifBlank { "my tape" }
}

private fun RackItem.shell(): Color = when (this) {
    is RackItem.Src -> DeckColors.shell(source)
    is RackItem.Mine -> Color(tape.shell)
}

private fun RackItem.font(): dev.foldman.deck.TapeFont = when (this) {
    is RackItem.Src -> dev.foldman.deck.TapeFont.CLASSIC
    is RackItem.Mine -> tape.font
}

private fun RackItem.label(): Color = when (this) {
    is RackItem.Src -> DeckColors.Paper
    is RackItem.Mine -> Color(tape.label)
}

private fun RackItem.inDeck(ui: DeckUi): Boolean = when (this) {
    is RackItem.Src -> !ui.empty && ui.tape == null && ui.source == source
    is RackItem.Mine -> ui.tape?.id == tape.id
}

private val Typewriter = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFFB9B2A8), fontSize = 11.sp)

/**
 * The unfolded phone: the cassette rack on the left, the open deck on the right. Tap a tape to read
 * its card; hold it and drag it into the deck to load it. Closing the phone shows the portable
 * player with that tape in, ready for PLAY.
 */
@Composable
internal fun RackDeckLayout(
    ui: DeckUi,
    anim: DeckAnimations,
    deck: DeckControls,
    actions: ShelfActions,
    modifier: Modifier = Modifier,
    /** Screen previews only: pretend rack item N is being dragged, over the deck or not. */
    previewDrag: Pair<Int, Boolean>? = null,
    settings: SettingsActions = SettingsActions.None,
    /** Screen previews only: open with the Settings page showing. */
    previewSettings: Boolean = false,
    /** Phones that don't fold: shuts the lid and goes back to the player. Null on a foldable. */
    onClose: (() -> Unit)? = null,
    /** Screen previews only: open with rack item N's card showing. */
    previewCard: Int? = null,
) {
    var showSettings by remember { mutableStateOf(previewSettings) }
    val view = LocalView.current
    val density = LocalDensity.current
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    var deckBounds by remember { mutableStateOf(Rect.Zero) }
    var dragging by remember { mutableStateOf<RackItem?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var spineWidth by remember { mutableStateOf(0) }
    var card by remember { mutableStateOf<RackItem?>(null) }
    var editing by remember { mutableStateOf<CustomTape?>(null) }
    var creating by remember { mutableStateOf(false) }
    val overDeck = dragging != null && deckBounds.contains(dragPos)
    val scope = rememberCoroutineScope()
    val sounds = LocalMechSounds.current
    /** How far the tape in the deck has been slid out to the right. */
    val ejectX = remember { Animatable(0f) }
    LaunchedEffect(ui.empty, ui.tape?.id, ui.source) { ejectX.snapTo(0f) }
    // The tape's height in its slot, for sizing the held cassette (not state: no recompose loop).
    val tapeHeight = remember { FloatArray(1) }
    // 0 = still a spine, 1 = flipped over into a whole cassette in your hand.
    val flip = remember { Animatable(0f) }
    LaunchedEffect(dragging?.key) {
        if (dragging != null) {
            flip.snapTo(0f)
            flip.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    // The deck glows light blue while a tape is held: faintly, then fully when it's over the deck.
    val glow by animateFloatAsState(if (overDeck) 1f else if (dragging != null) 0.3f else 0f, tween(200), label = "glow")
    // A full deck won't take another tape: it glows red instead, and the tape goes back to the rack.
    val blocked = overDeck && !ui.empty
    val glowLight by animateColorAsState(if (blocked) BlockedLight else glowLightFor(ui.theme), tween(160), label = "glowLight")
    val glowEdge by animateColorAsState(if (blocked) BlockedEdge else glowEdgeFor(ui.theme), tween(160), label = "glowEdge")
    // Over the deck the cassette turns upright to match the one standing in it.
    val turn by animateFloatAsState(if (overDeck && !blocked) -90f else -4f, tween(260), label = "turn")

    fun load(item: RackItem) {
        when (item) {
            is RackItem.Src -> actions.selectSource(item.source)
            is RackItem.Mine -> actions.tapes.load(item.tape)
        }
    }

    val items = remember(ui.tapes) { Source.entries.map { RackItem.Src(it) } + ui.tapes.map { RackItem.Mine(it) } }
    LaunchedEffect(previewCard) { previewCard?.let { card = items[it] } }
    LaunchedEffect(previewDrag, deckBounds) {
        val (i, over) = previewDrag ?: return@LaunchedEffect
        if (deckBounds == Rect.Zero) return@LaunchedEffect
        dragging = items[i]
        dragPos = if (over) deckBounds.center else Offset(deckBounds.left * 0.5f, deckBounds.center.y)
    }

    Box(modifier.onGloballyPositioned { boxOrigin = it.positionInRoot() }) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // The rack.
            Column(Modifier.weight(0.46f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("CASSETTE RACK", style = EngravedCaps.copy(color = Color(0xFFD9D2C8), fontSize = 13.sp, letterSpacing = 2.sp))
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "⚙ SETTINGS",
                        style = EngravedCaps.copy(color = accentFor(ui.theme), fontSize = 10.sp, letterSpacing = 1.5.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, accentFor(ui.theme).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable { showSettings = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${items.size} TAPES", style = EngravedCaps.copy(color = Color(0xFF8C857C), fontSize = 9.sp))
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0E0D0C))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items.forEach { item ->
                        RackSpine(
                            item = item,
                            inDeck = item.inDeck(ui),
                            beingDragged = dragging?.key == item.key,
                            onWidth = { spineWidth = it },
                            onTap = { card = item },
                            onDragStart = { pos ->
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                dragging = item
                                dragPos = pos
                            },
                            onDrag = { delta -> dragPos += delta },
                            onDragEnd = {
                                val item2 = dragging
                                if (item2 != null && deckBounds.contains(dragPos)) {
                                    if (ui.empty) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        load(item2)
                                    } else {
                                        // Deck's full: the tape just goes back on the rack.
                                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    }
                                }
                                dragging = null
                            },
                        )
                    }
                    // A blank tape: make a new one from a YouTube Music playlist.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF34322F), Color(0xFF22201E))))
                            .clickable { creating = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("BLANK TAPE  ·  MAKE ONE FROM A PLAYLIST", style = EngravedCaps.copy(color = Color(0xFF9C958B), fontSize = 9.sp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "TAP A TAPE TO READ IT  ·  HOLD TO DRAG IT IN",
                    style = EngravedCaps.copy(color = Color(0xFF8C857C), fontSize = 8.sp),
                    maxLines = 2,
                )
            }

            // The open deck.
            Column(Modifier.weight(0.54f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The deck floor, with a slot only a millimetre or so bigger than the tape.
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { deckBounds = it.boundsInRoot() },
                    contentAlignment = Alignment.Center,
                ) {
                    val gap = SlotGap
                    // Stood on end the cassette is 64 wide by 100 tall.
                    val tapeH = minOf(maxHeight - gap * 2, (maxWidth - gap * 2) * (100f / 64f))
                    val tapeW = tapeH * (64f / 100f)
                    tapeHeight[0] = with(density) { tapeH.toPx() }
                    Box(
                        Modifier
                            .size(tapeW + gap * 2, tapeH + gap * 2)
                            .deckGlow(glow, glowLight)
                            .background(Brush.verticalGradient(listOf(Color(0xFF151413), Color(0xFF050505))), RoundedCornerShape(SlotCorner))
                            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(SlotCorner))
                            .deckGlowInside(glow, glowLight, glowEdge),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (ui.empty) {
                            // The bare inside of the deck: spindles, a screw, and a small hint.
                            EmptyDeckInterior(Modifier.size(tapeW, tapeH))
                            Text(
                                "DRAG A TAPE IN",
                                style = EngravedCaps.copy(color = Color(0xFF5E5951), fontSize = 9.sp, letterSpacing = 2.sp),
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                            )
                        } else {
                            FullCassette(
                                ui, anim,
                                modifier = Modifier
                                    .size(tapeW, tapeH)
                                    .offset { IntOffset(ejectX.value.roundToInt(), 0) }
                                    .pointerInput(Unit) {
                                        // Slide the tape to the right to eject it.
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, dx ->
                                                change.consume()
                                                scope.launch { ejectX.snapTo((ejectX.value + dx).coerceAtLeast(0f)) }
                                            },
                                            onDragEnd = {
                                                scope.launch {
                                                    if (ejectX.value > size.width * 0.3f) {
                                                        sounds?.tapeOut()
                                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                        ejectX.animateTo(ejectX.value + 2000f, tween(320, easing = FastOutSlowInEasing))
                                                        actions.tapes.eject()
                                                    } else {
                                                        ejectX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 500f))
                                                    }
                                                }
                                            },
                                            onDragCancel = { scope.launch { ejectX.animateTo(0f) } },
                                        )
                                    },
                                vertical = true,
                            )
                        }
                    }
                    if (overDeck) {
                        Text(
                            if (blocked) "EJECT THE TAPE FIRST" else "DROP TO LOAD",
                            style = EngravedCaps.copy(color = if (blocked) BlockedLight else glowLight, fontSize = 12.sp),
                            modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                // The J-card under the deck: title in the tape's writing, its picture, the song.
                if (!ui.empty) {
                    Spacer(Modifier.height(8.dp))
                    val deckTitle = ui.tape?.title?.ifBlank { "my tape" } ?: RackItem.Src(ui.source).title()
                    MiniJCard(
                        ui, deckTitle,
                        Modifier.fillMaxWidth().then(if (onClose != null) Modifier.height(58.dp) else Modifier),
                        compact = onClose != null,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when {
                                ui.empty -> "DRAG A TAPE INTO THE DECK"
                                onClose != null -> "CLOSE THE LID TO PLAY  ·  SWIPE RIGHT TO EJECT"
                                else -> "CLOSE LID TO PLAY  ·  SWIPE RIGHT TO EJECT"
                            },
                            style = EngravedCaps.copy(color = Color(0xFFD9D2C8), fontSize = if (onClose != null) 10.sp else 12.sp, letterSpacing = 2.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            deckStatus(ui),
                            style = EngravedCaps.copy(color = Color(0xFF8C857C), fontSize = 9.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                    if (onClose != null) {
                        Spacer(Modifier.width(14.dp))
                        CloseLidButton(accentFor(ui.theme), onClose)
                    }
                }
                // No keys in here: like a real player, you open it to change the tape and close it to play.
                Spacer(Modifier.height(6.dp))
            }
        }

        // Tapping one of your tapes slides its J-card out over the rack.
        val cardTape = (card as? RackItem.Mine)?.let { m -> ui.tapes.firstOrNull { it.id == m.tape.id } }
        if (card is RackItem.Mine && cardTape == null) card = null
        if (cardTape != null) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.46f)) {
                JCardSheet(
                    cardTape, ui,
                    JCardActions(
                        save = actions.tapes.save,
                        delete = actions.tapes.delete,
                        pickPhoto = actions.tapes.pickPhoto,
                        removePhoto = actions.tapes.removePhoto,
                    ),
                    onLoad = { if (ui.empty) load(RackItem.Mine(cardTape)); card = null },
                    onClose = { card = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // The tape following your finger: it flips over from a spine into a whole cassette.
        val d = dragging
        if (d != null && spineWidth > 0) {
            val local = dragPos - boxOrigin
            val f = flip.value
            if (f < 0.5f) {
                val spineH = with(density) { 46.dp.toPx() }
                TapeSpineView(
                    title = d.title(),
                    shell = d.shell(),
                    label = d.label(),
                    trailing = null,
                    font = d.font(),
                    modifier = Modifier
                        .width(with(density) { spineWidth.toDp() })
                        .offset { IntOffset((local.x - spineWidth / 2f).roundToInt(), (local.y - spineH / 2f).roundToInt()) }
                        .graphicsLayer {
                            rotationX = f * 180f
                            rotationZ = -4f * f * 2f
                            cameraDistance = 14f * density.density
                        }
                        .shadow(16.dp, RoundedCornerShape(4.dp)),
                )
            } else {
                val heldUi = remember(d, ui.tapes) { heldTapeUi(ui, d) }
                val heldAnim = remember { DeckAnimations() }
                LaunchedEffect(heldAnim) { heldAnim.insert.snapTo(0f) }
                val wPx = with(density) { HeldCassetteWidth.toPx() }
                val hPx = wPx * 64f / 100f
                // Over the deck it grows to the size of the slot, ready to drop in.
                val fitTarget = if (overDeck && tapeHeight[0] > 0f) (tapeHeight[0] / wPx).coerceAtLeast(1f) else 1.06f
                val fit by animateFloatAsState(fitTarget, tween(260), label = "fit")
                FullCassette(
                    heldUi, heldAnim,
                    modifier = Modifier
                        .width(HeldCassetteWidth)
                        .offset { IntOffset((local.x - wPx / 2f).roundToInt(), (local.y - hPx / 2f).roundToInt()) }
                        .graphicsLayer {
                            rotationX = (f - 1f) * 180f
                            rotationZ = turn
                            scaleX = fit; scaleY = fit
                            cameraDistance = 14f * density.density
                            shadowElevation = 24f * density.density
                            shape = RoundedCornerShape(8.dp)
                        },
                    vertical = false,
                )
            }
        }
    }

    if (showSettings) {
        // Settings sits over the rack and deck, in the same dark style.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(enabled = false) {}.then(modifier)) {
            SettingsPanel(ui, settings, onClose = { showSettings = false }, modifier = Modifier.fillMaxSize())
        }
    }

    (card as? RackItem.Src)?.let { item ->
        TapeCardDialog(
            item = item,
            ui = ui,
            actions = actions,
            onLoad = { if (ui.empty) load(item); card = null },
            onEdit = { card = null },
            onDismiss = { card = null },
        )
    }
    if (creating) TapeEditorDialog(null, actions.tapes) { creating = false }
    editing?.let { t -> TapeEditorDialog(t, actions.tapes) { editing = null } }
}

private fun deckStatus(ui: DeckUi): String {
    if (ui.empty) return "NO TAPE"
    // The J-card above shows the tape and the song, so this is just the state and the time.
    val time = if (ui.durationMs > 0) "${formatTime(ui.positionMs)} / ${formatTime(ui.durationMs)}" else null
    val state = when {
        ui.status == SourceStatus.NeedsAccess -> "NEEDS NOTIFICATION ACCESS (SEE SETTINGS)"
        ui.tape != null && ui.tapeCued -> "PRESS PLAY TO START"
        ui.transport == Transport.PLAYING -> "PLAYING"
        ui.transport == Transport.PAUSED -> "PAUSED"
        else -> "STOPPED"
    }
    return listOfNotNull(state, time).joinToString("  ·  ").uppercase()
}

@Composable
private fun RackSpine(
    item: RackItem,
    inDeck: Boolean,
    beingDragged: Boolean,
    onWidth: (Int) -> Unit,
    onTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val gestures = Modifier
        .onGloballyPositioned { coords = it; onWidth(it.size.width) }
        .pointerInput(item.key) {
            detectDragGesturesAfterLongPress(
                onDragStart = { off -> onDragStart(coords?.localToRoot(off) ?: off) },
                onDrag = { change, amount -> change.consume(); onDrag(amount) },
                onDragEnd = onDragEnd,
                onDragCancel = onDragEnd,
            )
        }
        .pointerInput(item.key) { detectTapGestures(onTap = { onTap() }) }

    if (inDeck) {
        // The slot the tape came out of: an empty gap in the rack.
        Box(
            gestures
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF1C1A18), Color(0xFF3A342C), Color(0xFF1C1A18)))),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("  IN DECK  ·  ${item.title().uppercase()}", style = EngravedCaps.copy(color = Color(0xFF8C857C), fontSize = 9.sp), maxLines = 1)
        }
    } else {
        TapeSpineView(
            title = item.title(),
            shell = item.shell(),
            label = item.label(),
            font = item.font(),
            trailing = if (item is RackItem.Src && item.source != Source.LOCAL) "AUX" else null,
            modifier = gestures.alpha(if (beingDragged) 0.25f else 1f).shadow(3.dp, RoundedCornerShape(4.dp)),
        )
    }
}

/** The J-card: what's on the tape, and buttons to load or change it. */
@Composable
private fun TapeCardDialog(
    item: RackItem,
    ui: DeckUi,
    actions: ShelfActions,
    onLoad: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hand = TextStyle(fontFamily = FontFamily.Cursive, fontSize = 26.sp)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title(), style = hand) },
        text = {
            Column {
                when (item) {
                    is RackItem.Mine -> {
                        Text("SIDE A", style = Typewriter.copy(color = DeckColors.AccentDeep))
                        Text("YouTube Music playlist", style = Typewriter.copy(color = Color.Unspecified, fontSize = 13.sp))
                        Text(item.tape.playlistId, style = Typewriter.copy(color = Color.Gray, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        Text("Drop it in the deck (or tap Put in deck), close the phone and press PLAY. YT Music starts this playlist.", style = Typewriter.copy(color = Color.Unspecified, fontSize = 12.sp))
                    }
                    is RackItem.Src -> {
                        Text(
                            when (item.source) {
                                Source.LOCAL -> "Plays the music files in a folder on this phone."
                                else -> "An adapter: controls whatever ${item.source.label} is playing. Pick songs in ${item.source.label} itself."
                            },
                            style = Typewriter.copy(color = Color.Unspecified, fontSize = 12.sp),
                        )
                        Spacer(Modifier.height(8.dp))
                        if (item.source == Source.LOCAL) {
                            if (ui.folderName != null) Text("Folder: ${ui.folderName} · ${ui.tracks.size} songs", style = Typewriter.copy(color = Color.Gray))
                            TextButton(onClick = actions.pickFolder) { Text(if (ui.folderName == null) "Choose music folder" else "Change folder") }
                        } else {
                            TextButton(onClick = actions.openAccess) { Text("Notification access settings") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLoad, enabled = !item.inDeck(ui) && ui.empty) {
                Text(
                    when {
                        item.inDeck(ui) -> "In the deck"
                        !ui.empty -> "Eject the tape first"
                        else -> "Put in deck"
                    },
                )
            }
        },
        dismissButton = {
            Row {
                if (item is RackItem.Mine) TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

/** The app's light blue (the front player's enamel, lightened) for the glowing drop area. */
private val GlowBlue = Color(0xFF8CC4FF)
private val GlowEdge = Color(0xFF4A9BFF)
private val BlockedLight = Color(0xFFFF7A6E)
private val BlockedEdge = Color(0xFFE53935)

/** The drop glow follows the theme: light blue for Original, gold for Black & Gold. */
private fun glowLightFor(theme: DeckTheme) = when (theme) {
    DeckTheme.ORIGINAL -> GlowBlue
    DeckTheme.BLACK_GOLD -> BlackGold.GoldHi
}

private fun glowEdgeFor(theme: DeckTheme) = when (theme) {
    DeckTheme.ORIGINAL -> GlowEdge
    DeckTheme.BLACK_GOLD -> BlackGold.Gold
}
private val HeldCassetteWidth = 230.dp
/** Room around the tape in its slot: about a millimetre on the Fold's screen. */
private val SlotGap = 7.dp
private val SlotCorner = 10.dp

/** What the held cassette shows: the tape's own label, not the song that's playing. */
private fun heldTapeUi(ui: DeckUi, item: RackItem): DeckUi = when (item) {
    is RackItem.Mine -> ui.copy(source = Source.YT_MUSIC, tape = item.tape, title = "", artist = "", artwork = null, label = LabelPalette.Default, empty = false)
    is RackItem.Src -> ui.copy(source = item.source, tape = null, title = "", artist = "", artwork = null, label = LabelPalette.Default, empty = false)
}

/** A soft light-blue halo around the deck, strongest when a tape is over it. */
private fun Modifier.deckGlow(glow: Float, light: Color): Modifier = drawBehind {
    if (glow <= 0f) return@drawBehind
    val r = SlotCorner.toPx()
    for (i in 1..6) {
        val spread = i * 3.dp.toPx()
        drawRoundRect(
            light.copy(alpha = 0.28f * glow * (1f - i / 7f)),
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2, size.height + spread * 2),
            cornerRadius = CornerRadius(r + spread),
            style = Stroke(3.dp.toPx()),
        )
    }
}

/** Inside the deck: a light-blue wash and a bright blue outline over everything. */
private fun Modifier.deckGlowInside(glow: Float, light: Color, edge: Color): Modifier = drawWithContent {
    drawContent()
    if (glow <= 0f) return@drawWithContent
    val r = CornerRadius(SlotCorner.toPx())
    drawRoundRect(
        Brush.radialGradient(
            listOf(light.copy(alpha = 0.5f * glow), light.copy(alpha = 0.22f * glow)),
            center = center, radius = size.maxDimension * 0.7f,
        ),
        cornerRadius = r,
    )
    drawRoundRect(light.copy(alpha = 0.55f * glow), cornerRadius = r, style = Stroke(14.dp.toPx()))
    drawRoundRect(edge.copy(alpha = glow), cornerRadius = r, style = Stroke(4.dp.toPx()))
}

/** Phones only: the lid latch. Shuts the deck and goes back to the player. */
@Composable
private fun CloseLidButton(accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3632), Color(0xFF1E1C1A))))
            .border(1.5.dp, accent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▼", style = EngravedCaps.copy(color = accent, fontSize = 11.sp))
        Spacer(Modifier.width(8.dp))
        Text("CLOSE", style = EngravedCaps.copy(color = accent, fontSize = 13.sp, letterSpacing = 3.sp))
    }
}
