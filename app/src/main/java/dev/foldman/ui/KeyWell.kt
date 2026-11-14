// Foldy Streaman addition (not in the original Foldman): every theme's keys sit raised in a dark
// well in the casing and sink into it when pressed (or latched), like the keys on a real player.
package dev.foldman.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Where a key's cap sits in its well: [top] and [faceH] of the face, and the [lip] showing under it. */
internal class KeyBox(val top: Float, val faceH: Float, val lip: Float)

/**
 * [depth] 0 = raised: the cap sits at the top of the well with its front edge below it and the
 * dark of the well showing underneath. 1 = pressed: the cap has slid down into that dark, its
 * bottom tucked under the well's rim, and the dark shows above it instead.
 */
internal fun DrawScope.keyBox(depth: Float): KeyBox {
    val h = size.height
    val gap = h * 0.2f
    val lip = h * 0.1f
    return KeyBox(top = (gap + lip + h * 0.03f) * depth, faceH = h - gap - lip, lip = lip)
}

/** The dark well in the casing, with a shadow under its top edge. */
internal fun DrawScope.drawKeyWell(corner: CornerRadius) {
    drawRoundRect(Color(0xFF121214), topLeft = Offset(-3f, -3f), size = Size(size.width + 6f, size.height + 6f), cornerRadius = corner)
}

/**
 * Draws the well, then [block] (the key) clipped to it so a pressed key tucks under the rim, then
 * the shadow the rim casts across the top of a pressed key.
 */
internal inline fun DrawScope.keyWell(depth: Float, corner: CornerRadius, block: DrawScope.() -> Unit) {
    drawKeyWell(corner)
    val well = Path().apply { addRoundRect(RoundRect(-3f, -3f, size.width + 3f, size.height + 3f, corner)) }
    clipPath(well) {
        block()
        if (depth > 0.01f) {
            val box = keyBox(depth)
            val reach = box.faceH * 0.38f
            drawRect(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = (0.55f * depth).coerceIn(0f, 1f)), Color.Transparent), box.top, box.top + reach),
                topLeft = Offset(0f, box.top), size = Size(size.width, reach),
            )
        }
    }
}

/** The front edge of the cap, below its face. */
internal fun DrawScope.drawKeyLip(box: KeyBox, corner: CornerRadius, colors: List<Color>) {
    val y = box.top + box.faceH - corner.y
    drawRoundRect(Brush.verticalGradient(colors, y, y + box.lip + corner.y), topLeft = Offset(0f, y), size = Size(size.width, box.lip + corner.y), cornerRadius = corner)
}
