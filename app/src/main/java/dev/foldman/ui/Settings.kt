// Foldy Streaman addition (not in the original Foldman): the Settings page on the unfolded phone.
package dev.foldman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckTheme
import dev.foldman.deck.DeckUi
import dev.foldman.deck.SourceStatus

/** What the Settings page can change. */
class SettingsActions(
    val setTheme: (DeckTheme) -> Unit,
    val setKeySounds: (Boolean) -> Unit,
    val setHaptics: (Boolean) -> Unit,
    val openAccess: () -> Unit,
    val pickFolder: () -> Unit,
    val appleSignIn: () -> Unit,
    val appleSignOut: () -> Unit,
    val setStopOnOpen: (Boolean) -> Unit = {},
    val setVolumeKeys: (Boolean) -> Unit = {},
    val setLockScreen: (Boolean) -> Unit = {},
    val setPhonePortrait: (Boolean) -> Unit = {},
    val setNoticeMode: (dev.foldman.deck.NoticeMode) -> Unit = {},
    val spotifyLogin: () -> Unit = {},
    val youTubeLogin: () -> Unit = {},
    val youTubeLogout: () -> Unit = {},
    val spotifyLogout: () -> Unit = {},
    val setSpotifyClientId: (String) -> Unit = {},
    val setFrontShelf: (Boolean) -> Unit = {},
    val setJcardDark: (Boolean) -> Unit = {},
    val setSourceShown: (dev.foldman.deck.Source, Boolean) -> Unit = { _, _ -> },
    val setHingeStyle: (dev.foldman.deck.HingeStyle) -> Unit = {},
    val backup: () -> Unit = {},
    val restore: () -> Unit = {},
) {
    companion object {
        val None = SettingsActions({}, {}, {}, {}, {}, {}, {})
    }
}

/** The highlight colour inside the app for each theme. */
fun accentFor(theme: DeckTheme): Color = when (theme) {
    DeckTheme.ORIGINAL -> DeckColors.Accent
    DeckTheme.BLACK_GOLD -> BlackGold.Gold
    DeckTheme.CHERRY_RED -> Color(0xFFFF6F84)
    DeckTheme.SILVER_BLUE -> Color(0xFF6F8CFF)
    DeckTheme.RED_CHROME -> Color(0xFFFF5A66)
    DeckTheme.SPORT_YELLOW -> SportYellow.Yellow
}

private val Label = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFFD9D2C8), fontSize = 13.sp)
private val Note = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFF8C857C), fontSize = 11.sp, lineHeight = 15.sp)

@Composable
internal fun SettingsPanel(ui: DeckUi, actions: SettingsActions, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentFor(ui.theme)
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1A19), Color(0xFF0B0B0B))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SETTINGS", style = EngravedCaps.copy(color = Color(0xFFD9D2C8), fontSize = 15.sp, letterSpacing = 3.sp), modifier = Modifier.weight(1f))
            Text(
                "DONE",
                style = EngravedCaps.copy(color = accent, fontSize = 12.sp, letterSpacing = 2.sp),
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClose).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Section("THEME") {
                // Two themes to a row, so the cards stay big enough to see.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeckTheme.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { t ->
                                ThemeCard(t, selected = ui.theme == t, accent = accent, onClick = { actions.setTheme(t) }, modifier = Modifier.weight(1f))
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Changes the front screen player and the highlights in here.", style = Note)
                if (!ui.phoneMode) {
                    Spacer(Modifier.height(14.dp))
                    Text("Hinges down the fold", style = Label)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dev.foldman.deck.HingeStyle.entries.forEach { h ->
                            val on = ui.hingeStyle == h
                            Text(
                                h.label.uppercase(),
                                style = EngravedCaps.copy(color = if (on) accent else Color(0xFFD9D2C8), fontSize = 10.sp, letterSpacing = 1.5.sp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(if (on) 2.dp else 1.dp, if (on) accent else Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .clickable { actions.setHingeStyle(h) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
            Section("NOTIFICATIONS ON THE PLAYER") {
                Text("New notifications show on the front player for a few seconds, each theme in its own spot. Needs notification access. The phone's own pop-up still appears too.", style = Note)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dev.foldman.deck.NoticeMode.entries.forEach { m ->
                        val on = ui.noticeMode == m
                        Text(
                            m.label.uppercase(),
                            style = EngravedCaps.copy(color = if (on) accent else Color(0xFFD9D2C8), fontSize = 10.sp, letterSpacing = 1.5.sp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(if (on) 2.dp else 1.dp, if (on) accent else Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .clickable { actions.setNoticeMode(m) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Section("FEEL") {
                ToggleRow("Click sounds", "Keys, tapes going in and out", ui.keySounds, accent) { actions.setKeySounds(!ui.keySounds) }
                ToggleRow("Haptics", "A buzz with each click", ui.haptics, accent) { actions.setHaptics(!ui.haptics) }
                ToggleRow("Dark J-cards", "Tape cards on dark paper, with lighter buttons", ui.jcardDark, accent) { actions.setJcardDark(!ui.jcardDark) }
                ToggleRow(
                    "Tape shelf on the front screen",
                    "A TAPES button on the player, to change tapes without opening it",
                    ui.frontShelf, accent,
                ) { actions.setFrontShelf(!ui.frontShelf) }
                if (ui.phoneMode) {
                    ToggleRow(
                        "Open the deck upright",
                        "EJECT keeps the phone upright: the tape lies across the top with the rack and J-card below (more room when the keyboard is up). Off = the deck opens sideways",
                        ui.phonePortrait, accent,
                    ) { actions.setPhonePortrait(!ui.phonePortrait) }
                }
                ToggleRow(
                    "Show the player on the lock screen",
                    "If Foldy is the app you had open, waking the phone shows the player and its keys work without unlocking. Opening the tapes or leaving Foldy asks you to unlock",
                    ui.lockScreen, accent,
                ) { actions.setLockScreen(!ui.lockScreen) }
                ComeBackRow(accent)
                ToggleRow("Volume buttons work the player", "On the front screen: up = play, down = pause, double-tap to skip or stop. Off = normal volume", ui.volumeKeys, accent) { actions.setVolumeKeys(!ui.volumeKeys) }
                if (!ui.phoneMode) {
                    ToggleRow("Stop the music when I open the phone", "Like opening the lid of a real player", ui.stopOnOpen, accent) { actions.setStopOnOpen(!ui.stopOnOpen) }
                }
            }
            Section("YT MUSIC & APPLE MUSIC") {
                val needs = ui.status == SourceStatus.NeedsAccess
                ActionRow(
                    "Notification access",
                    if (needs) "Off: Foldy can't see or control YT Music" else "On (or not needed for this tape)",
                    "OPEN", accent, actions.openAccess,
                )
                ActionRow(
                    "Apple Music",
                    if (ui.apple.signedIn) "Signed in" else "Only needed for browsing Apple Music",
                    if (ui.apple.signedIn) "SIGN OUT" else "SIGN IN", accent,
                    if (ui.apple.signedIn) actions.appleSignOut else actions.appleSignIn,
                )
            }
            Section("YOUTUBE MUSIC") {
                ActionRow(
                    "Google account",
                    if (ui.youTubeSignedIn) "Logged in. Pick your YouTube Music playlists from a list when you make a tape"
                    else "Log in to pick your YouTube Music playlists from a list instead of pasting links",
                    if (ui.youTubeSignedIn) "LOG OUT" else "LOG IN", accent,
                    if (ui.youTubeSignedIn) actions.youTubeLogout else actions.youTubeLogin,
                )
                ui.youTubeMessage?.let { Text(it, style = Note.copy(color = Color(0xFFFF8A80))) }
            }
            Section("SPOTIFY") {
                val name = ui.spotifyName
                ActionRow(
                    "Spotify account",
                    if (name != null) "Logged in as $name. Spotify tapes start without leaving Foldy, and you can pick playlists from a list"
                    else "Log in to pick your playlists from a list and start Spotify tapes without leaving Foldy (needs Premium)",
                    if (name != null) "LOG OUT" else "LOG IN", accent,
                    if (name != null) actions.spotifyLogout else actions.spotifyLogin,
                )
                ui.spotifyMessage?.let { Text(it, style = Note.copy(color = Color(0xFFFF8A80))) }
            }
            Section("ADAPTERS ON THE RACK") {
                Text("Switch off the ones you don't use. Your own playlist tapes stay either way.", style = Note)
                Spacer(Modifier.height(6.dp))
                listOf(
                    dev.foldman.deck.Source.LOCAL to ("My music files" to "Music files saved on this phone"),
                    dev.foldman.deck.Source.YT_MUSIC to ("YT Music adapter" to "Controls whatever YT Music is playing"),
                    dev.foldman.deck.Source.SPOTIFY to ("Spotify adapter" to "Controls whatever Spotify is playing"),
                    dev.foldman.deck.Source.APPLE_MUSIC to ("Apple Music adapter" to "Controls whatever Apple Music is playing"),
                ).forEach { (src, words) ->
                    val on = src !in ui.hiddenSources
                    ToggleRow(words.first, words.second, on, accent) { actions.setSourceShown(src, !on) }
                }
            }
            Section("BACKUP") {
                ActionRow("Back up tapes", "Saves every tape (pictures and all) and your settings to one file", "SAVE", accent, actions.backup)
                ActionRow("Restore tapes", "Loads a backup file, e.g. after reinstalling the app", "LOAD", accent, actions.restore)
                ui.backupMessage?.let { Text(it, style = Note.copy(color = accent)) }
            }
            Section("MY MUSIC FILES") {
                ActionRow(
                    "Music folder",
                    ui.folderName?.let { "$it · ${ui.tracks.size} songs" } ?: "None chosen",
                    if (ui.folderName == null) "CHOOSE" else "CHANGE", accent, actions.pickFolder,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = EngravedCaps.copy(color = Color(0xFF8C857C), fontSize = 10.sp, letterSpacing = 2.sp))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ThemeCard(theme: DeckTheme, selected: Boolean, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A tiny swatch of the front: body colour with the side strip.
        Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(6.dp))) {
            when (theme) {
                DeckTheme.ORIGINAL -> {
                    Box(Modifier.weight(0.72f).fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF5B92D0), Color(0xFF2F6FB5), Color(0xFF214F86)))))
                    Box(Modifier.weight(0.28f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFB7BABF), Color(0xFFE4E5E7), Color(0xFFB0B3B8)))))
                }
                DeckTheme.SPORT_YELLOW -> {
                    Box(Modifier.weight(0.79f).fillMaxSize().background(Brush.verticalGradient(listOf(SportYellow.YellowHi, SportYellow.Yellow, SportYellow.YellowLo))), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxSize(0.62f).clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)).background(SportYellow.Grey[1]))
                    }
                    Box(Modifier.weight(0.21f).fillMaxSize().background(SportYellow.YellowLo))
                }
                DeckTheme.RED_CHROME -> {
                    Box(Modifier.weight(0.72f).fillMaxSize().background(Brush.verticalGradient(listOf(RedChrome.RedHi, RedChrome.Red, RedChrome.RedLo))), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize(0.6f).clip(RoundedCornerShape(3.dp)).background(RedChrome.Glass))
                    }
                    Box(Modifier.weight(0.28f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFF7F8F9), RedChrome.Red, RedChrome.RedLo))))
                }
                DeckTheme.SILVER_BLUE -> {
                    Box(Modifier.weight(0.72f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFB7BABF), Color(0xFFE4E5E7), Color(0xFFB0B3B8)))), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize(0.55f).clip(RoundedCornerShape(50)).background(SilverBlue.Blue))
                    }
                    Box(Modifier.weight(0.28f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF9EA2A9), Color(0xFFD7D9DD)))))
                }
                DeckTheme.CHERRY_RED -> {
                    Box(Modifier.weight(0.72f).fillMaxSize().background(Brush.verticalGradient(listOf(CherryRed.BodyHi, CherryRed.Body, CherryRed.BodyLo))))
                    Box(Modifier.weight(0.28f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF2E2D31), Color(0xFF3C3B40)))))
                }
                DeckTheme.BLACK_GOLD -> {
                    Box(Modifier.weight(0.72f).fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2A2D), Color(0xFF121214)))))
                    Box(Modifier.weight(0.28f).fillMaxSize().background(Brush.horizontalGradient(listOf(BlackGold.GoldLo, BlackGold.GoldHi, BlackGold.Gold))))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(theme.label.uppercase(), style = EngravedCaps.copy(color = if (selected) accent else Color(0xFFD9D2C8), fontSize = 10.sp, letterSpacing = 2.sp))
    }
}

/**
 * Coming back to Foldy by itself after the music app has started a tape. Newer Android versions only
 * let an app bring itself back to the front if the owner has allowed "Appear on top" for it, so this
 * row shows whether that is allowed and opens the phone's page for it.
 */
@Composable
private fun ComeBackRow(accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
    // Check again each time you come back from the phone's settings page.
    androidx.compose.runtime.DisposableEffect(owner) {
        val watch = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) allowed = android.provider.Settings.canDrawOverlays(context)
        }
        owner.lifecycle.addObserver(watch)
        onDispose { owner.lifecycle.removeObserver(watch) }
    }
    ToggleRow(
        "Come back here after a tape starts",
        if (allowed) "On: Foldy returns by itself once YT Music or Spotify starts the tape"
        else "Tap, find Foldy Streaman in the list and allow \"Appear on top\"",
        allowed, accent,
    ) {
        val page = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + context.packageName))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(page) }
    }
}

@Composable
private fun ToggleRow(title: String, note: String, on: Boolean, accent: Color, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Label)
            Text(note, style = Note)
        }
        // A little slide switch.
        Box(
            Modifier
                .size(46.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (on) accent.copy(alpha = 0.85f) else Color(0xFF3A3632)),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.padding(3.dp).size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF2EBDC)))
        }
    }
}

@Composable
private fun ActionRow(title: String, note: String, button: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Label)
            Text(note, style = Note)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            button,
            style = EngravedCaps.copy(color = accent, fontSize = 10.sp, letterSpacing = 2.sp),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
