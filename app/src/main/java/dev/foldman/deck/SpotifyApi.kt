// Foldy Streaman addition (not in the original Foldman): "Log in with Spotify". With it, Foldy can
// list the account's playlists and tell Spotify to play one without switching to the Spotify app
// (Spotify's Web API). The login is Spotify's own page in the browser; Foldy never sees the
// password, only keys Spotify hands back, which stay in the app's private storage on this phone.
package dev.foldman.deck

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/** One playlist from the logged-in Spotify account. */
data class SpotifyPlaylist(val id: String, val name: String, val imageUrl: String?, val tracks: Int)

object SpotifyApi {
    /** Where Spotify sends the browser back to after the login. Registered with the Spotify app. */
    const val REDIRECT = "foldystreaman://spotify-login"
    private const val SCOPES = "playlist-read-private playlist-read-collaborative user-read-playback-state user-modify-playback-state"
    private const val K_CLIENT = "spotify_client_id"
    private const val K_VERIFIER = "spotify_verifier"
    private const val K_ACCESS = "spotify_access"
    private const val K_REFRESH = "spotify_refresh"
    private const val K_EXPIRES = "spotify_expires"
    private const val K_NAME = "spotify_name"

    private fun prefs(c: Context) = c.getSharedPreferences("foldman", Context.MODE_PRIVATE)

    /** The Spotify app's Client ID (not a secret), built into the app. (A typed one is only used by builds without it.) */
    fun clientId(c: Context): String = dev.foldman.BuildConfig.SPOTIFY_CLIENT_ID.ifBlank { prefs(c).getString(K_CLIENT, null).orEmpty() }

    fun setClientId(c: Context, id: String) = prefs(c).edit().putString(K_CLIENT, id.trim()).apply()

    fun signedIn(c: Context): Boolean = prefs(c).getString(K_REFRESH, null) != null

    /** The account's display name, once logged in. */
    fun name(c: Context): String? = if (signedIn(c)) prefs(c).getString(K_NAME, null) ?: "Spotify" else null

    fun logOut(c: Context) = prefs(c).edit().remove(K_ACCESS).remove(K_REFRESH).remove(K_EXPIRES).remove(K_NAME).apply()

    /** Spotify's login page for this app. A fresh one-time code ties the answer back to this phone. */
    fun loginPage(c: Context): Uri {
        val raw = ByteArray(48).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        prefs(c).edit().putString(K_VERIFIER, verifier).apply()
        val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId(c))
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", SCOPES)
            .build()
    }

    /** The browser came back from the login. Returns null when logged in, or what went wrong. */
    suspend fun finishLogin(c: Context, back: Uri): String? = withContext(Dispatchers.IO) {
        back.getQueryParameter("error")?.let { return@withContext "Spotify said: $it" }
        val code = back.getQueryParameter("code") ?: return@withContext "Spotify didn't send a login code."
        val verifier = prefs(c).getString(K_VERIFIER, null) ?: return@withContext "The login was started somewhere else. Try again."
        runCatching {
            val (status, body) = form(
                "https://accounts.spotify.com/api/token",
                mapOf("grant_type" to "authorization_code", "code" to code, "redirect_uri" to REDIRECT, "client_id" to clientId(c), "code_verifier" to verifier),
            )
            if (status != 200) error(JSONObject(body).optString("error_description", "error $status"))
            saveTokens(c, JSONObject(body))
            val me = call(c, "GET", "https://api.spotify.com/v1/me")
            if (me.first == 200) prefs(c).edit().putString(K_NAME, JSONObject(me.second).optString("display_name").ifBlank { "Spotify" }).apply()
        }.exceptionOrNull()?.let { "Couldn't finish the Spotify login: ${it.message}" }
    }

    /** The account's playlists (its own and the ones it follows), up to 200. */
    suspend fun playlists(c: Context): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        val out = ArrayList<SpotifyPlaylist>()
        var url: String? = "https://api.spotify.com/v1/me/playlists?limit=50"
        while (url != null && out.size < 200) {
            val (status, body) = call(c, "GET", url)
            if (status != 200) error("Spotify answered $status")
            val page = JSONObject(body)
            val items = page.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val p = items.optJSONObject(i) ?: continue
                out += SpotifyPlaylist(
                    id = p.optString("id"),
                    name = p.optString("name"),
                    imageUrl = p.optJSONArray("images")?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() },
                    tracks = (p.optJSONObject("tracks") ?: p.optJSONObject("items"))?.optInt("total") ?: 0,
                )
            }
            url = page.optString("next").takeIf { it.startsWith("https://") }
        }
        out.filter { it.id.isNotBlank() }
    }

    /** Something found by [search]: a playlist or an album, and who it is by. */
    class Found(val id: String, val name: String, val by: String, val imageUrl: String?, val tracks: Int, val album: Boolean)

    /** Searches all of Spotify for playlists and albums matching [query] (an artist's name finds their albums). */
    suspend fun search(c: Context, query: String): List<Found> = withContext(Dispatchers.IO) {
        val (status, body) = call(c, "GET", "https://api.spotify.com/v1/search?type=album,playlist&limit=10&q=" + URLEncoder.encode(query.trim(), "UTF-8"))
        if (status != 200) error(reason(status, body))
        val o = JSONObject(body)
        fun image(p: JSONObject) = p.optJSONArray("images")?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
        val out = ArrayList<Found>()
        o.optJSONObject("albums")?.optJSONArray("items")?.let { items ->
            for (i in 0 until items.length()) {
                val p = items.optJSONObject(i) ?: continue
                val artists = p.optJSONArray("artists")
                val by = (0 until (artists?.length() ?: 0)).mapNotNull { artists?.optJSONObject(it)?.optString("name") }.joinToString(", ")
                out += Found(p.optString("id"), p.optString("name"), by, image(p), p.optInt("total_tracks"), album = true)
            }
        }
        o.optJSONObject("playlists")?.optJSONArray("items")?.let { items ->
            for (i in 0 until items.length()) {
                val p = items.optJSONObject(i) ?: continue
                out += Found(
                    p.optString("id"), p.optString("name"), p.optJSONObject("owner")?.optString("display_name").orEmpty(), image(p),
                    (p.optJSONObject("tracks") ?: p.optJSONObject("items"))?.optInt("total") ?: 0, album = false,
                )
            }
        }
        out.filter { it.id.isNotBlank() }
    }

    /** How asking Spotify to play went. [noDevice] = Spotify on this phone wasn't awake to hear it. */
    class PlayResult(val ok: Boolean, val noDevice: Boolean = false, val message: String = "")

    /** Tells Spotify to play a playlist from the top on this phone. */
    suspend fun play(c: Context, playlistId: String, album: Boolean = false): PlayResult = withContext(Dispatchers.IO) {
        runCatching {
            val (dStatus, dBody) = call(c, "GET", "https://api.spotify.com/v1/me/player/devices")
            if (dStatus != 200) return@runCatching PlayResult(false, message = "Spotify wouldn't list its devices (${reason(dStatus, dBody)})")
            val all = JSONObject(dBody).optJSONArray("devices").let { l -> (0 until (l?.length() ?: 0)).mapNotNull { l?.optJSONObject(it) } }
            val device = thisPhone(c, all)
                ?: return@runCatching PlayResult(
                    false, noDevice = true,
                    message = if (all.isEmpty()) "Spotify isn't awake on this phone" else "Spotify is only awake on: " + all.joinToString { it.optString("name") },
                )
            val body = JSONObject().put("context_uri", "spotify:${if (album) "album" else "playlist"}:$playlistId").put("offset", JSONObject().put("position", 0)).put("position_ms", 0)
            val (status, text) = call(c, "PUT", "https://api.spotify.com/v1/me/player/play?device_id=" + URLEncoder.encode(device, "UTF-8"), body.toString())
            when {
                status in 200..299 -> PlayResult(true)
                status == 403 && text.contains("PREMIUM", true) -> PlayResult(false, message = "Spotify only lets Premium accounts be started from another app")
                // Spotify lists the phone but has let it go to sleep.
                status == 404 -> PlayResult(false, noDevice = true, message = "Spotify isn't awake on this phone")
                else -> PlayResult(false, message = "Spotify wouldn't start it (${reason(status, text)})")
            }
        }.getOrElse { PlayResult(false, message = "Couldn't reach Spotify (${it.message ?: "no internet?"})") }
    }

    /** Spotify's own words for why it said no. */
    private fun reason(status: Int, body: String): String =
        runCatching { JSONObject(body).getJSONObject("error").let { e -> e.optString("message").ifBlank { e.optString("reason") } } }.getOrNull()?.takeIf { it.isNotBlank() } ?: "error $status"

    /** This phone among the places Spotify can play right now. */
    private fun thisPhone(c: Context, all: List<JSONObject>): String? {
        val usable = all.filter { !it.optBoolean("is_restricted") && it.optString("id").isNotBlank() }
        val names = listOfNotNull(
            runCatching { Settings.Global.getString(c.contentResolver, "device_name") }.getOrNull(),
            runCatching { Settings.Secure.getString(c.contentResolver, "bluetooth_name") }.getOrNull(),
            Build.MODEL,
        ).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        // By name first (a folding phone can show up as a tablet), then the only phone or tablet.
        val handhelds = usable.filter { it.optString("type").lowercase() in setOf("smartphone", "tablet") }
        val mine = usable.firstOrNull { it.optString("name").trim().lowercase() in names }
            ?: handhelds.singleOrNull()
            ?: handhelds.firstOrNull { it.optBoolean("is_active") }
        return mine?.optString("id")
    }

    private fun saveTokens(c: Context, o: JSONObject) {
        val e = prefs(c).edit().putString(K_ACCESS, o.getString("access_token"))
            .putLong(K_EXPIRES, System.currentTimeMillis() + (o.optLong("expires_in", 3600) - 60) * 1000)
        o.optString("refresh_token").takeIf { it.isNotBlank() }?.let { e.putString(K_REFRESH, it) }
        e.remove(K_VERIFIER).apply()
    }

    /** A working key for the Web API, renewed when the old one has run out. */
    private fun token(c: Context, forceNew: Boolean = false): String {
        val p = prefs(c)
        val have = p.getString(K_ACCESS, null)
        if (!forceNew && have != null && System.currentTimeMillis() < p.getLong(K_EXPIRES, 0)) return have
        val refresh = p.getString(K_REFRESH, null) ?: error("Not logged in to Spotify")
        val (status, body) = form(
            "https://accounts.spotify.com/api/token",
            mapOf("grant_type" to "refresh_token", "refresh_token" to refresh, "client_id" to clientId(c)),
        )
        if (status == 400 || status == 401) {
            // Spotify has withdrawn the login (password changed, access removed): log out cleanly.
            logOut(c)
            error("The Spotify login has run out. Log in again in Settings.")
        }
        if (status != 200) error("Spotify answered $status")
        saveTokens(c, JSONObject(body))
        return p.getString(K_ACCESS, null)!!
    }

    private fun call(c: Context, method: String, url: String, json: String? = null): Pair<Int, String> {
        val first = http(method, url, token(c), json)
        return if (first.first == 401) http(method, url, token(c, forceNew = true), json) else first
    }

    private fun http(method: String, url: String, bearer: String?, json: String?): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            bearer?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            if (json != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(json.toByteArray()) }
            } else if (method == "PUT" || method == "POST") {
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(0)
                conn.outputStream.close()
            }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            return status to text
        } finally {
            conn.disconnect()
        }
    }

    private fun form(url: String, fields: Map<String, String>): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { o -> o.write(fields.entries.joinToString("&") { URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }.toByteArray()) }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            return status to text
        } finally {
            conn.disconnect()
        }
    }
}
