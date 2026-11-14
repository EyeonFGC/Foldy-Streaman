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
    val setFrontShelf: (Boolean) -> Unit = {},
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
            }
            Section("FEEL") {
                ToggleRow("Click sounds", "Keys, tapes going in and out", ui.keySounds, accent) { actions.setKeySounds(!ui.keySounds) }
                ToggleRow("Haptics", "A buzz with each click", ui.haptics, accent) { actions.setHaptics(!ui.haptics) }
                ToggleRow(
                    "Tape shelf on the front screen",
                    "A TAPES button on the player, to change tapes without opening it",
                    ui.frontShelf, accent,
                ) { actions.setFrontShelf(!ui.frontShelf) }
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
