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
    /** The writing on the label, spine and J-card. */
    val font: TapeFont = TapeFont.CLASSIC,
    /** Our own copy of the J-card picture (in the app's files), or null. */
    val photoPath: String? = null,
    /** Where the picture sits in its frame: -1..1 across and down, and how far it's zoomed in (1 = just fills). */
    val photoX: Float = 0f,
    val photoY: Float = 0f,
    val photoZoom: Float = 1f,
) {
    val playUrl: String get() = "https://music.youtube.com/watch?list=$playlistId"
}

/** Fonts for the label, spine and J-card. */
enum class TapeFont(val label: String) {
    CLASSIC("Classic pen"),
    TYPE_WORN("Old typewriter"),
    TYPE_CLEAN("Typewriter"),
    PEN("Neat handwriting"),
    SCRIBBLE("Scribble"),
    MARKER("Marker"),
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
                font = runCatching { TapeFont.valueOf(o.optString("font")) }.getOrDefault(TapeFont.CLASSIC),
                photoPath = o.optString("photo").takeIf { it.isNotBlank() },
                photoX = o.optDouble("photoX", 0.0).toFloat(),
                photoY = o.optDouble("photoY", 0.0).toFloat(),
                photoZoom = o.optDouble("photoZoom", 1.0).toFloat(),
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
                    .put("label", t.label)
                    .put("font", t.font.name)
                    .put("photo", t.photoPath ?: "")
                    .put("photoX", t.photoX.toDouble())
                    .put("photoY", t.photoY.toDouble())
                    .put("photoZoom", t.photoZoom.toDouble()),
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private companion object {
        const val KEY = "custom_tapes"
    }
}
