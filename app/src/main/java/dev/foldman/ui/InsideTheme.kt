// Foldy Streaman addition (not in the original Foldman): the inside of the open player follows the
// theme: the body round the rack and the deck is that theme's material, with lettering to match.
package dev.foldman.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.foldman.deck.DeckTheme

/** Colours for lettering printed on the body inside: names, small print, and highlights. */
internal class InsideLook(val label: Color, val muted: Color, val accent: Color)

internal fun insideLook(theme: DeckTheme): InsideLook = when (theme) {
    DeckTheme.ORIGINAL -> InsideLook(Color(0xFFF1F3F6), Color(0xFFC6D6EC), Color(0xFFFFC14A))
    DeckTheme.BLACK_GOLD -> InsideLook(Color(0xFFE9E7E2), Color(0xFF9C988F), BlackGold.Gold)
    DeckTheme.CHERRY_RED -> InsideLook(Color.White, Color(0xFFFFD9DF), CherryRed.Yellow)
    DeckTheme.RED_CHROME -> InsideLook(Color.White, Color(0xFFFFD0D4), Color(0xFF8CFFA0))
    DeckTheme.SPORT_YELLOW -> InsideLook(SportYellow.Ink, Color(0xFF5E4C00), SportYellow.Blue)
    DeckTheme.SILVER_BLUE -> InsideLook(SilverBlue.Ink, Color(0xFF4A5480), SilverBlue.Blue)
}

/** The body material behind the rack and the deck, the same as on the front. */
internal fun Modifier.insideBody(theme: DeckTheme): Modifier = when (theme) {
    DeckTheme.ORIGINAL -> paintedBody(Color(0xFF2F6FB5), corner = 0.dp, framed = false)
    DeckTheme.BLACK_GOLD -> matteBlack()
    DeckTheme.CHERRY_RED -> glossyRed(trim = 0.dp)
    DeckTheme.RED_CHROME -> metallicRed()
    DeckTheme.SPORT_YELLOW -> sportYellow()
    DeckTheme.SILVER_BLUE -> chassis(corner = 0.dp, screws = false)
}

/** The colour behind the front-screen tape drawer's rounded top, to match the theme's body. */
internal fun drawerColor(theme: DeckTheme): Color = when (theme) {
    DeckTheme.ORIGINAL -> Color(0xFF2F6FB5)
    DeckTheme.BLACK_GOLD -> Color(0xFF1C1C1F)
    DeckTheme.CHERRY_RED -> CherryRed.Body
    DeckTheme.RED_CHROME -> RedChrome.Red
    DeckTheme.SPORT_YELLOW -> SportYellow.Yellow
    DeckTheme.SILVER_BLUE -> Color(0xFFC3C5C9)
}
