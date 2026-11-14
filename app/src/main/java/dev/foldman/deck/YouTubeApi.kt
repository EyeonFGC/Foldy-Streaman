// Foldy Streaman addition (not in the original Foldman): "Log in with Google" so Foldy can list the
// account's YouTube Music playlists (they are ordinary YouTube playlists underneath) to make tapes
// from. Read-only: Foldy asks only to see the account's YouTube playlists. The login is Google's
// own screen; Foldy never sees the password, and keeps nothing but "is logged in".
package dev.foldman.deck

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/** One playlist from the logged-in YouTube account. */
data class YouTubePlaylist(val id: String, val name: String, val imageUrl: String?, val tracks: Int)

object YouTubeApi {
    private const val SCOPE = "https://www.googleapis.com/auth/youtube.readonly"

    /** What Foldy asks Google for: to see the account's YouTube playlists, nothing more. */
    fun request(): AuthorizationRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    /** A working key from Google without showing anything, or null if it needs a login on screen. */
    private suspend fun token(c: Context): String? = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(c).authorize(request())
            .addOnSuccessListener { r -> if (cont.isActive) cont.resume(if (r.hasResolution()) null else r.accessToken) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

    /** The account's own playlists (up to 200), with "Liked music" first. */
    suspend fun playlists(c: Context): List<YouTubePlaylist> {
        val key = token(c) ?: error("Log in with Google again in Settings")
        return withContext(Dispatchers.IO) {
            val out = arrayListOf(YouTubePlaylist("LM", "Liked music", null, 0))
            var page: String? = null
            do {
                val url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&mine=true&maxResults=50" + (page?.let { "&pageToken=$it" } ?: "")
                val conn = URL(url).openConnection() as HttpURLConnection
                val body = try {
                    conn.connectTimeout = 8000
                    conn.readTimeout = 12000
                    conn.setRequestProperty("Authorization", "Bearer $key")
                    val status = conn.responseCode
                    val text = (if (status in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                    if (status !in 200..299) {
                        error(runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull()?.takeIf { it.isNotBlank() } ?: "YouTube answered $status")
                    }
                    JSONObject(text)
                } finally {
                    conn.disconnect()
                }
                val items = body.optJSONArray("items")
                for (i in 0 until (items?.length() ?: 0)) {
                    val p = items!!.optJSONObject(i) ?: continue
                    val s = p.optJSONObject("snippet")
                    val thumbs = s?.optJSONObject("thumbnails")
                    val thumb = listOf("medium", "high", "default").firstNotNullOfOrNull { thumbs?.optJSONObject(it)?.optString("url")?.takeIf { u -> u.isNotBlank() } }
                    out += YouTubePlaylist(p.optString("id"), s?.optString("title").orEmpty(), thumb, p.optJSONObject("contentDetails")?.optInt("itemCount") ?: 0)
                }
                page = body.optString("nextPageToken").takeIf { it.isNotBlank() }
            } while (page != null && out.size < 200)
            out.filter { it.id.isNotBlank() }
        }
    }
}
