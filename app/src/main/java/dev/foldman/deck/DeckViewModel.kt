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

/** Foldy Streaman: how the player looks. */
enum class DeckTheme(val label: String) {
    ORIGINAL("Original"),
    BLACK_GOLD("Black & Gold"),
    CHERRY_RED("Cherry Red"),
    SILVER_BLUE("Silver & Blue"),
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
    /** Sound is going to headphones, a speaker or the car rather than this phone: a plug sits in the jack. */
    val audioElsewhere: Boolean = false,
    /** Settings: a TAPES button on the front screen opens a tape shelf there. */
    val frontShelf: Boolean = false,
    /** Settings: adapters switched off, left off the rack. */
    val hiddenSources: Set<Source> = emptySet(),
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
    private var theme = runCatching { DeckTheme.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(DeckTheme.ORIGINAL)
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
            else -> backend.pause()
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
        if (backend.positionMs() > 3000 || !_ui.value.hasPrevious) backend.seekTo(0) else backend.skipToPrevious()
        tick()
    }

    fun next() {
        if (deckEmpty) return
        if (winding != 0) finishWinding()
        backend.skipToNext()
        tick()
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
        coverShown = shown
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

    private var stopOnOpen = prefs.getBoolean(KEY_STOP_ON_OPEN, false)
    private var frontShelf = prefs.getBoolean(KEY_FRONT_SHELF, false)

    private var hiddenSources: Set<Source> = prefs.getStringSet(KEY_HIDDEN_SOURCES, emptySet()).orEmpty()
        .mapNotNull { runCatching { Source.valueOf(it) }.getOrNull() }.toSet()

    /** Settings > Adapters on the rack: show or hide one adapter (your own tapes aren't affected). */
    fun setSourceShown(s: Source, shown: Boolean) {
        hiddenSources = if (shown) hiddenSources - s else hiddenSources + s
        prefs.edit { putStringSet(KEY_HIDDEN_SOURCES, hiddenSources.map { it.name }.toSet()) }
        tick()
    }

    fun setFrontShelf(on: Boolean) {
        frontShelf = on
        prefs.edit { putBoolean(KEY_FRONT_SHELF, on) }
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
        if (remotes[tape.source]?.playFromUri(Uri.parse(tape.playUrl)) == true) return
        val app = getApplication<Application>()
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(tape.playUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        runCatching { app.startActivity(Intent(base).setPackage(tape.source.packageName)) }
            .recoverCatching { app.startActivity(base) }
        comeBackWhenPlaying()
    }

    /** Whether our screen is the one showing (set by MainActivity). */
    private var screenShown = false
    /** Until when to jump back to Foldy once the other app starts playing. */
    private var comeBackBy = 0L

    fun onScreenShown(shown: Boolean) {
        screenShown = shown
        if (shown) comeBackBy = 0L
    }

    /**
     * Android lets an app reopen its own screen for a few seconds after it opened another app, so
     * Foldy jumps back as soon as YT Music starts playing (or gives up after [COME_BACK_MS]).
     */
    private fun comeBackWhenPlaying() {
        val now = SystemClock.elapsedRealtime()
        comeBackBy = now + COME_BACK_MS
        comeBackAnywayAt = now + COME_BACK_BLIND_MS
    }

    /** Without notification access we can't tell when YT Music starts, so come back after a pause. */
    private var comeBackAnywayAt = 0L

    private fun maybeComeBack(state: BackendState, now: Long) {
        if (comeBackBy == 0L) return
        if (now > comeBackBy || screenShown) {
            comeBackBy = 0L
            return
        }
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
        maybeComeBack(state, now)
        if (state.ended) stopped = true
        if (state.isPlaying && winding == 0) {
            stopped = false
            playRequestedAt = 0
        }
        // A tape's playlist needs YT Music to open first, so give it longer before the key pops up.
        val playPending = now - playRequestedAt < if (playRequestedAt == tapeStartAt) 10_000 else 1500
        val transport = when {
            winding < 0 -> Transport.REWINDING
            winding > 0 -> Transport.FAST_FORWARDING
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
            audioElsewhere = audioElsewhere,
            frontShelf = frontShelf,
            hiddenSources = hiddenSources,
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
        private const val KEY_FRONT_SHELF = "front_shelf"
        private const val KEY_HIDDEN_SOURCES = "hidden_sources"
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
