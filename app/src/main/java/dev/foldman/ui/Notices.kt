// Foldy Streaman addition (not in the original Foldman): the phone's notifications shown on the
// front player itself, each theme in its own spot (in the tape window, turned on its side, in the
// middle of the round window, or in the dial strip). Nothing is stored: a notice lives in memory
// for the few seconds it is on screen.
package dev.foldman.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.NoticeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** One notification, as much of it as the player shows. */
class Notice(val key: String, val app: String, val title: String, val text: String, val icon: Bitmap?, val at: Long)

/** Where the notification listener drops what just arrived, for the player to pick up. */
object NoticeFeed {
    val latest = MutableStateFlow<Notice?>(null)
}

/** Settings: off, the app's name only, or the whole message. */
val LocalNoticeMode = staticCompositionLocalOf { NoticeMode.OFF }

/** Screen previews only: a notice to show as if it had just arrived. */
val LocalPreviewNotice = staticCompositionLocalOf<Notice?> { null }

/** How long a notice stays on the player. */
private const val NOTICE_MS = 5000L

/** The notice to show right now, if any: a new one stays up for a few seconds, then goes. */
@Composable
fun rememberNotice(): Notice? {
    LocalPreviewNotice.current?.let { return it }
    val mode = LocalNoticeMode.current
    val arrived by NoticeFeed.latest.collectAsState()
    var shown by remember { mutableStateOf<Notice?>(null) }
    LaunchedEffect(arrived, mode) {
        val n = arrived
        if (n == null || mode == NoticeMode.OFF || System.currentTimeMillis() - n.at > NOTICE_MS) {
            shown = null
        } else {
            // "App name only" keeps the message itself off the screen.
            shown = if (mode == NoticeMode.APP_ONLY) Notice(n.key, n.app, "New notification", "", n.icon, n.at) else n
            delay(NOTICE_MS)
            shown = null
        }
    }
    return shown
}

/** 0 = hidden, 1 = fully in, eased both ways; and the notice to draw (kept while it slides out). */
@Composable
fun noticeSlide(): Pair<Float, Notice?> {
    val now = rememberNotice()
    var last by remember { mutableStateOf<Notice?>(null) }
    if (now != null) last = now
    val preview = LocalPreviewNotice.current != null
    val t by animateFloatAsState(if (now != null) 1f else 0f, tween(if (preview) 0 else 480, easing = FastOutSlowInEasing), label = "notice")
    return (if (preview) 1f else t) to last
}

enum class NoticeStyle {
    /** Slides down into the tape window, which darkens behind it. */
    WINDOW,
    /** The same, turned a quarter turn to run up a tall narrow window. */
    SIDEWAYS,
    /** Fades into the middle of a round window. */
    ROUND,
}

/**
 * A notification over a theme's tape window: the window darkens and the notice comes in over it in
 * light lettering, then leaves the way it came. Put it last inside the window's own (clipped) box.
 */
@Composable
fun NoticeOverlay(style: NoticeStyle, modifier: Modifier = Modifier) {
    val (t, notice) = noticeSlide()
    if (t <= 0.001f || notice == null) return
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Color(0xFF050506).copy(alpha = 0.72f * t)))
        val centred = style == NoticeStyle.ROUND
        Column(
            Modifier
                .then(if (style == NoticeStyle.SIDEWAYS) Modifier.quarterTurn() else Modifier)
                .fillMaxSize()
                .graphicsLayer {
                    alpha = t
                    // Down from the top of the window; the round one grows out of the middle instead.
                    if (centred) { scaleX = 0.8f + 0.2f * t; scaleY = 0.8f + 0.2f * t } else translationY = -(1f - t) * size.height
                }
                .padding(if (centred) 30.dp else 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoticeIcon(notice, 18)
                Spacer(Modifier.width(6.dp))
                Text(
                    notice.app.uppercase(),
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, letterSpacing = 1.5.sp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                notice.title,
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, textAlign = if (centred) TextAlign.Center else TextAlign.Start),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (notice.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    notice.text,
                    style = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 17.sp, textAlign = if (centred) TextAlign.Center else TextAlign.Start),
                    maxLines = if (centred) 3 else 5, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The sending app's icon, or a plain dot when it couldn't be read. */
@Composable
fun NoticeIcon(notice: Notice, sizeDp: Int) {
    val bmp = notice.icon
    if (bmp != null) {
        Image(remember(bmp) { bmp.asImageBitmap() }, contentDescription = null, modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(4.dp)))
    } else {
        Box(Modifier.size((sizeDp * 0.6f).dp).clip(RoundedCornerShape(50)).background(Color(0xFFFF4A3A)))
    }
}

/** A box the shape of the Original theme's tape window stood on end, inset to its glass. */
fun Modifier.originalWindowOnEnd(): Modifier =
    aspectRatio(1f / TAPE_WINDOW_ASPECT, matchHeightConstraintsFirst = true).padding(5.dp).clip(RoundedCornerShape(10.dp))
