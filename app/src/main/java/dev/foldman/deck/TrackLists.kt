// Foldy Streaman addition (not in the original Foldman): a tape's track list, read from the account
// logged in for its service (YouTube or Spotify), or from the music app's own queue while the tape
// plays. Kept in memory for a while so opening a J-card again doesn't ask again.
package dev.foldman.deck

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One song on a tape. [id] is the YouTube video ID or the Spotify track ID; [uri] Spotify's track URI. */
data class TapeTrack(val id: String, val title: String, val artist: String, val uri: String? = null)

object TrackLists {
    private class Cached(val tracks: List<TapeTrack>, val at: Long)
    private val cache = HashMap<String, Cached>()
    private const val KEEP_MS = 20 * 60 * 1000L

    /** The songs on [tape], or an error saying why they can't be read (not logged in, and so on). */
    suspend fun load(c: Context, tape: CustomTape, youTubeIn: Boolean, spotifyIn: Boolean): List<TapeTrack> {
        val key = tape.service.name + ":" + tape.playlistId + ":" + tape.album
        cache[key]?.takeIf { System.currentTimeMillis() - it.at < KEEP_MS }?.let { return it.tracks }
        val tracks = when (tape.service) {
            TapeService.YT_MUSIC -> {
                if (!youTubeIn) error("Log in with Google (Settings > YouTube Music) to see this tape's songs")
                YouTubeApi.playlistItems(c, tape.playlistId)
            }
            TapeService.SPOTIFY -> {
                if (!spotifyIn) error("Log in to Spotify (Settings > Spotify) to see this tape's songs")
                SpotifyApi.tracks(c, tape.playlistId, tape.album)
            }
        }
        cache[key] = Cached(tracks, System.currentTimeMillis())
        return tracks
    }

    /** Whether two song titles are the same song (YouTube titles often carry the artist and extras). */
    fun sameSong(a: String?, b: String?): Boolean {
        fun clean(s: String) = s.lowercase().replace(Regex("\\(.*?\\)|\\[.*?]"), "").replace(Regex("[^a-z0-9]"), "")
        val x = clean(a ?: return false)
        val y = clean(b ?: return false)
        if (x.length < 3 || y.length < 3) return x == y && x.isNotEmpty()
        return x == y || x.contains(y) || y.contains(x)
    }

    internal suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    internal fun json(text: String) = JSONObject(text)
}
