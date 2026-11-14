// Foldy Streaman addition (not in the original Foldman): steel hinges down the fold of the open
// player, like the butt hinges on a box lid. They're only drawn; the phone's own fold does the rest.
package dev.foldman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A hinge's colours, following the theme: painted leaves, a polished barrel and screws. */
class HingeLook(val leaf: List<Color>, val barrel: List<Color>, val screw: List<Color>)

private val Chrome = listOf(Color(0xFF6E737A), Color(0xFFF4F5F7), Color(0xFFC3C7CC), Color(0xFF7D828A))
private val ChromeScrew = listOf(Color(0xFFF1F2F4), Color(0xFF8D9299))

fun hingeLook(theme: dev.foldman.deck.DeckTheme): HingeLook = when (theme) {
    dev.foldman.deck.DeckTheme.ORIGINAL -> HingeLook(listOf(Color(0xFF6A9FDA), Color(0xFF2F6FB5), Color(0xFF214F86)), Chrome, ChromeScrew)
    dev.foldman.deck.DeckTheme.BLACK_GOLD -> HingeLook(
        listOf(BlackGold.GoldHi, BlackGold.Gold, BlackGold.GoldLo),
        listOf(Color(0xFF8A6208), BlackGold.GoldHi, BlackGold.Gold, Color(0xFF8A6208)),
        listOf(Color(0xFFFFE08A), Color(0xFF9C7410)),
    )
    dev.foldman.deck.DeckTheme.CHERRY_RED -> HingeLook(listOf(CherryRed.BodyHi, CherryRed.Body, CherryRed.BodyLo), Chrome, ChromeScrew)
    dev.foldman.deck.DeckTheme.SILVER_BLUE -> HingeLook(
        listOf(Color(0xFFE9EBEE), Color(0xFFB9BDC3), Color(0xFFA2A7AE)),
        listOf(SilverBlue.BlueLo, SilverBlue.BlueHi, SilverBlue.Blue, SilverBlue.BlueLo),
        ChromeScrew,
    )
}

/** How wide the gap down the fold is: room for the hinges without touching either side. */
val HingeGap: Dp = 58.dp

/** Three hinges down the fold: near the top, in the middle, near the bottom. */
@Composable
fun FoldHinges(modifier: Modifier = Modifier, theme: dev.foldman.deck.DeckTheme = dev.foldman.deck.DeckTheme.ORIGINAL) {
    Column(
        modifier.width(HingeGap).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) { Hinge(Modifier.size(44.dp, 64.dp), theme) }
    }
}

/** One butt hinge seen flat: two steel leaves with countersunk screws, the knuckled barrel between. */
@Composable
fun Hinge(modifier: Modifier = Modifier, theme: dev.foldman.deck.DeckTheme = dev.foldman.deck.DeckTheme.ORIGINAL) {
    val look = hingeLook(theme)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val barrelL = w * 0.39f
        val barrelR = w * 0.61f
        val leafR = CornerRadius(w * 0.05f)
        // Soft shadow on the body underneath.
        drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(2f, 4f), size = Size(w, h), cornerRadius = leafR)
        // The two leaves: brushed steel, lit from the top left.
        val steel = Brush.linearGradient(look.leaf, Offset.Zero, Offset(w, h))
        drawRoundRect(steel, topLeft = Offset(0f, h * 0.04f), size = Size(barrelL + 2f, h * 0.92f), cornerRadius = leafR)
        drawRoundRect(steel, topLeft = Offset(barrelR - 2f, h * 0.04f), size = Size(w - barrelR + 2f, h * 0.92f), cornerRadius = leafR)
        for (i in 0 until 14) {
            val y = h * (0.07f + i * 0.064f)
            drawLine(Color.White.copy(alpha = 0.10f), Offset(1f, y), Offset(barrelL, y), 0.8f)
            drawLine(Color.White.copy(alpha = 0.10f), Offset(barrelR, y), Offset(w - 1f, y), 0.8f)
        }
        drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, h * 0.04f), size = Size(barrelL + 2f, h * 0.92f), cornerRadius = leafR, style = Stroke(1f))
        drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(barrelR - 2f, h * 0.04f), size = Size(w - barrelR + 2f, h * 0.92f), cornerRadius = leafR, style = Stroke(1f))
        // Two screws in each leaf.
        val sr = w * 0.085f
        for (y in listOf(h * 0.27f, h * 0.73f)) {
            drawScrew(Offset(barrelL * 0.48f, y), sr, look.screw)
            drawScrew(Offset(barrelR + (w - barrelR) * 0.52f, y), sr, look.screw)
        }
        // The barrel: a polished cylinder in three knuckles, with the pin's caps showing at the ends.
        val cyl = Brush.horizontalGradient(look.barrel, barrelL, barrelR)
        val bw = barrelR - barrelL
        drawRoundRect(cyl, topLeft = Offset(barrelL, 0f), size = Size(bw, h), cornerRadius = CornerRadius(bw / 2f))
        for (k in listOf(h / 3f, h * 2f / 3f)) {
            drawLine(Color.Black.copy(alpha = 0.45f), Offset(barrelL, k), Offset(barrelR, k), 1.4f)
            drawLine(Color.White.copy(alpha = 0.5f), Offset(barrelL, k + 1.4f), Offset(barrelR, k + 1.4f), 0.8f)
        }
        drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(barrelL, 0f), size = Size(bw, h), cornerRadius = CornerRadius(bw / 2f), style = Stroke(1f))
    }
}

/** A countersunk screw: a dished recess and a slotted head. */
private fun DrawScope.drawScrew(c: Offset, r: Float, head: List<Color>) {
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 1.4f, c)
    drawCircle(Brush.radialGradient(head, c - Offset(r * 0.3f, r * 0.3f), r * 1.3f), r, c)
    drawLine(Color(0xFF3A3D42), c - Offset(r * 0.75f, -r * 0.2f), c + Offset(r * 0.75f, -r * 0.2f), r * 0.32f)
    drawCircle(Color.Black.copy(alpha = 0.3f), r, c, style = Stroke(0.8f))
}

/**
 * One long piano hinge down the whole fold: narrow steel leaves with a row of screws, and the
 * barrel split into many short knuckles.
 */
@Composable
fun PianoHinge(modifier: Modifier = Modifier, theme: dev.foldman.deck.DeckTheme = dev.foldman.deck.DeckTheme.ORIGINAL) {
    val look = hingeLook(theme)
    androidx.compose.foundation.layout.Box(modifier.width(HingeGap).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(30.dp).fillMaxHeight(0.97f)) {
            val w = size.width
            val h = size.height
            val barrelL = w * 0.36f
            val barrelR = w * 0.64f
            val bw = barrelR - barrelL
            val r = CornerRadius(w * 0.08f)
            drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(2f, 4f), size = Size(w, h), cornerRadius = r)
            val steel = Brush.horizontalGradient(look.leaf)
            drawRoundRect(steel, topLeft = Offset(0f, 0f), size = Size(barrelL + 2f, h), cornerRadius = r)
            drawRoundRect(steel, topLeft = Offset(barrelR - 2f, 0f), size = Size(w - barrelR + 2f, h), cornerRadius = r)
            drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, 0f), size = Size(barrelL + 2f, h), cornerRadius = r, style = Stroke(1f))
            drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(barrelR - 2f, 0f), size = Size(w - barrelR + 2f, h), cornerRadius = r, style = Stroke(1f))
            // A screw every so often down each leaf, staggered side to side.
            val sr = w * 0.11f
            val step = w * 2.4f
            var y = step * 0.5f
            var left = true
            while (y < h - sr * 2) {
                drawScrew(Offset(if (left) barrelL * 0.48f else barrelR + (w - barrelR) * 0.52f, y), sr, look.screw)
                left = !left
                y += step / 2f
            }
            // The barrel, cut into many short knuckles.
            val cyl = Brush.horizontalGradient(look.barrel, barrelL, barrelR)
            drawRoundRect(cyl, topLeft = Offset(barrelL, 0f), size = Size(bw, h), cornerRadius = CornerRadius(bw / 2f))
            val knuckle = w * 0.9f
            var k = knuckle
            while (k < h - 2f) {
                drawLine(Color.Black.copy(alpha = 0.45f), Offset(barrelL, k), Offset(barrelR, k), 1.2f)
                drawLine(Color.White.copy(alpha = 0.45f), Offset(barrelL, k + 1.2f), Offset(barrelR, k + 1.2f), 0.7f)
                k += knuckle
            }
            drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(barrelL, 0f), size = Size(bw, h), cornerRadius = CornerRadius(bw / 2f), style = Stroke(1f))
        }
    }
}
