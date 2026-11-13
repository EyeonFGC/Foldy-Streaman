// Foldy Streaman addition (not in the original Foldman): your own tapes, each one a YouTube Music playlist.
package dev.foldman.deck

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A tape you made: a title written on the label, shell and label colours, and the YouTube Music
 * playlist it plays. Colours are ARGB longs so they store as plain numbers.
 */
data class CustomTape(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val playlistId: String,
    val shell: Long,
    val label: Long,
) {
    val playUrl: String get() = "https://music.youtube.com/watch?list=$playlistId"
}

/** The colour choices offered in the tape editor. */
object TapeColors {
    val shells = listOf(
        0xFF3A3D44, 0xFFB8322A, 0xFFE9E5DF, 0xFF1F5FA8, 0xFF2E8B57,
        0xFFF2B33D, 0xFF7B4FB8, 0xFFE86A9A, 0xFF101010, 0x99D8DCE0,
    )
    val labels = listOf(
        0xFFF2EBDC, 0xFFFFFFFF, 0xFFFFE58A, 0xFFBFE3FF, 0xFFC9F2C7,
        0xFFFFC8D6, 0xFFE3D1FF, 0xFFFFB48A, 0xFF2A2522, 0xFFD9D9D9,
    )
}

/** Pulls the playlist ID out of a YouTube / YouTube Music link, or accepts a bare ID. */
fun playlistIdFrom(text: String): String? {
    val t = text.trim()
    if (t.isEmpty()) return null
    if (!t.contains('/') && !t.contains('=')) {
        return t.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
    }
    val withScheme = if (t.startsWith("http")) t else "https://$t"
    return runCatching { Uri.parse(withScheme).getQueryParameter("list") }.getOrNull()?.takeIf { it.isNotBlank() }
}

/** Keeps the tapes in the app's settings as JSON. */
class TapeStore(private val prefs: SharedPreferences) {
    fun load(): List<CustomTape> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CustomTape(
                id = o.getString("id"),
                title = o.optString("title"),
                playlistId = o.getString("playlist"),
                shell = o.optLong("shell", TapeColors.shells[1]),
                label = o.optLong("label", TapeColors.labels[0]),
            )
        }
    }.getOrDefault(emptyList())

    fun save(tapes: List<CustomTape>) {
        val arr = JSONArray()
        tapes.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("playlist", t.playlistId)
                    .put("shell", t.shell)
                    .put("label", t.label),
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private companion object {
        const val KEY = "custom_tapes"
    }
}
