package dev.foldman.deck

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import dev.foldman.BuildConfig
import dev.foldman.apple.AppleAuthExpired
import dev.foldman.apple.AppleKitBackend
import dev.foldman.apple.AppleMusicApi
import dev.foldman.apple.ApplePlaylist
import dev.foldman.apple.MusicKit
import dev.foldman.playback.LocalBackend
import dev.foldman.playback.LocalLibrary
import dev.foldman.remote.RemoteBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Cassette label colours, taken from the album art when there is some. */
data class LabelPalette(val paper: Color, val stripe: Color, val stripe2: Color, val ink: Color) {
    companion object {
        val Default = LabelPalette(Color(0xFFF2EBDC), Color(0xFFE8642C), Color(0xFFF2B33D), Color(0xFF2A2522))
    }
}

/** Foldy Streaman: whether the phone's notifications show on the front player, and how much of them. */
enum class NoticeMode(val label: String) {
    OFF("Off"),
    APP_ONLY("App name only"),
    FULL("Full message"),
}

/** Foldy Streaman: what runs down the fold of the open player. */
enum class HingeStyle(val label: String) {
    PIANO("One long hinge"),
    THREE("Three hinges"),
    NONE("None"),
}

/** Foldy Streaman: how the player looks. */
enum class DeckTheme(val label: String) {
    ORIGINAL("Original"),
    BLACK_GOLD("Black & Gold"),
    CHERRY_RED("Cherry Red"),
    SILVER_BLUE("Silver & Blue"),
    RED_CHROME("Red Chrome"),
    SPORT_YELLOW("Sport Yellow"),
    CRYSTAL_CLEAR("Crystal Clear"),
}

data class DeckUi(
    val source: Source = Source.LOCAL,
    val transport: Transport = Transport.STOPPED,
    val status: SourceStatus = SourceStatus.NeedsFolder,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: ImageBitmap? = null,
    val label: LabelPalette = LabelPalette.Default,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val trackIndex: Int = -1,
    val trackCount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val tracks: List<LocalTrack> = emptyList(),
    val folderName: String? = null,
    val volume: Float = 0.5f,
    val queue: List<QueueEntry> = emptyList(),
    val currentQueueId: Long = -1,
    val keySounds: Boolean = true,
    val haptics: Boolean = true,
    val apple: AppleUi = AppleUi(),
    /** Your own tapes on the rack (Foldy Streaman). */
    val tapes: List<CustomTape> = emptyList(),
    /** The tape in the deck, when it's one of yours rather than a plain source. */
    val tape: CustomTape? = null,
    /** The tape was just put in: PLAY starts its playlist from the top. */
    val tapeCued: Boolean = false,
    /** Nothing in the deck: the tape was swiped out. */
    val empty: Boolean = false,
    val theme: DeckTheme = DeckTheme.ORIGINAL,
    /** A phone that doesn't fold: EJECT opens the deck (landscape), CLOSE shuts it (portrait). */
    val phoneMode: Boolean = false,
    /** Phone mode only: the deck is open (rack and deck showing). */
    val deckOpen: Boolean = false,
    /** Settings: opening the Fold stops the music. */
    val stopOnOpen: Boolean = false,
    /** Front screen: the volume buttons work the player (play, pause, skip) instead of the volume. */
    val volumeKeys: Boolean = true,
    /** Phones that don't fold: the open deck stays upright (tape across the top, rack below) instead of turning sideways. */
    val phonePortrait: Boolean = false,
    /** Show the front player over the lock screen (Settings). */
    val lockScreen: Boolean = false,
    /** The folded front screen (the player) is what's showing, not the inside. */
    val coverShown: Boolean = false,
    /** Logged in with Google, for picking YouTube Music playlists. */
    val youTubeSignedIn: Boolean = false,
    /** The last thing the Google login had to say (an error, usually). */
    val youTubeMessage: String? = null,
    /** The logged-in Spotify account's name, or null when not logged in. */
    val spotifyName: String? = null,
    /** The Spotify app's Client ID as Foldy has it (blank = not set up yet). */
    val spotifyClientId: String = "",
    /** The last thing the Spotify login had to say (an error, usually). */
    val spotifyMessage: String? = null,
    /** Sound is going to headphones, a speaker or the car rather than this phone: a plug sits in the jack. */
    val audioElsewhere: Boolean = false,
    /** Settings: a TAPES button on the front screen opens a tape shelf there. */
    val frontShelf: Boolean = false,
    /** Settings: J-cards on dark paper. */
    val jcardDark: Boolean = false,
    /** Settings: adapters switched off, left off the rack. */
    val hiddenSources: Set<Source> = emptySet(),
    val hingeStyle: HingeStyle = HingeStyle.PIANO,
    val noticeMode: NoticeMode = NoticeMode.OFF,
    /** The last backup or restore, said in plain words (shown in Settings). */
    val backupMessage: String? = null,
) {
    /** How far through the tape we are: the left reel empties into the right one as this grows. */
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

sealed interface DeckEvent {
    /** The tape ran out while winding: the mechanism thunks back to stop. */
    data object TapeEnd : DeckEvent
    /** STOP pressed while already stopped: the cassette door pops. */
    data object Eject : DeckEvent
    /** A phone volume button pressed a key on the front screen: click and buzz like a tap. */
    data object KeyClick : DeckEvent
}

class DeckViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("foldman", Context.MODE_PRIVATE)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val library = LocalLibrary(app)
    private val local = LocalBackend(app).also { it.connect() }
    /** In-app Apple Music, when MusicKit's playback AAR is in the build; otherwise the Apple Music app is driven remotely. */
    private val appleKit = if (MusicKit.hasPlayback) AppleKitBackend(app) else null
    private val apple = MutableStateFlow(AppleUi(sdkAuth = MusicKit.hasAuth, sdkPlayback = appleKit != null))
    private var appleJob: Job? = null
    private val remotes = mapOf(
        Source.YT_MUSIC to RemoteBackend(app, Source.YT_MUSIC),
        Source.SPOTIFY to RemoteBackend(app, Source.SPOTIFY),
        Source.APPLE_MUSIC to RemoteBackend(app, Source.APPLE_MUSIC),
    )

    private val _ui = MutableStateFlow(DeckUi())
    val ui: StateFlow<DeckUi> = _ui.asStateFlow()
    private val _events = MutableSharedFlow<DeckEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DeckEvent> = _events

    private var source = runCatching { Source.valueOf(prefs.getString(KEY_SOURCE, null) ?: "") }.getOrDefault(Source.LOCAL)
    private val backend: DeckBackend
        get() = when (source) {
            Source.LOCAL -> local
            Source.APPLE_MUSIC -> appleKit ?: remotes.getValue(source)
            else -> remotes.getValue(source)
        }

    private val tapeStore = TapeStore(prefs)
    private var tapes: List<CustomTape> = tapeStore.load()
    private var loadedTape: CustomTape? =
        prefs.getString(KEY_TAPE, null)?.let { id -> tapes.firstOrNull { it.id == id && it.source == source } }
    private var tapeCued = false
    private var deckEmpty = prefs.getBoolean(KEY_EMPTY, false)
    private var theme = runCatching { DeckTheme.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(DeckTheme.SPORT_YELLOW)
    private var phoneMode = false
    private var deckOpen = prefs.getBoolean(KEY_DECK_OPEN, false)

    /** Set by MainActivity: true on phones without a hinge. */
    fun setPhoneMode(on: Boolean) {
        if (phoneMode == on) return
        phoneMode = on
        tick()
    }

    /** Phone mode: EJECT pops the door open and shows the rack and deck. */
    private fun openDeck() {
        deckOpen = true
        prefs.edit { putBoolean(KEY_DECK_OPEN, true) }
        tick()
    }

    /** Phone mode: the CLOSE button shuts the lid and goes back to the player. */
    fun closeDeck() {
        _events.tryEmit(DeckEvent.KeyClick)
        deckOpen = false
        prefs.edit { putBoolean(KEY_DECK_OPEN, false) }
        tick()
    }

    /** The folded front screen is showing (volume buttons become PLAY / PAUSE there). */
    var coverShown = false
        private set
    /** When PLAY last started a tape's playlist: YT Music takes a few seconds to open and start. */
    private var tapeStartAt = -1L

    private var tracks: List<LocalTrack> = emptyList()
    private var folderName: String? = null
    private var scanJob: Job? = null

    /** STOP was pressed (or the tape ended): show STOPPED rather than PAUSED while not playing. */
    private var stopped = true
    /** When STOP was last pressed: every key stays up while the other app winds down. */
    private var stopRequestedAt = 0L
    /** Play was just pressed; keep the play key down while the player catches up. */
    private var playRequestedAt = 0L
    /** -1 rewinding, +1 fast-forwarding, 0 neither. */
    private var winding = 0
    private var windPosition = 0L
    private var windLastTick = 0L
    private var windLastSeek = 0L
    private var windWaitingForTrack = -1

    private var paletteKey: Any? = null
    private var label = LabelPalette.Default
    private var artBitmap: ImageBitmap? = null
    private var artSource: Any? = null

    // Settings read on every refresh of the screen. They must be loaded before the init block
    // below, which refreshes the screen straight away (an unloaded list here crashed the app).
    private var stopOnOpen = prefs.getBoolean(KEY_STOP_ON_OPEN, false)
    var volumeKeysOn = prefs.getBoolean(KEY_VOLUME_KEYS, true)
        private set
    private var lockScreen = prefs.getBoolean(KEY_LOCK_SCREEN, false)
    private var phonePortrait = prefs.getBoolean(KEY_PHONE_PORTRAIT, false)
    private var spotifyName: String? = SpotifyApi.name(app)
    private var spotifyClientId: String = SpotifyApi.clientId(app)
    private var spotifyMessage: String? = null
    private var youTubeSignedIn = prefs.getBoolean(KEY_YT_SIGNED_IN, false)
    private var youTubeMessage: String? = null
    private var frontShelf = prefs.getBoolean(KEY_FRONT_SHELF, false)
    private var jcardDark = prefs.getBoolean(KEY_JCARD_DARK, false)

    private var hiddenSources: Set<Source> = prefs.getStringSet(KEY_HIDDEN_SOURCES, emptySet()).orEmpty()
        .mapNotNull { runCatching { Source.valueOf(it) }.getOrNull() }.toSet()
    private var noticeMode = runCatching { NoticeMode.valueOf(prefs.getString(KEY_NOTICES, null) ?: "") }.getOrDefault(NoticeMode.OFF)
    private var hingeStyle = runCatching { HingeStyle.valueOf(prefs.getString(KEY_HINGES, null) ?: "") }.getOrDefault(HingeStyle.PIANO)
    private var backupMessage: String? = null

    init {
        prefs.getString(KEY_FOLDER, null)?.let { loadFolder(Uri.parse(it), firstLoad = true) }
        syncAppleTokens()
        if (apple.value.signedIn) loadApplePlaylists()
        appleKit?.let { kit ->
            viewModelScope.launch {
                kit.messages.collect { m -> if (m != null) apple.value = apple.value.copy(message = m) }
            }
        }
        backend.refresh()
        // Re-check where the sound goes every couple of seconds, for changes Android doesn't announce.
        viewModelScope.launch {
            while (true) {
                delay(2000)
                checkAudioOutput()
            }
        }
        viewModelScope.launch {
            while (true) {
                tick()
                delay(if (winding != 0) 33 else 100)
            }
        }
    }

    // region Keys

    /**
     * The PLAY key. Like a Walkman: when PLAY is already down (playing, or paused with PAUSE also
     * down) pressing it does nothing; PAUSE brings it back, STOP pops everything up.
     */
    fun play() {
        val t = _ui.value.transport
        if (t == Transport.PLAYING || t == Transport.PAUSED) return
        startPlaying()
    }

    private fun startPlaying() {
        if (deckEmpty) return
        stopRequestedAt = 0
        if (winding != 0) finishWinding()
        val tape = loadedTape
        if (tape != null && tapeCued && source == tape.source) {
            tapeCued = false
            stopped = false
            playRequestedAt = SystemClock.elapsedRealtime()
            tapeStartAt = playRequestedAt
            startPlaylist(tape)
            tick()
            return
        }
        stopped = false
        playRequestedAt = SystemClock.elapsedRealtime()
        backend.play()
        // Foldy Streaman: if YT Music isn't running, it can't hear the key; open it and come back.
        val remote = remotes[source]
        if (remote != null && backend === remote && !remote.hasSession && remote.hasAccess && remote.isInstalled) {
            viewModelScope.launch {
                delay(2500)
                if (!remote.hasSession) {
                    remote.openApp()
                    comeBackWhenPlaying()
                }
            }
        }
        tick()
    }

    fun pauseKey() {
        when (_ui.value.transport) {
            Transport.PLAYING -> {
                stopped = false
                playRequestedAt = 0
                backend.pause()
            }
            Transport.PAUSED -> startPlaying()
            else -> return
        }
        tick()
    }

    fun stop() {
        when {
            winding != 0 -> finishWinding()
            _ui.value.transport == Transport.STOPPED -> {
                _events.tryEmit(DeckEvent.Eject)
                // On a phone that doesn't fold, EJECT opens the deck once the door has popped.
                if (phoneMode && !deckOpen) viewModelScope.launch {
                    delay(380)
                    openDeck()
                }
            }
            else -> {
                backend.pause()
                stopRequestedAt = SystemClock.elapsedRealtime()
            }
        }
        stopped = true
        playRequestedAt = 0
        tick()
    }

    fun rewind() = startWinding(-1)
    fun fastForward() = startWinding(+1)

    /** AMS back: to the top of this song, or the previous one when already near its start. */
    fun previous() {
        if (deckEmpty) return
        if (winding != 0) finishWinding()
        val wasPlaying = _ui.value.transport == Transport.PLAYING
        if (backend.positionMs() > 3000 || !_ui.value.hasPrevious) backend.seekTo(0) else backend.skipToPrevious()
        if (!wasPlaying) stayStoppedAfterSkip()
        tick()
    }

    fun next() {
        if (deckEmpty) return
        if (winding != 0) finishWinding()
        val wasPlaying = _ui.value.transport == Transport.PLAYING
        backend.skipToNext()
        if (!wasPlaying) stayStoppedAfterSkip()
        tick()
    }

    /**
     * Skipping while stopped or paused only moves to the song: every key pops up (a pause becomes
     * a stop) and nothing plays until PLAY. Music apps often start playing on a skip, so that is
     * paused again straight away.
     */
    private fun stayStoppedAfterSkip() {
        stopped = true
        playRequestedAt = 0
        stopRequestedAt = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            repeat(8) {
                delay(250)
                if (stopped && backend.state.value.isPlaying) backend.pause()
            }
        }
    }

    private fun startWinding(direction: Int) {
        if (deckEmpty) return
        if (winding == direction) return
        if (_ui.value.status != SourceStatus.Ready) return
        if (winding == 0) {
            backend.pause()
            windPosition = backend.positionMs()
        }
        winding = direction
        windLastTick = SystemClock.elapsedRealtime()
        windWaitingForTrack = -1
        playRequestedAt = 0
        tick()
    }

    private fun finishWinding() {
        winding = 0
        windWaitingForTrack = -1
        backend.seekTo(windPosition)
    }

    private fun tapeEnd() {
        finishWinding()
        stopped = true
        _events.tryEmit(DeckEvent.TapeEnd)
    }

    /** Moves the tape at [WIND_SPEED]× while REW/FF is latched, crossing into neighbouring songs like a real tape. */
    private fun windStep(state: BackendState) {
        val now = SystemClock.elapsedRealtime()
        val dt = now - windLastTick
        windLastTick = now
        val duration = state.nowPlaying.durationMs
        if (windWaitingForTrack >= 0) {
            // Just rewound into the previous song: wait until we know how long it is, then start from its end.
            if (state.trackIndex != windWaitingForTrack || duration <= 0) return
            windWaitingForTrack = -1
            windPosition = duration
        }
        windPosition += winding * dt * WIND_SPEED
        val isLocal = source == Source.LOCAL
        when {
            windPosition <= 0 -> {
                windPosition = 0
                if (isLocal && state.hasPrevious) {
                    windWaitingForTrack = state.trackIndex - 1
                    backend.skipToPrevious()
                } else tapeEnd()
            }
            duration > 0 && windPosition >= duration -> {
                if (state.hasNext) {
                    windPosition = 0
                    backend.skipToNext()
                    if (isLocal) backend.pause() else tapeEnd()
                } else {
                    windPosition = duration
                    tapeEnd()
                }
            }
            // Other apps' sessions get fewer seeks; ours can take one per frame.
            isLocal || now - windLastSeek > 250 -> {
                backend.seekTo(windPosition)
                windLastSeek = now
            }
        }
    }

    // endregion

    // region Sources

    /** Loads a plain source (Local folder, YT Music, Apple Music), taking out any tape of yours. */
    fun selectSource(newSource: Source) {
        val hadTape = loadedTape != null
        if (hadTape) {
            loadedTape = null
            tapeCued = false
            prefs.edit { remove(KEY_TAPE) }
        }
        setEmpty(false)
        switchSource(newSource)
        tick()
    }

    private fun setEmpty(empty: Boolean) {
        if (deckEmpty == empty) return
        deckEmpty = empty
        prefs.edit { putBoolean(KEY_EMPTY, empty) }
    }

    /** Swipes the tape out of the deck: the music stops and the slot is left empty. */
    fun setTheme(t: DeckTheme) {
        theme = t
        prefs.edit { putString(KEY_THEME, t.name) }
        tick()
    }

    fun setCoverShown(shown: Boolean) {
        if (coverShown == shown) return
        coverShown = shown
        tick()
    }

    fun setNoticeMode(mode: NoticeMode) {
        noticeMode = mode
        prefs.edit { putString(KEY_NOTICES, mode.name) }
        tick()
    }

    fun setPhonePortrait(on: Boolean) {
        phonePortrait = on
        prefs.edit { putBoolean(KEY_PHONE_PORTRAIT, on) }
        tick()
    }

    fun setLockScreen(on: Boolean) {
        lockScreen = on
        prefs.edit { putBoolean(KEY_LOCK_SCREEN, on) }
        tick()
    }

    private var volumeUpTaps = 0
    private var volumeDownTaps = 0
    private var volumeJob: Job? = null

    /**
     * The phone's volume buttons on the front screen, like Bluetooth headphones:
     * up = PLAY, up twice = next song, up three times = previous song;
     * down = PAUSE (again to un-pause), down twice = STOP.
     * Each press clicks straight away; the action waits a moment to count the taps.
     */
    fun volumeButton(up: Boolean) {
        _events.tryEmit(DeckEvent.KeyClick)
        if (up) {
            volumeDownTaps = 0
            volumeUpTaps++
        } else {
            volumeUpTaps = 0
            volumeDownTaps++
        }
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch {
            delay(MULTI_TAP_MS)
            val ups = volumeUpTaps
            val downs = volumeDownTaps
            volumeUpTaps = 0
            volumeDownTaps = 0
            when {
                ups == 1 -> play()
                ups == 2 -> next()
                ups >= 3 -> previous()
                downs == 1 -> pauseKey()
                downs >= 2 -> if (_ui.value.transport != Transport.STOPPED) stop()
            }
        }
    }

    // region Opening the phone, audio output, J-card photos (Foldy Streaman)


    fun setHingeStyle(h: HingeStyle) {
        hingeStyle = h
        prefs.edit { putString(KEY_HINGES, h.name) }
        tick()
    }

    // region Backup (Foldy Streaman)

    private fun settingsJson() = org.json.JSONObject()
        .put("theme", theme.name)
        .put("keySounds", prefs.getBoolean(KEY_SOUNDS, true))
        .put("haptics", prefs.getBoolean(KEY_HAPTICS, true))
        .put("stopOnOpen", stopOnOpen)
        .put("volumeKeys", volumeKeysOn)
        .put("lockScreen", lockScreen)
        .put("frontShelf", frontShelf)
        .put("hiddenSources", org.json.JSONArray(hiddenSources.map { it.name }))
        .put("hinges", hingeStyle.name)
        .put("notices", noticeMode.name)

    /** Saves every tape (pictures included) and the settings to the file the person picked. */
    fun exportBackup(uri: Uri) {
        val app = getApplication<Application>()
        val snapshot = tapes
        val settings = settingsJson()
        viewModelScope.launch {
            backupMessage = runCatching {
                withContext(Dispatchers.IO) { TapeBackup.write(app, uri, snapshot, settings) }
                "Backed up ${snapshot.size} tape${if (snapshot.size == 1) "" else "s"} and your settings."
            }.getOrElse { "Backup didn't work: ${it.message ?: "unknown problem"}" }
            tick()
        }
    }

    /** Loads a backup: its tapes join the rack (a tape already here is replaced), and its settings apply. */
    fun importBackup(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            backupMessage = runCatching {
                val (restored, settings) = withContext(Dispatchers.IO) { TapeBackup.read(app, uri) }
                val ids = restored.map { it.id }.toSet()
                tapes = tapes.filterNot { it.id in ids } + restored
                tapeStore.save(tapes)
                settings?.let { applySettings(it) }
                "Restored ${restored.size} tape${if (restored.size == 1) "" else "s"}${if (settings != null) " and your settings" else ""}."
            }.getOrElse { "Restore didn't work: ${it.message ?: "unknown problem"}" }
            tick()
        }
    }

    private fun applySettings(o: org.json.JSONObject) {
        runCatching { DeckTheme.valueOf(o.optString("theme")) }.getOrNull()?.let { setTheme(it) }
        if (o.has("keySounds")) prefs.edit { putBoolean(KEY_SOUNDS, o.optBoolean("keySounds", true)) }
        if (o.has("haptics")) prefs.edit { putBoolean(KEY_HAPTICS, o.optBoolean("haptics", true)) }
        if (o.has("stopOnOpen")) setStopOnOpen(o.optBoolean("stopOnOpen"))
        if (o.has("volumeKeys")) setVolumeKeys(o.optBoolean("volumeKeys"))
        if (o.has("lockScreen")) setLockScreen(o.optBoolean("lockScreen"))
        runCatching { NoticeMode.valueOf(o.optString("notices")) }.getOrNull()?.let { setNoticeMode(it) }
        if (o.has("frontShelf")) setFrontShelf(o.optBoolean("frontShelf"))
        o.optJSONArray("hiddenSources")?.let { arr ->
            hiddenSources = List(arr.length()) { arr.optString(it) }.mapNotNull { runCatching { Source.valueOf(it) }.getOrNull() }.toSet()
            prefs.edit { putStringSet(KEY_HIDDEN_SOURCES, hiddenSources.map { it.name }.toSet()) }
        }
        runCatching { HingeStyle.valueOf(o.optString("hinges")) }.getOrNull()?.let { setHingeStyle(it) }
    }

    // endregion

    /** Settings > Adapters on the rack: show or hide one adapter (your own tapes aren't affected). */
    fun setSourceShown(s: Source, shown: Boolean) {
        hiddenSources = if (shown) hiddenSources - s else hiddenSources + s
        prefs.edit { putStringSet(KEY_HIDDEN_SOURCES, hiddenSources.map { it.name }.toSet()) }
        tick()
    }

    fun setJcardDark(on: Boolean) {
        jcardDark = on
        prefs.edit { putBoolean(KEY_JCARD_DARK, on) }
        tick()
    }

    fun setFrontShelf(on: Boolean) {
        frontShelf = on
        prefs.edit { putBoolean(KEY_FRONT_SHELF, on) }
        tick()
    }

    /** The Google login finished (or was logged out of): remember it, and anything it had to say. */
    fun setYouTubeSignedIn(on: Boolean, message: String? = null) {
        youTubeSignedIn = on
        youTubeMessage = message
        prefs.edit { putBoolean(KEY_YT_SIGNED_IN, on) }
        tick()
    }

    // region Spotify login

    fun setSpotifyClientId(id: String) {
        SpotifyApi.setClientId(getApplication(), id)
        spotifyClientId = SpotifyApi.clientId(getApplication())
        tick()
    }

    /** Opens Spotify's own login page in the browser; it comes back through [finishSpotifyLogin]. */
    fun spotifyLogin() {
        val app = getApplication<Application>()
        if (SpotifyApi.clientId(app).isBlank()) {
            spotifyMessage = "Add the Spotify Client ID first."
            tick()
            return
        }
        spotifyMessage = null
        runCatching { app.startActivity(Intent(Intent.ACTION_VIEW, SpotifyApi.loginPage(app)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { spotifyMessage = "Couldn't open the browser for the Spotify login." }
        tick()
    }

    fun finishSpotifyLogin(back: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            spotifyMessage = SpotifyApi.finishLogin(app, back)
            spotifyName = SpotifyApi.name(app)
            tick()
        }
    }

    fun spotifyLogout() {
        SpotifyApi.logOut(getApplication())
        spotifyName = null
        spotifyMessage = null
        tick()
    }

    // endregion

    fun setVolumeKeys(on: Boolean) {
        volumeKeysOn = on
        prefs.edit { putBoolean(KEY_VOLUME_KEYS, on) }
        tick()
    }

    fun setStopOnOpen(on: Boolean) {
        stopOnOpen = on
        prefs.edit { putBoolean(KEY_STOP_ON_OPEN, on) }
        tick()
    }

    /** The Fold was just opened: stop the music if Settings says so. */
    fun onPhoneOpened() {
        if (!stopOnOpen) return
        if (_ui.value.transport != Transport.STOPPED) stop()
    }

    /** Sound is going somewhere other than this phone's own speaker (Bluetooth, wired, USB...). */
    private var audioElsewhere = false
    /** Where the music last went while something was playing (kept while it's paused). */
    private var lastRouted: Boolean? = null
    private val audioDevices = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out android.media.AudioDeviceInfo>?) = checkAudioOutput()
        override fun onAudioDevicesRemoved(removed: Array<out android.media.AudioDeviceInfo>?) = checkAudioOutput()
    }
    /** Players starting, stopping or moving to another output. */
    private val audioPlayers = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>?) = checkAudioOutput()
    }

    // Start listening only now that both listeners above exist (an earlier init block runs before they do).
    init {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        runCatching { audio.registerAudioDeviceCallback(audioDevices, main) }
        runCatching { audio.registerAudioPlaybackCallback(audioPlayers, main) }
        checkAudioOutput()
    }

    /**
     * The plug sits in the jack when the music comes out of anything but this phone. Nothing
     * connected: no plug. Something connected: follow where the playing music actually goes, so
     * switching to "This phone" with headphones still connected pulls the plug out. While paused,
     * keep the last answer.
     */
    private fun checkAudioOutput() {
        val now = when {
            !AudioOutput.anyConnected(audio) -> {
                lastRouted = null
                false
            }
            else -> {
                AudioOutput.playingElsewhere(audio)?.let { lastRouted = it }
                lastRouted ?: true
            }
        }
        if (now != audioElsewhere) {
            audioElsewhere = now
            tick()
        }
    }


    /** Opens the system's "where does the sound go" menu (falls back to Bluetooth settings). */
    fun openOutputSwitcher() {
        _events.tryEmit(DeckEvent.KeyClick)
        AudioOutput.openSwitcher(getApplication(), remotes[source]?.let { source.packageName })
    }

    /** Copies a picked picture into the app (scaled down) and puts it on the tape's J-card. */
    fun setTapePhoto(tapeId: String, uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { TapePhotos.copyIn(app, tapeId, uri) } ?: return@launch
            val tape = tapes.firstOrNull { it.id == tapeId } ?: return@launch
            tape.photoPath?.let { old -> if (old != path) withContext(Dispatchers.IO) { java.io.File(old).delete() } }
            saveTape(tape.copy(photoPath = path, photoX = 0f, photoY = 0f, photoZoom = 1f))
        }
    }

    /** Puts an album cover from the cover search on the tape's J-card (Foldy keeps its own copy). */
    fun setTapeCover(tapeId: String, url: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val path = CoverSearch.download(app, tapeId, url) ?: return@launch
            val tape = tapes.firstOrNull { it.id == tapeId } ?: return@launch
            tape.photoPath?.let { old -> if (old != path) withContext(Dispatchers.IO) { java.io.File(old).delete() } }
            saveTape(tape.copy(photoPath = path, photoX = 0f, photoY = 0f, photoZoom = 1f))
        }
    }

    fun removeTapePhoto(tapeId: String) {
        val tape = tapes.firstOrNull { it.id == tapeId } ?: return
        tape.photoPath?.let { java.io.File(it).delete() }
        saveTape(tape.copy(photoPath = null, photoX = 0f, photoY = 0f, photoZoom = 1f))
    }

    // endregion

    fun ejectTape() {
        if (winding != 0) finishWinding()
        if (backend.state.value.isPlaying) backend.pause()
        loadedTape = null
        tapeCued = false
        stopped = true
        playRequestedAt = 0
        prefs.edit { remove(KEY_TAPE) }
        setEmpty(true)
        tick()
    }

    // region Your tapes (Foldy Streaman)

    /** Puts one of your tapes in the deck. PLAY then starts its playlist in YT Music. */
    fun loadTape(tape: CustomTape) {
        // A blank tape (no playlist on it yet) has nothing to play.
        if (tape.playlistId.isBlank()) return
        if (loadedTape?.id == tape.id) return
        if (winding != 0) finishWinding()
        if (backend.state.value.isPlaying) backend.pause()
        setEmpty(false)
        switchSource(tape.source)
        loadedTape = tape
        tapeCued = true
        stopped = true
        playRequestedAt = 0
        prefs.edit { putString(KEY_TAPE, tape.id) }
        tick()
    }

    /** Adds a new tape or saves changes to an existing one. */
    fun saveTape(tape: CustomTape) {
        val old = tapes.firstOrNull { it.id == tape.id }
        tapes = if (old == null) tapes + tape else tapes.map { if (it.id == tape.id) tape else it }
        tapeStore.save(tapes)
        if (loadedTape?.id == tape.id) {
            loadedTape = tape
            if (old?.playlistId != tape.playlistId) tapeCued = true
        }
        tick()
    }

    /** Drag to reorder: puts tape [id] just before tape [beforeId] on the rack (null = at the end). */
    fun moveTape(id: String, beforeId: String?) {
        val moving = tapes.firstOrNull { it.id == id } ?: return
        val rest = tapes.filterNot { it.id == id }
        val at = beforeId?.let { b -> rest.indexOfFirst { it.id == b } }?.takeIf { it >= 0 } ?: rest.size
        tapes = rest.take(at) + moving + rest.drop(at)
        tapeStore.save(tapes)
        tick()
    }

    fun deleteTape(id: String) {
        tapes.firstOrNull { it.id == id }?.photoPath?.let { java.io.File(it).delete() }
        tapes = tapes.filterNot { it.id == id }
        tapeStore.save(tapes)
        if (loadedTape?.id == id) {
            loadedTape = null
            tapeCued = false
            prefs.edit { remove(KEY_TAPE) }
        }
        tick()
    }

    /**
     * Asks YT Music to play the tape's playlist. Inside its player when it allows that (no screen
     * switch); otherwise by opening the playlist in YT Music, then coming straight back here as
     * soon as it starts playing.
     */
    private fun startPlaylist(tape: CustomTape) {
        // Spotify's player says it accepts links handed to it in the background, but then just
        // carries on with whatever it was playing. The way that does work is to open the playlist
        // in Spotify itself with ":play" on the end, so Spotify tapes always go that way (Spotify
        // shows for a moment, then Foldy comes back).
        val spotify = tape.service == TapeService.SPOTIFY
        if (!spotify && remotes[tape.source]?.playFromUri(Uri.parse(tape.playUrl)) == true) return
        // Logged in to Spotify: tell it over the internet to play the tape on this phone, with no
        // switching apps. That needs Spotify awake on the phone; if it isn't, nudge it and try a
        // few more times, and only then fall back to opening the playlist in Spotify itself.
        if (spotify && spotifyName != null) {
            val app = getApplication<Application>()
            if (backend.state.value.isPlaying) backend.pause()
            viewModelScope.launch {
                var r = SpotifyApi.play(app, tape.playlistId, tape.album)
                if (r.noDevice) {
                    // Spotify is asleep: wake it quietly in the background and keep asking.
                    wakeSpotify()
                    repeat(7) {
                        if (!r.ok && r.noDevice) {
                            delay(1200)
                            r = SpotifyApi.play(app, tape.playlistId, tape.album)
                        }
                    }
                }
                if (!r.ok && loadedTape?.id == tape.id && !stopped) {
                    // Say why, then do it the old way: open the playlist in Spotify itself.
                    android.widget.Toast.makeText(app, r.message + ". Opening Spotify instead.", android.widget.Toast.LENGTH_LONG).show()
                    openPlaylistInApp(tape)
                }
            }
            return
        }
        openPlaylistInApp(tape)
    }

    /** Kept while Spotify is being woken, so the connection isn't dropped straight away. */
    private var spotifyWaker: android.media.browse.MediaBrowser? = null

    /**
     * Wakes Spotify without showing it: knocking on the door it keeps for car screens and the like
     * starts it up in the background (whether or not it lets us in), which is enough for it to
     * report this phone as a place it can play.
     */
    private fun wakeSpotify() {
        val app = getApplication<Application>()
        val pkg = Source.SPOTIFY.packageName ?: return
        runCatching {
            val door = app.packageManager.queryIntentServices(Intent("android.media.browse.MediaBrowserService").setPackage(pkg), 0)
                .firstOrNull()?.serviceInfo ?: return
            spotifyWaker?.disconnect()
            val browser = android.media.browse.MediaBrowser(app, android.content.ComponentName(door.packageName, door.name), object : android.media.browse.MediaBrowser.ConnectionCallback() {}, null)
            spotifyWaker = browser
            browser.connect()
            viewModelScope.launch {
                delay(12_000)
                if (spotifyWaker === browser) {
                    runCatching { browser.disconnect() }
                    spotifyWaker = null
                }
            }
        }
        // And the plain way, for when Spotify is running but dozing.
        remotes[Source.SPOTIFY]?.pause()
    }

    /**
     * Plays [track] of the tape in the deck: straight from the music app's own queue when it's there,
     * through Spotify's connection when logged in, or by opening the song (within the playlist) in
     * the music app.
     */
    fun playTapeTrack(tape: CustomTape, track: TapeTrack) {
        if (loadedTape?.id != tape.id || deckEmpty) return
        if (winding != 0) finishWinding()
        stopRequestedAt = 0
        tapeCued = false
        stopped = false
        playRequestedAt = SystemClock.elapsedRealtime()
        tapeStartAt = playRequestedAt
        val hit = backend.state.value.queue.firstOrNull { TrackLists.sameSong(it.title, track.title) }
        if (hit != null) {
            backend.playQueueItem(hit.id)
            tick()
            return
        }
        val app = getApplication<Application>()
        when (tape.service) {
            TapeService.SPOTIFY -> {
                if (backend.state.value.isPlaying) backend.pause()
                viewModelScope.launch {
                    var r = if (spotifyName != null && track.uri != null) SpotifyApi.play(app, tape.playlistId, tape.album, track.uri) else null
                    if (r != null && r.noDevice) {
                        wakeSpotify()
                        repeat(5) {
                            if (r?.ok != true && r?.noDevice == true) {
                                delay(1200)
                                r = SpotifyApi.play(app, tape.playlistId, tape.album, track.uri)
                            }
                        }
                    }
                    if (r?.ok != true) openLinkInApp(tape, "spotify:track:${track.id}:play")
                }
            }
            TapeService.YT_MUSIC -> {
                val link = "https://music.youtube.com/watch?v=${track.id}&list=${tape.playlistId}"
                if (remotes[tape.source]?.playFromUri(Uri.parse(link)) != true) openLinkInApp(tape, link)
            }
        }
        tick()
    }

    /** Opens [link] in the tape's music app, then comes back here once it plays. */
    private fun openLinkInApp(tape: CustomTape, link: String) {
        val app = getApplication<Application>()
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        runCatching { app.startActivity(Intent(base).setPackage(tape.source.packageName)) }
            .recoverCatching { app.startActivity(base) }
        comeBackWhenPlaying()
    }

    /** Opens the tape's playlist in its own app and has it play, then comes back here. */
    private fun openPlaylistInApp(tape: CustomTape) {
        val spotify = tape.service == TapeService.SPOTIFY
        if (spotify) {
            // Stop the old music first, so if Spotify is slow the wrong playlist isn't heard.
            if (backend.state.value.isPlaying) backend.pause()
        } else {
            pendingTape = tape
            pendingUntil = SystemClock.elapsedRealtime() + COME_BACK_MS
        }
        val app = getApplication<Application>()
        val open = if (spotify) tape.playUrl + ":play" else tape.playUrl
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(open))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        runCatching { app.startActivity(Intent(base).setPackage(tape.source.packageName)) }
            .recoverCatching { app.startActivity(base) }
        comeBackWhenPlaying()
    }

    /** A tape whose playlist was opened in its app but may not have started playing there yet. */
    private var pendingTape: CustomTape? = null
    private var pendingUntil = 0L

    /** Starts [pendingTape] inside the app's own player once that player can be reached. */
    private fun maybeStartPending(state: BackendState, now: Long) {
        val tape = pendingTape ?: return
        if (now > pendingUntil || loadedTape?.id != tape.id || stopped) {
            pendingTape = null
            return
        }
        // Already playing: the app started it itself.
        if (state.isPlaying) {
            pendingTape = null
            return
        }
        if (remotes[tape.source]?.playFromUri(Uri.parse(tape.playUrl)) == true) pendingTape = null
    }

    /** Whether our screen is the one showing (set by MainActivity). */
    private var screenShown = false
    /** Until when to jump back to Foldy once the other app starts playing. */
    private var comeBackBy = 0L

    /** Whether the other app has actually taken over the screen since we opened it. */
    private var leftForOtherApp = false

    fun onScreenShown(shown: Boolean) {
        screenShown = shown
        if (!shown) leftForOtherApp = true
        // Back on our own screen after the other app had it: nothing left to do.
        else if (leftForOtherApp) comeBackBy = 0L
    }

    /**
     * Android lets an app reopen its own screen for a few seconds after it opened another app, so
     * Foldy jumps back as soon as YT Music starts playing (or gives up after [COME_BACK_MS]).
     */
    private fun comeBackWhenPlaying() {
        val now = SystemClock.elapsedRealtime()
        comeBackBy = now + COME_BACK_MS
        comeBackAnywayAt = now + COME_BACK_BLIND_MS
        leftForOtherApp = false
    }

    /** Without notification access we can't tell when YT Music starts, so come back after a pause. */
    private var comeBackAnywayAt = 0L

    private fun maybeComeBack(state: BackendState, now: Long) {
        if (comeBackBy == 0L) return
        if (now > comeBackBy) {
            comeBackBy = 0L
            return
        }
        // The other app takes a moment to cover our screen. Until it has, wait: giving up here
        // (as this used to) meant a slower phone stayed in Spotify instead of coming back.
        if (!leftForOtherApp) return
        val canSee = remotes[source]?.hasSession == true
        if (!state.isPlaying && (canSee || now < comeBackAnywayAt)) return
        comeBackBy = 0L
        val app = getApplication<Application>()
        val intent = Intent(app, dev.foldman.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { app.startActivity(intent) }
    }

    // endregion

    private fun switchSource(newSource: Source) {
        if (newSource == source) return
        if (winding != 0) finishWinding()
        // One tape in the deck at a time: ejecting stops the old one.
        if (backend.state.value.isPlaying) backend.pause()
        source = newSource
        prefs.edit { putString(KEY_SOURCE, newSource.name) }
        stopped = !backend.state.value.isPlaying
        backend.refresh()
        tick()
    }

    fun setFolder(uri: Uri) {
        prefs.edit { putString(KEY_FOLDER, uri.toString()) }
        selectSource(Source.LOCAL)
        loadFolder(uri, firstLoad = false)
    }

    fun playTrack(index: Int) {
        selectSource(Source.LOCAL)
        if (winding != 0) finishWinding()
        stopped = false
        playRequestedAt = SystemClock.elapsedRealtime()
        local.playTrack(index)
    }

    private fun loadFolder(uri: Uri, firstLoad: Boolean) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            local.setLibraryStatus(SourceStatus.Scanning(0))
            val found = runCatching {
                library.scan(uri) { n -> viewModelScope.launch { local.setLibraryStatus(SourceStatus.Scanning(n)) } }
            }.getOrElse {
                local.setLibraryStatus(SourceStatus.NeedsFolder)
                return@launch
            }
            folderName = found.firstOrNull()?.folder ?: uri.lastPathSegment?.substringAfterLast(':')
            tracks = found
            local.setTracks(found, startIndex = if (firstLoad) null else 0)
            if (!firstLoad) stopped = true
            tick()
            val tagged = library.readTags(found)
            tracks = tagged
            local.setTracks(tagged)
            tick()
        }
    }

    fun refresh() {
        backend.refresh()
        tick()
    }

    fun openAccessSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }
    }

    fun openSourceApp() {
        (remotes[source])?.openApp()
    }

    private var queueFor: List<LocalTrack>? = null
    private var queueCache: List<QueueEntry> = emptyList()

    private fun localQueue(): List<QueueEntry> {
        if (queueFor !== tracks) {
            queueFor = tracks
            queueCache = tracks.mapIndexed { i, t ->
                QueueEntry(i.toLong(), t.title, t.artist ?: t.folder, artworkUri = t.coverUri)
            }
        }
        return queueCache
    }

    fun playQueueItem(id: Long) {
        if (source == Source.LOCAL) return playTrack(id.toInt())
        if (winding != 0) finishWinding()
        stopped = false
        playRequestedAt = SystemClock.elapsedRealtime()
        backend.playQueueItem(id)
    }

    fun setKeySounds(on: Boolean) {
        prefs.edit { putBoolean(KEY_SOUNDS, on) }
        tick()
    }

    fun setHaptics(on: Boolean) {
        prefs.edit { putBoolean(KEY_HAPTICS, on) }
        tick()
    }

    // region Apple Music

    private val developerToken: String?
        get() = prefs.getString(KEY_APPLE_DEV, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.APPLE_DEVELOPER_TOKEN.takeIf { it.isNotBlank() }
    private val musicUserToken: String? get() = prefs.getString(KEY_APPLE_USER, null)

    private fun syncAppleTokens() {
        appleKit?.setTokens(developerToken, musicUserToken)
        apple.value = apple.value.copy(hasDeveloperToken = developerToken != null, signedIn = musicUserToken != null)
    }

    fun setAppleDeveloperToken(token: String) {
        prefs.edit { putString(KEY_APPLE_DEV, token.trim()) }
        syncAppleTokens()
        tick()
    }

    /** Apple's sign-in screen, or null when it can't be shown (no SDK or no developer token). */
    fun appleSignInIntent(): Intent? {
        val dev = developerToken ?: return null
        return MusicKit.signInIntent(getApplication(), dev).also {
            if (it == null) apple.value = apple.value.copy(message = "MusicKit couldn't start sign-in.")
        }
    }

    fun onAppleSignInResult(data: Intent?) {
        MusicKit.readSignInResult(getApplication(), data)
            .onSuccess { token ->
                prefs.edit { putString(KEY_APPLE_USER, token) }
                apple.value = apple.value.copy(message = null)
                syncAppleTokens()
                loadApplePlaylists()
            }
            .onFailure { e -> apple.value = apple.value.copy(message = "Sign-in didn't finish (${e.message}).") }
        tick()
    }

    fun signOutOfApple() {
        prefs.edit { remove(KEY_APPLE_USER) }
        apple.value = apple.value.copy(playlists = emptyList(), openPlaylist = null, tracks = emptyList())
        syncAppleTokens()
        tick()
    }

    fun loadApplePlaylists() = appleCall { api ->
        apple.value = apple.value.copy(playlists = api.playlists())
    }

    fun openApplePlaylist(playlist: ApplePlaylist) {
        apple.value = apple.value.copy(openPlaylist = playlist, tracks = emptyList())
        appleCall { api ->
            val tracks = api.tracks(playlist.id)
            if (apple.value.openPlaylist?.id == playlist.id) apple.value = apple.value.copy(tracks = tracks)
        }
    }

    fun closeApplePlaylist() {
        apple.value = apple.value.copy(openPlaylist = null, tracks = emptyList())
        tick()
    }

    /** Plays the open playlist from [trackIndex]: in-app with MusicKit, otherwise handed to the Apple Music app. */
    fun playApplePlaylist(trackIndex: Int = 0) {
        val ui = apple.value
        val playlist = ui.openPlaylist ?: return
        selectSource(Source.APPLE_MUSIC)
        val kit = appleKit
        if (kit == null) {
            openInAppleMusic(playlist)
            return
        }
        // Uploaded songs have no catalog id and can't be streamed; skip them, keeping the start song.
        val playable = ui.tracks.withIndex().filter { it.value.catalogId != null }
        if (playable.isEmpty()) {
            apple.value = ui.copy(message = "Nothing in this playlist can be streamed.")
            return
        }
        val start = playable.indexOfFirst { it.index >= trackIndex }.coerceAtLeast(0)
        if (winding != 0) finishWinding()
        stopped = false
        playRequestedAt = SystemClock.elapsedRealtime()
        kit.messages.value = null
        apple.value = apple.value.copy(message = null)
        kit.playSongs(playable.map { it.value.catalogId!! }, start)
        tick()
    }

    private fun openInAppleMusic(playlist: ApplePlaylist) {
        val url = playlist.globalId?.let { "https://music.apple.com/playlist/$it" }
            ?: "https://music.apple.com/library/playlist/${playlist.id}"
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        val app = getApplication<Application>()
        runCatching { app.startActivity(Intent(base).setPackage(Source.APPLE_MUSIC.packageName)) }
            .recoverCatching { app.startActivity(base) }
    }

    private fun appleCall(block: suspend (AppleMusicApi) -> Unit) {
        val dev = developerToken ?: return
        val user = musicUserToken ?: return
        appleJob?.cancel()
        appleJob = viewModelScope.launch {
            apple.value = apple.value.copy(loading = true, message = null)
            try {
                block(AppleMusicApi(dev, user))
            } catch (e: AppleAuthExpired) {
                prefs.edit { remove(KEY_APPLE_USER) }
                syncAppleTokens()
                apple.value = apple.value.copy(message = "Apple Music sign-in expired or the developer token was rejected. Sign in again.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                apple.value = apple.value.copy(message = e.message ?: "Couldn't reach Apple Music.")
            } finally {
                apple.value = apple.value.copy(loading = false)
                tick()
            }
        }
    }

    // endregion

    fun setVolume(fraction: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (fraction.coerceIn(0f, 1f) * max).roundToInt()
        if (target != audio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        _ui.value = _ui.value.copy(volume = fraction.coerceIn(0f, 1f))
    }

    // endregion

    private fun tick() {
        val state = backend.state.value
        val now = SystemClock.elapsedRealtime()
        if (winding != 0) windStep(state)
        maybeStartPending(state, now)
        maybeComeBack(state, now)
        if (state.ended) stopped = true
        // Just after STOP the other app may still say "playing" for a moment: don't let that undo the stop.
        val stopping = now - stopRequestedAt < 2500
        if (state.isPlaying && winding == 0 && !stopping) {
            stopped = false
            playRequestedAt = 0
        }
        // A tape's playlist needs YT Music to open first, so give it longer before the key pops up.
        val playPending = now - playRequestedAt < if (playRequestedAt == tapeStartAt) 10_000 else 1500
        val transport = when {
            winding < 0 -> Transport.REWINDING
            winding > 0 -> Transport.FAST_FORWARDING
            stopping -> Transport.STOPPED
            state.isPlaying || playPending -> Transport.PLAYING
            stopped -> Transport.STOPPED
            else -> Transport.PAUSED
        }
        val np = state.nowPlaying
        updateLabel(np)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        val prev = _ui.value
        _ui.value = DeckUi(
            source = source,
            transport = transport,
            status = state.status,
            title = if (deckEmpty) "" else np.title.orEmpty(),
            artist = if (deckEmpty) "" else np.artist.orEmpty(),
            album = if (deckEmpty) "" else np.album.orEmpty(),
            artwork = if (deckEmpty) null else artBitmap,
            label = label,
            positionMs = if (winding != 0) windPosition else backend.positionMs(),
            durationMs = np.durationMs,
            trackIndex = state.trackIndex,
            trackCount = state.trackCount,
            hasNext = state.hasNext,
            hasPrevious = state.hasPrevious,
            tracks = tracks,
            folderName = folderName,
            // While the wheel is being turned our own value leads; otherwise follow the hardware keys.
            volume = if (kotlin.math.abs(prev.volume - volume) * max < 1f) prev.volume else volume,
            queue = if (source == Source.LOCAL) localQueue() else state.queue,
            currentQueueId = if (source == Source.LOCAL) state.trackIndex.toLong() else state.currentQueueId,
            keySounds = prefs.getBoolean(KEY_SOUNDS, true),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            apple = apple.value,
            tapes = tapes,
            tape = loadedTape,
            tapeCued = tapeCued,
            empty = deckEmpty,
            theme = theme,
            phoneMode = phoneMode,
            deckOpen = phoneMode && deckOpen,
            stopOnOpen = stopOnOpen,
            volumeKeys = volumeKeysOn,
            lockScreen = lockScreen,
            phonePortrait = phonePortrait,
            coverShown = coverShown,
            spotifyName = spotifyName,
            spotifyClientId = spotifyClientId,
            spotifyMessage = spotifyMessage,
            youTubeSignedIn = youTubeSignedIn,
            youTubeMessage = youTubeMessage,
            audioElsewhere = audioElsewhere,
            frontShelf = frontShelf,
            jcardDark = jcardDark,
            hiddenSources = hiddenSources,
            hingeStyle = hingeStyle,
            noticeMode = noticeMode,
            backupMessage = backupMessage,
        )
    }

    private fun updateLabel(np: NowPlaying) {
        val bitmap = np.artwork
        if (bitmap == null) {
            artBitmap = null
            artSource = null
            paletteKey = null
            label = LabelPalette.Default
            return
        }
        if (artSource !== bitmap) {
            artSource = bitmap
            artBitmap = bitmap.asImageBitmap()
        }
        val key = np.artworkKey ?: bitmap
        if (key == paletteKey) return
        paletteKey = key
        viewModelScope.launch {
            val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(16).generate() }
            if (paletteKey != key) return@launch
            val d = LabelPalette.Default
            label = LabelPalette(
                paper = palette.lightMutedSwatch?.rgb?.let { blend(Color(it), d.paper, 0.75f) } ?: d.paper,
                stripe = (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let(::Color) ?: d.stripe,
                stripe2 = (palette.lightVibrantSwatch ?: palette.mutedSwatch)?.rgb?.let(::Color) ?: d.stripe2,
                ink = palette.darkMutedSwatch?.rgb?.let { blend(Color(it), d.ink, 0.5f) } ?: d.ink,
            )
            tick()
        }
    }

    override fun onCleared() {
        runCatching { audio.unregisterAudioDeviceCallback(audioDevices) }
        runCatching { audio.unregisterAudioPlaybackCallback(audioPlayers) }
        local.release()
        remotes.values.forEach { it.release() }
        appleKit?.release()
    }

    companion object {
        private const val KEY_SOURCE = "source"
        private const val KEY_TAPE = "custom_tape_in_deck"
        private const val KEY_EMPTY = "deck_empty"
        private const val KEY_THEME = "theme"
        private const val KEY_DECK_OPEN = "phone_deck_open"
        private const val KEY_STOP_ON_OPEN = "stop_on_open"
        private const val KEY_VOLUME_KEYS = "volume_keys"
        private const val KEY_YT_SIGNED_IN = "youtube_signed_in"
        private const val KEY_LOCK_SCREEN = "lock_screen"
        private const val KEY_PHONE_PORTRAIT = "phone_portrait"
        private const val KEY_NOTICES = "notices_on_player"
        private const val KEY_FRONT_SHELF = "front_shelf"
        private const val KEY_JCARD_DARK = "jcard_dark"
        private const val KEY_HIDDEN_SOURCES = "hidden_sources"
        private const val KEY_HINGES = "hinges"
        /** How long to wait for another volume-button tap before acting. */
        private const val MULTI_TAP_MS = 380L
        private const val KEY_FOLDER = "folder"
        private const val KEY_SOUNDS = "key_sounds"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_APPLE_DEV = "apple_developer_token"
        private const val KEY_APPLE_USER = "apple_music_user_token"
        /** Winding speed relative to play: a four-minute song winds through in about 20 s. */
        const val WIND_SPEED = 12L
        /** How long after opening YT Music Foldy may still jump back in front of it. */
        private const val COME_BACK_MS = 9_000L
        /** When Foldy can't see YT Music's player: come back this long after opening it. */
        private const val COME_BACK_BLIND_MS = 4_500L

        private fun blend(a: Color, b: Color, t: Float) = Color(
            red = a.red * (1 - t) + b.red * t,
            green = a.green * (1 - t) + b.green * t,
            blue = a.blue * (1 - t) + b.blue * t,
        )
    }
}
