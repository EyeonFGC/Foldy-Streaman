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
    /** Which app plays this tape's playlist. */
    val service: TapeService = TapeService.YT_MUSIC,
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
    /** Show the J-card picture on the cassette's label too, where the label colour goes. */
    val photoOnLabel: Boolean = false,
) {
    /** The link that opens and plays the playlist in its app. */
    val playUrl: String get() = when (service) {
        TapeService.YT_MUSIC -> "https://music.youtube.com/watch?list=$playlistId"
        TapeService.SPOTIFY -> "spotify:playlist:$playlistId"
    }

    /** A normal web link to the playlist (what the editor shows). */
    val shareUrl: String get() = when (service) {
        TapeService.YT_MUSIC -> "https://music.youtube.com/playlist?list=$playlistId"
        TapeService.SPOTIFY -> "https://open.spotify.com/playlist/$playlistId"
    }

    val source: Source get() = service.source
}

/** The streaming apps a tape can play from. */
enum class TapeService(val label: String, val short: String, val source: Source) {
    YT_MUSIC("YouTube Music", "YT", Source.YT_MUSIC),
    SPOTIFY("Spotify", "SP", Source.SPOTIFY),
}

/** A playlist found in a pasted link. */
data class PlaylistRef(val service: TapeService, val id: String)

/**
 * Reads a pasted link: YouTube / YouTube Music (list=…), Spotify (open.spotify.com/playlist/… or
 * spotify:playlist:…), or a bare YouTube playlist ID.
 */
fun parsePlaylist(text: String): PlaylistRef? {
    val t = text.trim()
    if (t.isEmpty()) return null
    Regex("spotify:playlist:([A-Za-z0-9]+)").find(t)?.let { return PlaylistRef(TapeService.SPOTIFY, it.groupValues[1]) }
    Regex("spotify\\.com/(?:[a-z-]+/)?playlist/([A-Za-z0-9]+)").find(t)?.let { return PlaylistRef(TapeService.SPOTIFY, it.groupValues[1]) }
    return playlistIdFrom(t)?.let { PlaylistRef(TapeService.YT_MUSIC, it) }
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
                service = runCatching { TapeService.valueOf(o.optString("service")) }.getOrDefault(TapeService.YT_MUSIC),
                shell = o.optLong("shell", TapeColors.shells[1]),
                label = o.optLong("label", TapeColors.labels[0]),
                font = runCatching { TapeFont.valueOf(o.optString("font")) }.getOrDefault(TapeFont.CLASSIC),
                photoPath = o.optString("photo").takeIf { it.isNotBlank() },
                photoX = o.optDouble("photoX", 0.0).toFloat(),
                photoY = o.optDouble("photoY", 0.0).toFloat(),
                photoZoom = o.optDouble("photoZoom", 1.0).toFloat(),
                photoOnLabel = o.optBoolean("photoOnLabel", false),
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
                    .put("service", t.service.name)
                    .put("shell", t.shell)
                    .put("label", t.label)
                    .put("font", t.font.name)
                    .put("photo", t.photoPath ?: "")
                    .put("photoX", t.photoX.toDouble())
                    .put("photoY", t.photoY.toDouble())
                    .put("photoZoom", t.photoZoom.toDouble())
                    .put("photoOnLabel", t.photoOnLabel),
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private companion object {
        const val KEY = "custom_tapes"
    }
}
