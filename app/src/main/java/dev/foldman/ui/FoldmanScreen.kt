package dev.foldman.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import dev.foldman.deck.DeckEvent
import dev.foldman.deck.DeckTheme
import dev.foldman.deck.DeckUi
import dev.foldman.deck.DeckViewModel
import dev.foldman.deck.Source
import kotlinx.coroutines.launch

private enum class Posture { Cover, Unfolded, Tabletop }

/** Body enamel: the blue of the first portable cassette players. */
private val Enamel = Color(0xFF2F6FB5)

/**
 * Picks a layout from the hinge:
 *  - folded (cover screen): the portable player stood upright, tape on end behind its window,
 *    keys and volume down a silver side strip;
 *  - unfolded: the same player laid down as a deck, with the library above and now-playing below;
 *  - half-open on a table (Flex mode): the deck on the upper half, the library on the lower.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldmanScreen(
    vm: DeckViewModel,
    windowLayout: WindowLayoutInfo?,
    onPickFolder: () -> Unit,
    onInstallApp: (Source) -> Unit,
    onAppleSignIn: () -> Unit,
    onPickPhoto: (tapeId: String) -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val anim = rememberDeckAnimations(ui.transport, ui.progress)
    val sounds = remember { MechSounds() }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val feel by rememberUpdatedState(ui)
    // Settings > Tape shelf on the front screen: the shelf that slides up from the TAPES button.
    var shelfOpen by rememberSaveable { mutableStateOf(false) }

    // A new tape drops into the deck whenever the source changes (and when the app opens).
    // Foldy Streaman: the tape slides the last bit into the slot and clicks as it seats.
    LaunchedEffect(ui.source, ui.tape?.id, ui.empty) {
        if (ui.empty) return@LaunchedEffect
        anim.insert.snapTo(0.35f)
        anim.insert.animateTo(0f, tween(230, easing = androidx.compose.animation.core.FastOutLinearInEasing))
        if (feel.keySounds) sounds.tapeIn()
        if (feel.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            if (e == DeckEvent.KeyClick) {
                if (feel.keySounds) sounds.keyDown()
                if (feel.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                return@collect
            }
            if (feel.keySounds) sounds.autoStop()
            when (e) {
                DeckEvent.TapeEnd -> if (feel.haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                DeckEvent.Eject -> {
                    if (feel.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    scope.launch {
                        anim.pop.animateTo(1f, tween(90))
                        anim.pop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 500f))
                    }
                }
                DeckEvent.KeyClick -> Unit
            }
        }
    }

    val appleActions = remember(vm) {
        AppleActions(
            signIn = onAppleSignIn,
            signOut = vm::signOutOfApple,
            setDeveloperToken = vm::setAppleDeveloperToken,
            refresh = vm::loadApplePlaylists,
            openPlaylist = vm::openApplePlaylist,
            closePlaylist = vm::closeApplePlaylist,
            play = vm::playApplePlaylist,
        )
    }
    val tapeActions = remember(vm) { TapeActions(load = vm::loadTape, save = vm::saveTape, delete = vm::deleteTape, eject = vm::ejectTape, pickPhoto = onPickPhoto, removePhoto = vm::removeTapePhoto, move = vm::moveTape, setCover = vm::setTapeCover) }
    val shelfActions = remember(vm) {
        ShelfActions(
            selectSource = vm::selectSource,
            pickFolder = onPickFolder,
            playTrack = vm::playTrack,
            openAccess = vm::openAccessSettings,
            openApp = vm::openSourceApp,
            installApp = onInstallApp,
            apple = appleActions,
            tapes = tapeActions,
        )
    }
    val deck = remember(vm) { DeckControls(vm) }
    val settingsActions = remember(vm) {
        SettingsActions(
            setTheme = vm::setTheme,
            setKeySounds = vm::setKeySounds,
            setHaptics = vm::setHaptics,
            openAccess = vm::openAccessSettings,
            pickFolder = onPickFolder,
            appleSignIn = onAppleSignIn,
            appleSignOut = vm::signOutOfApple,
            setStopOnOpen = vm::setStopOnOpen,
            setVolumeKeys = vm::setVolumeKeys,
            setFrontShelf = vm::setFrontShelf,
            setJcardDark = vm::setJcardDark,
            setSourceShown = vm::setSourceShown,
            setHingeStyle = vm::setHingeStyle,
            backup = onBackup,
            restore = onRestore,
        )
    }
    val fold = windowLayout?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()

    CompositionLocalProvider(
        LocalMechSounds provides sounds.takeIf { ui.keySounds },
        LocalHaptics provides ui.haptics,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF2A2420), DeckColors.Backdrop), radius = 1800f)),
        ) {
            val density = LocalDensity.current
            // Held sideways the inner screen is short, so the library gets a bigger share.
            val landscape = maxWidth > maxHeight
            val posture = when {
                // A phone that doesn't fold: EJECT opens the deck, CLOSE shuts it.
                ui.phoneMode -> if (ui.deckOpen) Posture.Unfolded else Posture.Cover
                fold != null && fold.state == FoldingFeature.State.HALF_OPENED && fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> Posture.Tabletop
                fold != null && fold.orientation == FoldingFeature.Orientation.VERTICAL -> Posture.Unfolded
                maxWidth >= 600.dp && maxWidth > maxHeight * 0.7f -> Posture.Unfolded
                else -> Posture.Cover
            }
            val lastPosture = remember { arrayOfNulls<Posture>(1) }
            LaunchedEffect(posture) {
                // Phones: the open deck in landscape is short, so it gets the whole screen too.
                view.showSystemBars(posture != Posture.Cover && !ui.phoneMode)
                vm.setCoverShown(posture == Posture.Cover)
                // The Fold was just opened (not on first launch): Settings may say stop the music.
                if (!ui.phoneMode && lastPosture[0] == Posture.Cover && posture != Posture.Cover) vm.onPhoneOpened()
                lastPosture[0] = posture
            }
            when (posture) {
                // The cover screen is the player itself: edge to edge, system bars hidden.
                Posture.Cover -> when (ui.theme) {
                    DeckTheme.ORIGINAL -> CoverWalkman(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                    DeckTheme.BLACK_GOLD -> CoverBlackGold(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                    DeckTheme.SPORT_YELLOW -> CoverSportYellow(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                    DeckTheme.RED_CHROME -> CoverRedChrome(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                    DeckTheme.SILVER_BLUE -> CoverSilverBlue(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                    DeckTheme.CHERRY_RED -> CoverCherryRed(ui, anim, deck, onFixAccess = vm::openAccessSettings, modifier = Modifier.fillMaxSize(), onTapes = { shelfOpen = true })
                }
                // Foldy Streaman: open to pick a tape, close to play.
                // Inside, the body round the rack and deck is the theme's own material.
                Posture.Unfolded -> Box(Modifier.fillMaxSize().insideBody(ui.theme)) {
                    // The hinge runs the whole height of the screen, top edge to bottom, right on the fold.
                    if (!ui.phoneMode) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            when (ui.hingeStyle) {
                                dev.foldman.deck.HingeStyle.PIANO -> PianoHinge(theme = ui.theme)
                                dev.foldman.deck.HingeStyle.THREE -> FoldHinges(theme = ui.theme)
                                dev.foldman.deck.HingeStyle.NONE -> Unit
                            }
                        }
                    }
                    RackDeckLayout(
                        ui, anim, deck, shelfActions,
                        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
                        settings = settingsActions,
                        onClose = if (ui.phoneMode) vm::closeDeck else null,
                    )
                }
                Posture.Tabletop -> {
                    val (topH, gap) = with(density) {
                        fold!!.bounds.top.toDp() to (fold.bounds.bottom - fold.bounds.top).toDp()
                    }
                    Column(Modifier.fillMaxSize()) {
                        DeckBand(
                            ui, anim, deck, vm,
                            Modifier
                                .height(topH)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                        )
                        Spacer(Modifier.height(gap))
                        LibraryBand(
                            ui, shelfActions, vm::playQueueItem,
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                        )
                    }
                }
            }
            // Foldy Streaman: the front-screen tape shelf, only when it's switched on in Settings.
            val sheetHeight = maxHeight * 0.8f
            if (shelfOpen && ui.frontShelf && posture == Posture.Cover) {
                ModalBottomSheet(
                    onDismissRequest = { shelfOpen = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = drawerColor(ui.theme),
                ) {
                    TapeShelf(
                        ui,
                        ShelfActions(
                            selectSource = { vm.selectSource(it) },
                            pickFolder = onPickFolder,
                            playTrack = { vm.playTrack(it); shelfOpen = false },
                            openAccess = vm::openAccessSettings,
                            openApp = vm::openSourceApp,
                            installApp = onInstallApp,
                            apple = appleActions,
                            tapes = tapeActions,
                        ),
                        Modifier.fillMaxWidth().height(sheetHeight),
                        settings = settingsActions,
                    )
                }
            }
            // The inside screen darkens from the fold out as the phone closes (and clears as it opens).
            if (!ui.phoneMode && posture != Posture.Cover) {
                HingeShade(verticalFold = fold?.orientation != FoldingFeature.Orientation.HORIZONTAL, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Hides the status and navigation bars (a swipe from the edge brings them back briefly), or restores them. */
private fun android.view.View.showSystemBars(show: Boolean) {
    var ctx = context
    while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
    val window = (ctx as? android.app.Activity)?.window ?: return
    WindowCompat.getInsetsController(window, this).run {
        if (show) {
            show(WindowInsetsCompat.Type.systemBars())
        } else {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** Key actions, bundled so the layouts don't each need the view model. */
internal class DeckControls(
    val play: () -> Unit,
    val pause: () -> Unit,
    val stop: () -> Unit,
    val rewind: () -> Unit,
    val fastForward: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val volume: (Float) -> Unit,
    /** The headphone jack: choose where the sound goes. */
    val output: () -> Unit = {},
) {
    constructor(vm: DeckViewModel) : this(vm::play, vm::pauseKey, vm::stop, vm::rewind, vm::fastForward, vm::next, vm::previous, vm::setVolume, vm::openOutputSwitcher)
}

private val Serif = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = Color(0xFFF1F3F6), letterSpacing = 2.sp)
private val OnEnamel = EngravedCaps.copy(color = Color(0xFFDCE6F2))

/** The folded phone: an upright portable player. */
@Composable
internal fun CoverWalkman(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, onFixAccess: () -> Unit, modifier: Modifier, onTapes: () -> Unit = {}) {
    BoxWithConstraints(modifier) {
        val capH = maxHeight * 0.12f
        Row(Modifier.fillMaxSize().paintedBody(Enamel, capHeight = capH, corner = 0.dp, framed = false)) {
            Column(
                Modifier
                    .weight(0.73f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                    .padding(start = 20.dp, end = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(capH)) {
                    // Inset from the corner so the display's rounded corner doesn't clip it.
                    // Foldy Streaman: tapes live in the rack, which opens with the phone.
                    // The yellow hotline button, as on the original players. With the front tape shelf
                    // switched on in Settings it opens the shelf (and says TAPES); otherwise it's just for the look.
                    HotlineButton(if (ui.frontShelf) onTapes else ({}), Modifier.align(Alignment.TopStart).padding(top = 18.dp, start = 8.dp).size(60.dp, 30.dp))
                    if (ui.frontShelf) {
                        Text("TAPES", style = OnEnamel.copy(fontSize = 7.sp), modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 24.dp))
                    } else {
                        Text(changeTapesHint(ui), style = OnEnamel.copy(fontSize = 7.sp), modifier = Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 4.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("STEREO", style = Serif.copy(fontSize = 30.sp))
                Spacer(Modifier.height(18.dp))
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        TapeWindow(ui, anim, vertical = true)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    listOf(ui.title, ui.artist).filter { it.isNotBlank() }.joinToString("  —  ").ifBlank { ui.tape?.title?.ifBlank { "my tape" } ?: "NO TAPE" }.let { if (ui.empty) "NO TAPE" else it }.uppercase(),
                    style = OnEnamel.copy(fontSize = 9.sp, letterSpacing = 1.5.sp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (ui.status == dev.foldman.deck.SourceStatus.NeedsAccess) {
                    // Without notification access Foldy can't see or control YT Music.
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "TAP: TURN ON NOTIFICATION ACCESS",
                        style = OnEnamel.copy(fontSize = 9.sp, color = Color(0xFFFFE07A)),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable(onClick = onFixAccess)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Wordmark("STREAMAN", 40.sp)
                Spacer(Modifier.height(20.dp))
            }
            SideStrip(ui, deck, Modifier.weight(0.27f).fillMaxHeight())
        }
    }
}

/** The silver strip down the side of the case: mic, LED, skip, keys, volume, phones. */
@Composable
private fun SideStrip(ui: DeckUi, deck: DeckControls, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val keyH = ((maxHeight - 420.dp) / 5 - 18.dp).coerceIn(34.dp, 64.dp)
        // Runs the full height of the screen; the padding keeps controls clear of the rounded corners.
        Column(
            Modifier
                .fillMaxSize()
                .chassis(corner = 0.dp, screws = false)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(start = 10.dp, end = 12.dp, top = 28.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MicGrille(Modifier.fillMaxWidth().height(26.dp))
            Text("MIC", style = EngravedCaps.copy(fontSize = 7.sp))
            Spacer(Modifier.height(8.dp))
            StatusLed(ui.transport)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MiniButton("AMS", deck.previous, symbol = KeySymbol.PREV, diameter = 26.dp)
                MiniButton("AMS", deck.next, symbol = KeySymbol.NEXT, diameter = 26.dp, enabled = ui.hasNext)
            }
            Spacer(Modifier.height(10.dp))
            TransportKeys(ui, deck.stop, deck.rewind, deck.play, deck.fastForward, deck.pause, Modifier.fillMaxWidth(), keyHeight = keyH, vertical = true, redPlay = true)
            Spacer(Modifier.height(12.dp))
            Text("VOLUME", style = EngravedCaps.copy(fontSize = 7.sp))
            VolumeSlider(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(8.dp))
            Text("PHONES", style = EngravedCaps.copy(fontSize = 6.sp))
            Spacer(Modifier.height(3.dp))
            // Foldy Streaman: a proper socket; a plug sits in it when the sound isn't on the phone.
            HeadphoneJack(ui.audioElsewhere, deck.output, Modifier.size(40.dp))
        }
    }
}

/** The unfolded phone: the player laid down as a deck. */
@Composable
private fun DeckBand(ui: DeckUi, anim: DeckAnimations, deck: DeckControls, vm: DeckViewModel, modifier: Modifier) {
    Row(modifier.paintedBody(Enamel, corner = 22.dp)) {
        Column(Modifier.weight(0.74f).fillMaxHeight().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("STEREO", style = Serif.copy(fontSize = 24.sp))
                Spacer(Modifier.weight(1f))
                Text("FM-F8 · STEREO CASSETTE PLAYER", style = OnEnamel.copy(fontSize = 8.sp))
                Spacer(Modifier.width(16.dp))
                Wordmark("STREAMAN", 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(150.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                    Row(Modifier.fillMaxWidth().weight(1f).bezel().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        VuMeter(ui.transport, ui.source, Modifier.weight(1f).fillMaxHeight(), channel = 0)
                        VuMeter(ui.transport, ui.source, Modifier.weight(1f).fillMaxHeight(), channel = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.bezel().padding(5.dp)) { TapeCounter(anim.reels, digitWidth = 16.dp) }
                        Spacer(Modifier.width(8.dp))
                        MiniButton("RESET", { anim.reels.resetCounter() }, diameter = 20.dp)
                        Spacer(Modifier.width(6.dp))
                        StatusLed(ui.transport)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    TapeWindow(ui, anim)
                }
                Spacer(Modifier.width(12.dp))
                EjectArrow({ deck.stop() }, Modifier.size(26.dp, 50.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                MiniButton("AMS", deck.previous, symbol = KeySymbol.PREV, diameter = 30.dp)
                Spacer(Modifier.width(10.dp))
                TransportKeys(ui, deck.stop, deck.rewind, deck.play, deck.fastForward, deck.pause, Modifier.weight(1f), keyHeight = 58.dp)
                Spacer(Modifier.width(10.dp))
                MiniButton("AMS", deck.next, symbol = KeySymbol.NEXT, diameter = 30.dp, enabled = ui.hasNext)
            }
        }
        // Silver end strip: mic, switches, volume and phones.
        Row(
            Modifier
                .weight(0.26f)
                .fillMaxHeight()
                .padding(6.dp)
                .chassis(corner = 18.dp, screws = false)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                MicGrille(Modifier.fillMaxWidth().height(30.dp))
                Text("MIC", style = EngravedCaps.copy(fontSize = 7.sp))
                Spacer(Modifier.weight(1f))
                SlideSwitch(ui.keySounds, vm::setKeySounds, "CLICK", height = 54.dp)
                Spacer(Modifier.height(8.dp))
                SlideSwitch(ui.haptics, vm::setHaptics, "HAPTIC", height = 54.dp)
                Spacer(Modifier.weight(1f))
                PhonesJack(Modifier.size(18.dp))
                Text("PHONES", style = EngravedCaps.copy(fontSize = 6.sp))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VOLUME", style = EngravedCaps.copy(fontSize = 7.sp))
                VolumeSlider(ui.volume, deck.volume, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
