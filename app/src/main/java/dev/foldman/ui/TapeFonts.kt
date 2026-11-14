// Foldy Streaman addition (not in the original Foldman): the fonts you can write a tape in.
package dev.foldman.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.foldman.R
import dev.foldman.deck.TapeFont

// Free fonts from Google Fonts (licences in assets/font-licenses).
private val TypeWorn = FontFamily(Font(R.font.type_worn))
private val TypeClean = FontFamily(Font(R.font.type_clean))
private val Pen = FontFamily(Font(R.font.hand_pen))
private val Scribble = FontFamily(Font(R.font.hand_scribble))
private val Marker = FontFamily(Font(R.font.hand_marker))

fun TapeFont.family(): FontFamily = when (this) {
    TapeFont.CLASSIC -> FontFamily.Cursive
    TapeFont.TYPE_WORN -> TypeWorn
    TapeFont.TYPE_CLEAN -> TypeClean
    TapeFont.PEN -> Pen
    TapeFont.SCRIBBLE -> Scribble
    TapeFont.MARKER -> Marker
}

/** Each font looks a different size at the same setting; this evens them out. */
fun TapeFont.sizeScale(): Float = when (this) {
    TapeFont.CLASSIC -> 1f
    TapeFont.TYPE_WORN -> 0.9f
    TapeFont.TYPE_CLEAN -> 0.88f
    TapeFont.PEN -> 1.15f
    TapeFont.SCRIBBLE -> 1.0f
    TapeFont.MARKER -> 0.9f
}

/** The classic pen looks best bold; the others are drawn as designed. */
fun TapeFont.weight(): FontWeight = if (this == TapeFont.CLASSIC) FontWeight.Bold else FontWeight.Normal
