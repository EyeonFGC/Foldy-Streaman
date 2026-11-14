package dev.foldman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.content.pm.ActivityInfo
import androidx.window.layout.FoldingFeature
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import dev.foldman.deck.DeckViewModel
import dev.foldman.deck.Source
import dev.foldman.ui.FoldmanScreen

class MainActivity : ComponentActivity() {
    private val vm by lazy { ViewModelProvider(this)[DeckViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        spotifyLoginFrom(intent)
        // Foldy Streaman: phones that don't fold get EJECT-to-open and CLOSE-to-play instead.
        val foldable = isFoldable()
        vm.setPhoneMode(!foldable)
        if (!foldable) applyPhoneOrientation(getSharedPreferences("foldman", MODE_PRIVATE).getBoolean("phone_deck_open", false))
        // On a Fold the app never rotates: each screen (front and inside) stays upright, auto-rotate or not.
        else requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        setContent {
            val windowLayout by remember { WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this) }
                .collectAsStateWithLifecycle<WindowLayoutInfo?>(initialValue = null)
            // Seeing a hinge at any point means this is a foldable, for good.
            LaunchedEffect(windowLayout) {
                if (windowLayout?.displayFeatures?.any { it is FoldingFeature } == true) {
                    getSharedPreferences("foldman", MODE_PRIVATE).edit().putBoolean(KEY_SEEN_HINGE, true).apply()
                    vm.setPhoneMode(false)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                }
            }
            val deckUi by vm.ui.collectAsStateWithLifecycle()
            LaunchedEffect(deckUi.phoneMode, deckUi.deckOpen) {
                if (deckUi.phoneMode) applyPhoneOrientation(deckUi.deckOpen)
            }
            // Foldy Streaman: with "Show the player on the lock screen" on, the front player stays
            // up over the lock screen. Only the player: the inside (tapes, settings) asks for the
            // phone to be unlocked first.
            LaunchedEffect(deckUi.lockScreen) { setShowWhenLocked(deckUi.lockScreen) }
            LaunchedEffect(deckUi.lockScreen, deckUi.coverShown) {
                // A moment's grace: while the screen is still working out which side is showing,
                // don't ask for the unlock by mistake.
                kotlinx.coroutines.delay(500)
                val guard = getSystemService(android.app.KeyguardManager::class.java)
                if (deckUi.lockScreen && !deckUi.coverShown && guard?.isKeyguardLocked == true) guard.requestDismissKeyguard(this@MainActivity, null)
            }
            // Foldy Streaman: a picture for a tape's J-card, from Android's photo picker.
            var photoForTape by rememberSaveable { mutableStateOf<String?>(null) }
            val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                val id = photoForTape
                if (uri != null && id != null) vm.setTapePhoto(id, uri)
                photoForTape = null
            }
            // Foldy Streaman: back up tapes to a file, and load them back.
            val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) vm.exportBackup(uri)
            }
            val backupLoader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) vm.importBackup(uri)
            }
            val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    vm.setFolder(uri)
                }
            }
            val appleSignIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                vm.onAppleSignInResult(result.data)
            }
            // Only for the media notification; playback works without it.
            val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                FoldmanScreen(
                    vm, windowLayout,
                    onPickFolder = { folderPicker.launch(null) },
                    onInstallApp = ::openStoreListing,
                    onAppleSignIn = { vm.appleSignInIntent()?.let(appleSignIn::launch) },
                    onBackup = { backupSaver.launch("foldy-streaman-tapes.json") },
                    onRestore = { backupLoader.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                    onPickPhoto = { id ->
                        photoForTape = id
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onScreenShown(true)
        // Notification access may have just been granted in Settings, or the streaming app started.
        vm.refresh()
    }

    /**
     * Whether this phone folds: Android's hinge sensor, Samsung's Fold / Flip model numbers, or a
     * hinge seen before. Foldables keep the open-to-change, close-to-play behaviour.
     */
    private fun isFoldable(): Boolean {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)) return true
        val model = Build.MODEL.uppercase()
        if (model.startsWith("SM-F9") || model.startsWith("SM-F7")) return true
        return getSharedPreferences("foldman", MODE_PRIVATE).getBoolean(KEY_SEEN_HINGE, false)
    }

    /** Phones only: closed = portrait, open = landscape, and turning the phone changes nothing. */
    private fun applyPhoneOrientation(open: Boolean) {
        requestedOrientation = if (open) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // Foldy Streaman: on the front screen, volume up = PLAY and volume down = PAUSE (unless that
    // is switched off in Settings, when they are plain volume buttons).
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.coverShown && vm.volumeKeysOn && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            // Holding the button counts as one press.
            if (event.repeatCount == 0) {
                vm.volumeButton(up = keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.coverShown && vm.volumeKeysOn && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        spotifyLoginFrom(intent)
    }

    /** Foldy Streaman: the browser coming back from "Log in with Spotify". */
    private fun spotifyLoginFrom(intent: Intent?) {
        val back = intent?.data ?: return
        if (back.scheme == "foldystreaman" && back.host == "spotify-login") {
            vm.finishSpotifyLogin(back)
            intent.data = null
        }
    }

    override fun onPause() {
        super.onPause()
        vm.onScreenShown(false)
    }

    private fun openStoreListing(source: Source) {
        val pkg = source.packageName ?: return
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        runCatching { startActivity(market) }.onFailure {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
    }

    private companion object {
        const val KEY_SEEN_HINGE = "seen_hinge"
    }
}
