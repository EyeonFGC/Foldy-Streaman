// Foldy Streaman addition (not in the original Foldman): J-card pictures kept inside the app.
package dev.foldman.deck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File

object TapePhotos {
    private const val MAX_SIDE = 1600

    /** Copies [uri] into the app's own files, scaled so its longest side is at most [MAX_SIDE]. */
    fun copyIn(context: Context, tapeId: String, uri: Uri): String? = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(w, h))
            decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val dir = File(context.filesDir, "tape_photos").apply { mkdirs() }
        val file = File(dir, "$tapeId-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        file.absolutePath
    }.getOrNull()

    /** Loads a saved picture for drawing, or null if it's gone. */
    fun load(path: String): Bitmap? = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
}
