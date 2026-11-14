// Foldy Streaman addition (not in the original Foldman): search album covers for a tape's J-card.
// Uses MusicBrainz (the open music database) to find albums and the Cover Art Archive for the
// pictures; both are free for apps to use. Only the words typed in the search are sent.
package dev.foldman.deck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One album found by a search. */
data class CoverHit(val id: String, val title: String, val artist: String, val year: String) {
    val thumbUrl: String get() = "https://coverartarchive.org/release-group/$id/front-250"
    val fullUrl: String get() = "https://coverartarchive.org/release-group/$id/front-500"
}

object CoverSearch {
    private const val AGENT = "FoldyStreaman/1.0 (Android cassette player app)"
    private val thumbs = android.util.LruCache<String, Bitmap>(80)

    /** Albums matching [query] (an album, an artist, or both), best matches first. */
    suspend fun search(query: String): List<CoverHit> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val text = fetch("https://musicbrainz.org/ws/2/release-group/?query=$q&fmt=json&limit=30").toString(Charsets.UTF_8)
        val groups = JSONObject(text).optJSONArray("release-groups") ?: return@withContext emptyList()
        List(groups.length()) { i ->
            val g = groups.getJSONObject(i)
            val artists = g.optJSONArray("artist-credit")
            val artist = if (artists == null) "" else List(artists.length()) { a ->
                val c = artists.getJSONObject(a)
                c.optString("name") + c.optString("joinphrase")
            }.joinToString("")
            CoverHit(g.getString("id"), g.optString("title"), artist.trim(), g.optString("first-release-date").take(4))
        }
    }

    /** A small picture of a cover for the results grid, or null if that album has none. */
    suspend fun thumbnail(hit: CoverHit): Bitmap? = thumbs.get(hit.id) ?: withContext(Dispatchers.IO) {
        runCatching { fetch(hit.thumbUrl).let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull()
    }?.also { thumbs.put(hit.id, it) }

    /** Downloads a cover and keeps Foldy's own copy of it for tape [tapeId]. Returns its path. */
    suspend fun download(context: Context, tapeId: String, url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File.createTempFile("cover", ".img", context.cacheDir)
            try {
                tmp.writeBytes(fetch(url))
                TapePhotos.copyIn(context, tapeId, Uri.fromFile(tmp))
            } finally {
                tmp.delete()
            }
        }.getOrNull()
    }

    /** GETs [url], following the archive's redirects itself (always over https). */
    private fun fetch(url: String): ByteArray {
        var at = url
        repeat(6) {
            val c = URL(at.replaceFirst("http://", "https://")).openConnection() as HttpURLConnection
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = 8000
                c.readTimeout = 12000
                c.setRequestProperty("User-Agent", AGENT)
                c.setRequestProperty("Accept", "application/json, image/*")
                when (val code = c.responseCode) {
                    in 300..399 -> at = URL(URL(at), c.getHeaderField("Location") ?: error("Redirect without a target")).toString()
                    200 -> return c.inputStream.use { it.readBytes() }
                    else -> error("The cover service answered $code")
                }
            } finally {
                c.disconnect()
            }
        }
        error("Too many redirects")
    }
}
