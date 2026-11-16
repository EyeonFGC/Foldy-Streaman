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

    /** A title boiled down for comparing: no "Artist - " in front, no (extras), no punctuation. */
    private fun clean(s: String): String {
        val title = s.substringAfter(" - ", s)
        return title.lowercase().replace(Regex("\\(.*?\\)|\\[.*?]"), "").replace(Regex("feat\\..*|ft\\..*"), "").replace(Regex("[^a-z0-9]"), "")
    }

    /** Whether two song titles are the same song (whole titles only: "You" is not "Take You Home"). */
    fun sameSong(a: String?, b: String?): Boolean {
        val x = clean(a ?: return false)
        return x.isNotEmpty() && x == clean(b ?: return false)
    }

    /**
     * Which of [titles] is [now]: an exact match if there is one, otherwise the closest title that
     * contains it (or is contained in it) and is nearly the same length. -1 when none fits.
     */
    fun bestMatch(titles: List<String>, now: String?): Int {
        val n = clean(now ?: return -1)
        if (n.isEmpty()) return -1
        titles.indexOfFirst { clean(it) == n }.let { if (it >= 0) return it }
        var best = -1
        var bestRatio = 0.7f
        titles.forEachIndexed { i, t ->
            val c = clean(t)
            if (c.isEmpty() || !(c.contains(n) || n.contains(c))) return@forEachIndexed
            val ratio = minOf(c.length, n.length).toFloat() / maxOf(c.length, n.length)
            if (ratio > bestRatio) { bestRatio = ratio; best = i }
        }
        return best
    }

    internal suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    internal fun json(text: String) = JSONObject(text)
}
