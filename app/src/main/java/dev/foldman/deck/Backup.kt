// Foldy Streaman addition (not in the original Foldman): back up your tapes (pictures included)
// and settings to one file, and load them back after reinstalling.
package dev.foldman.deck

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TapeBackup {
    private const val FORMAT = "foldy-streaman-backup"

    /** Writes every tape (each picture packed inside) and [settings] to [uri]. */
    fun write(context: Context, uri: Uri, tapes: List<CustomTape>, settings: JSONObject) {
        val arr = JSONArray()
        tapes.forEach { t ->
            val o = t.toJson().put("photo", "")
            t.photoPath?.let { p ->
                val f = File(p)
                if (f.exists()) o.put("photoData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            }
            arr.put(o)
        }
        val root = JSONObject().put("format", FORMAT).put("version", 1).put("tapes", arr).put("settings", settings)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString().toByteArray()) }
            ?: error("Couldn't open that file to save into.")
    }

    /** Reads a backup: its tapes (pictures saved back into the app) and its settings. */
    fun read(context: Context, uri: Uri): Pair<List<CustomTape>, JSONObject?> {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Couldn't open that file.")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "That isn't a Foldy Streaman backup file." }
        val arr = root.getJSONArray("tapes")
        val dir = File(context.filesDir, "tape_photos").apply { mkdirs() }
        val tapes = List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val t = tapeFromJson(o)
            val data = o.optString("photoData")
            if (data.isNotBlank()) {
                val f = File(dir, "${t.id}-${System.currentTimeMillis()}-$i.jpg")
                f.writeBytes(Base64.decode(data, Base64.NO_WRAP))
                t.copy(photoPath = f.absolutePath)
            } else {
                t.copy(photoPath = null)
            }
        }
        return tapes to root.optJSONObject("settings")
    }
}
