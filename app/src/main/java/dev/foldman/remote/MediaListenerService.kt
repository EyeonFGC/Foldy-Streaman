package dev.foldman.remote

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import dev.foldman.ui.Notice
import dev.foldman.ui.NoticeFeed

/**
 * Android only lets apps with an enabled notification listener read other apps' media sessions,
 * which is how Foldman follows YT Music and Apple Music.
 *
 * Foldy Streaman: it also hands each new notification to the front player, which can show it for
 * a few seconds (Settings > Notifications on the player; off unless switched on). Nothing is kept.
 */
class MediaListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        runCatching {
            if (sbn.packageName == packageName || sbn.isOngoing) return
            val n = sbn.notification
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
            // Music players' own "now playing" cards aren't news.
            if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return
            // Only the ones the phone itself would pop up or sound for, not the silent kind.
            val rank = Ranking()
            if (rankingMap.getRanking(sbn.key, rank) && rank.importance < NotificationManager.IMPORTANCE_DEFAULT) return
            val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            if (title.isEmpty() && text.isEmpty()) return
            @Suppress("DEPRECATION")
            val info = n.extras.getParcelable<ApplicationInfo>("android.appInfo")
            val app = info?.let { runCatching { it.loadLabel(packageManager).toString() }.getOrNull() } ?: sbn.packageName
            val icon: Bitmap? = info?.let { runCatching { it.loadIcon(packageManager).toBitmap(96, 96) }.getOrNull() }
            NoticeFeed.latest.value = Notice(sbn.key, app, title.ifEmpty { app }, text, icon, System.currentTimeMillis())
        }
    }
}
