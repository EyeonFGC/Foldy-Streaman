// Foldy Streaman addition (not in the original Foldman): where the sound is going, and the
// system menu for changing it.
package dev.foldman.deck

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings

object AudioOutput {
    /** Device types that mean "this phone's own speaker". */
    private val builtIn = setOf(
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
        AudioDeviceInfo.TYPE_TELEPHONY,
    )

    /** Outputs that take the music away from the phone when connected. */
    private val external = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        AudioDeviceInfo.TYPE_AUX_LINE,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_DOCK,
    )

    /** Headphones, a speaker or the car are connected (Bluetooth, USB-C, wired...). */
    fun anyConnected(audio: AudioManager): Boolean =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in external }

    /**
     * Where the music that's playing (YT Music, Spotify, anything) actually comes out, read from
     * Android's list of active players: true = not this phone, false = this phone's speaker,
     * null = nothing playing right now, so it can't tell.
     */
    fun playingElsewhere(audio: AudioManager): Boolean? {
        val players = runCatching { audio.activePlaybackConfigurations }.getOrNull() ?: return null
        val music = players.filter {
            val usage = it.audioAttributes.usage
            usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME || usage == AudioAttributes.USAGE_UNKNOWN
        }
        val outputs = music.mapNotNull { p -> runCatching { p.audioDeviceInfo }.getOrNull() }
        if (outputs.isEmpty()) return null
        return outputs.any { it.type !in builtIn }
    }

    /**
     * Opens the menu for choosing where the sound plays. Android 14+: the official output
     * switcher. Older: the media output panel. Last resort: Bluetooth settings.
     */
    fun openSwitcher(context: Context, playingPackage: String?) {
        if (Build.VERSION.SDK_INT >= 34) {
            val shown = runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
            if (shown) return
        }
        val panel = Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
            .putExtra("com.android.settings.panel.extra.PACKAGE_NAME", playingPackage ?: context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(panel) }.isSuccess) return
        runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
