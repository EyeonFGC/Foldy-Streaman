// Foldy Streaman addition (not in the original Foldman): the tape-head mechanism inside the open
// deck, along the tape's open edge, like the grey plate you see when you open a real player: two
// pinch rollers in their wells, the capstans, the play and erase heads, guides, screws, and
// CASSETTE PLAYER stamped into the metal.
package dev.foldman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/** How deep the head plate is, as a share of the tape's short side. */
const val HeadShare = 0.22f

private val PlateHi = Color(0xFF9AA2A6)
private val Plate = Color(0xFF7E878B)
private val PlateLo = Color(0xFF5F676B)
private val Well = Color(0xFF1A1C1D)

/**
 * The head plate, drawn lying along the bottom of its box: its narrow middle part towards the tape
 * (the top) and its wide band along the outer edge. [facingLeft] turns it a quarter turn so the
 * tape side faces left (a tape stood on end).
 */
@Composable
fun HeadAssembly(modifier: Modifier = Modifier, facingLeft: Boolean = false) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.quarterTurn(facingLeft)) {
        val w = size.width
        val h = size.height
        // The plate is turned end for end (its wide band away from the tape); the lettering is put
        // on afterwards so it still reads the right way up.
        rotate(180f, pivot = center) {
        // The plate's outline: a full-width band along the tape's edge, then a narrower middle part
        // with slanted sides, like the real one.
        val r = h * 0.07f
        val plate = cornered(
            listOf(
                Offset(w * 0.02f, 0f) to r, Offset(w * 0.98f, 0f) to r,
                Offset(w * 0.98f, h * 0.36f) to r, Offset(w * 0.86f, h * 0.42f) to r * 1.6f,
                Offset(w * 0.82f, h * 0.96f) to r * 0.6f, Offset(w * 0.2f, h * 0.96f) to r * 0.6f,
                Offset(w * 0.17f, h * 0.44f) to r * 1.6f, Offset(w * 0.02f, h * 0.38f) to r,
            ),
        )
        translate(w * 0.004f, h * 0.05f) { drawPath(plate, Color.Black.copy(alpha = 0.45f)) }
        drawPath(plate, Brush.verticalGradient(listOf(PlateHi, Plate, PlateLo)))
        clipPath(plate) {
            // Fine brushing across the metal.
            var y = h * 0.02f
            while (y < h) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(w, y), 1f)
                y += h * 0.035f
            }
        }
        drawPath(plate, Color.White.copy(alpha = 0.22f), style = Stroke(h * 0.012f))
        // Panel lines pressed into the plate.
        val line = Color.Black.copy(alpha = 0.28f)
        val hi = Color.White.copy(alpha = 0.18f)
        fun groove(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(line, Offset(x1, y1), Offset(x2, y2), h * 0.012f)
            drawLine(hi, Offset(x1 + 1f, y1 + 1.2f), Offset(x2 + 1f, y2 + 1.2f), h * 0.008f)
        }
        groove(w * 0.24f, h * 0.52f, w * 0.79f, h * 0.52f)
        groove(w * 0.24f, h * 0.52f, w * 0.25f, h * 0.88f)
        groove(w * 0.79f, h * 0.52f, w * 0.78f, h * 0.88f)
        groove(w * 0.38f, h * 0.05f, w * 0.38f, h * 0.42f)
        groove(w * 0.62f, h * 0.05f, w * 0.62f, h * 0.42f)
        // Pinch rollers, each in a square well, with a capstan pin beside it.
        // Set in from the plate's edges, with metal all round each well.
        for (x in listOf(w * 0.24f, w * 0.76f)) pinchRoller(Offset(x, h * 0.4f), h)
        for (x in listOf(w * 0.305f, w * 0.695f)) capstan(Offset(x, h * 0.2f), h)
        // Tape guides either side of the heads.
        for (x in listOf(w * 0.415f, w * 0.585f)) {
            drawRoundRect(Well, Offset(x - h * 0.05f, h * 0.02f), Size(h * 0.1f, h * 0.3f), CornerRadius(h * 0.02f))
            drawRoundRect(Brush.horizontalGradient(listOf(PlateHi, Color(0xFFD9DEE1), PlateLo), x - h * 0.035f, x + h * 0.035f), Offset(x - h * 0.035f, h * 0.04f), Size(h * 0.07f, h * 0.24f), CornerRadius(h * 0.015f))
        }
        // The play head in the middle (chrome, with its fine gap), and the erase head.
        playHead(Offset(w * 0.5f, h * 0.2f), h * 0.32f, h * 0.36f)
        playHead(Offset(w * 0.345f, h * 0.18f), h * 0.18f, h * 0.24f)
        // Screws.
        for (p in listOf(Offset(w * 0.06f, h * 0.22f), Offset(w * 0.94f, h * 0.22f), Offset(w * 0.215f, h * 0.7f), Offset(w * 0.8f, h * 0.7f))) screw(p, h * 0.055f)
        // Slots cut through along the bottom edge.
        for ((x, sw) in listOf(w * 0.42f to h * 0.12f, w * 0.48f to h * 0.12f, w * 0.565f to h * 0.2f)) {
            drawRoundRect(Well, Offset(x, h * 0.86f), Size(sw, h * 0.07f), CornerRadius(h * 0.02f))
        }
        }
        // CASSETTE PLAYER, stamped into the metal: a dark edge above, a light edge below.
        val style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = (h * 0.24f / density).sp, letterSpacing = (h * 0.012f / density).sp)
        val text = measurer.measure("CASSETTE PLAYER", style)
        val tl = Offset((w - text.size.width) / 2f, h * 0.37f - text.size.height / 2f)
        drawText(text, color = Color.Black.copy(alpha = 0.35f), topLeft = tl - Offset(0f, 1.2f))
        drawText(text, color = Color.White.copy(alpha = 0.35f), topLeft = tl + Offset(0f, 1.4f))
        drawText(text, color = Plate.copy(alpha = 0.9f), topLeft = tl)
    }
}

/** A closed outline through [corners], each rounded off by its own radius. */
private fun cornered(corners: List<Pair<Offset, Float>>): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
    val n = corners.size
    for (i in 0 until n) {
        val (v, rad) = corners[i]
        fun toward(o: Offset): Offset { val d = o - v; return v + d / d.getDistance() * rad }
        val a = toward(corners[(i + n - 1) % n].first)
        if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
        val b = toward(corners[(i + 1) % n].first)
        quadraticTo(v.x, v.y, b.x, b.y)
    }
    close()
}

private fun DrawScope.pinchRoller(c: Offset, h: Float) {
    val s = h * 0.4f
    drawRoundRect(Well, Offset(c.x - s / 2f, c.y - s / 2f), Size(s, s), CornerRadius(h * 0.05f))
    drawRoundRect(Color.Black.copy(alpha = 0.6f), Offset(c.x - s / 2f, c.y - s / 2f), Size(s, s), CornerRadius(h * 0.05f), style = Stroke(h * 0.015f))
    // The rubber roller: black, with a soft sheen across its top.
    val r = s * 0.36f
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3C3E), Color(0xFF0B0B0C)), c - Offset(r * 0.3f, r * 0.4f), r * 1.5f), r, c)
    drawArc(Color.White.copy(alpha = 0.22f), 200f, 110f, false, Offset(c.x - r * 0.75f, c.y - r * 0.75f), Size(r * 1.5f, r * 1.5f), style = Stroke(r * 0.12f))
    drawCircle(Color(0xFF8D9498), r * 0.2f, c)
}

private fun DrawScope.capstan(c: Offset, h: Float) {
    drawCircle(Well, h * 0.06f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFE6EAEC), Color(0xFF7B8286)), c - Offset(h * 0.01f, h * 0.01f), h * 0.04f), h * 0.032f, c)
}

private fun DrawScope.playHead(top: Offset, w: Float, h: Float) {
    val tl = Offset(top.x - w / 2f, top.y - h * 0.55f)
    drawRoundRect(Color.Black.copy(alpha = 0.45f), tl + Offset(0f, h * 0.06f), Size(w, h), CornerRadius(w * 0.18f))
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8E959A), Color(0xFFF2F4F5), Color(0xFFB3B9BD), Color(0xFF6E7579)), tl.x, tl.x + w), tl, Size(w, h), CornerRadius(w * 0.18f))
    drawLine(Color(0xFF1E2022), Offset(top.x, tl.y + h * 0.12f), Offset(top.x, tl.y + h * 0.88f), w * 0.05f)
}

private fun DrawScope.screw(c: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 1.15f, c + Offset(0f, r * 0.15f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFD5DADD), Color(0xFF6E7579)), c - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, c)
    drawLine(Color(0xFF3A3E41), Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), r * 0.22f)
    drawLine(Color(0xFF3A3E41), Offset(c.x, c.y - r * 0.6f), Offset(c.x, c.y + r * 0.6f), r * 0.22f)
}
