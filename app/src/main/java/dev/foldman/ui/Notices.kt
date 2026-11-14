// Foldy Streaman addition (not in the original Foldman): the phone's notifications shown on the
// front player itself, each theme in its own spot (in the tape window, turned on its side, in the
// middle of the round window, or in the dial strip). Nothing is stored: a notice lives in memory
// for the few seconds it is on screen.
package dev.foldman.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

/**
 * 0 = hidden, 1 = in place; and the notice to draw (kept while it leaves). Coming in is quick with
 * a little bounce as it lands (so the value runs slightly past 1 and settles); leaving, it first
 * dips a touch further in, then shoots back out the way it came.
 */
@Composable
fun noticeSlide(): Pair<Float, Notice?> {
    val now = rememberNotice()
    var last by remember { mutableStateOf<Notice?>(null) }
    if (now != null) last = now
    if (LocalPreviewNotice.current != null) return 1f to last
    val t = remember { Animatable(0f) }
    LaunchedEffect(now != null) {
        if (now != null) {
            t.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 420f))
        } else if (t.value > 0f) {
            t.animateTo(1.07f, tween(130, easing = FastOutSlowInEasing))
            t.animateTo(0f, tween(260, easing = FastOutLinearInEasing))
        }
    }
    return t.value to last
}

enum class NoticeStyle {
    /** A dark panel across the top of the tape window, dropping in from above. */
    TOP,
    /** A dark panel across the bottom of the tape window, rising in from below. */
    BOTTOM,
    /** Turned a quarter turn to run up a tall narrow window, sliding in from the right. */
    SIDEWAYS,
    /** Zooms out of the middle of a round window. */
    ROUND,
}

/**
 * A notification over a theme's tape window, in light lettering on a dark panel. [share] is how
 * much of the window the panel takes (for TOP and BOTTOM). Put it last inside the window's own
 * (clipped) box.
 */
@Composable
fun NoticeOverlay(style: NoticeStyle, modifier: Modifier = Modifier, share: Float = 0.33f) {
    val (t, notice) = noticeSlide()
    if (t <= 0.001f || notice == null) return
    val seen = t.coerceIn(0f, 1f)
    val dark = Color(0xFF050506)
    Box(modifier.clipToBounds()) {
        when (style) {
            NoticeStyle.ROUND -> {
                Box(Modifier.fillMaxSize().background(dark.copy(alpha = 0.74f * seen)))
                NoticeText(
                    notice, centred = true, lines = 3, big = true,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = t; scaleY = t; alpha = seen }.padding(30.dp),
                )
            }
            NoticeStyle.SIDEWAYS -> {
                // Stood on its side: in the turned panel's own terms "down" is the screen's right,
                // so it comes in from the right-hand edge of the window.
                NoticeText(
                    notice, centred = false, lines = 2, big = true,
                    modifier = Modifier
                        .quarterTurn()
                        .fillMaxSize()
                        .graphicsLayer { translationY = (1f - t) * size.height }
                        .drawBehind { drawRect(dark.copy(alpha = 0.78f), Offset(0f, -size.height * 0.2f), Size(size.width, size.height * 1.4f)) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            NoticeStyle.TOP, NoticeStyle.BOTTOM -> {
                val top = style == NoticeStyle.TOP
                NoticeText(
                    notice, centred = false, lines = 2, big = false,
                    modifier = Modifier
                        .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(share)
                        .graphicsLayer { translationY = (if (top) -1f else 1f) * (1f - t) * size.height }
                        .drawBehind {
                            // The panel carries on past the window's edge, so its bounce never shows a gap,
                            // and fades out along its inner edge.
                            val fade = size.height * 0.16f
                            if (top) {
                                drawRect(dark.copy(alpha = 0.82f), Offset(0f, -size.height * 0.3f), Size(size.width, size.height * 1.3f - fade))
                                drawRect(Brush.verticalGradient(listOf(dark.copy(alpha = 0.82f), Color.Transparent), size.height - fade, size.height), Offset(0f, size.height - fade), Size(size.width, fade))
                            } else {
                                drawRect(dark.copy(alpha = 0.82f), Offset(0f, fade), Size(size.width, size.height * 1.3f - fade))
                                drawRect(Brush.verticalGradient(listOf(Color.Transparent, dark.copy(alpha = 0.82f)), 0f, fade), Offset.Zero, Size(size.width, fade))
                            }
                        }
                        .padding(start = 12.dp, end = 12.dp, top = if (top) 8.dp else 12.dp, bottom = if (top) 12.dp else 8.dp),
                )
            }
        }
    }
}

/** The notice itself: who it's from, its title and the start of its message. */
@Composable
private fun NoticeText(notice: Notice, centred: Boolean, lines: Int, big: Boolean, modifier: Modifier) {
    val align = if (centred) TextAlign.Center else TextAlign.Start
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NoticeIcon(notice, if (big) 18 else 14)
            Spacer(Modifier.width(6.dp))
            Text(
                notice.app.uppercase(),
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f), fontSize = if (big) 9.sp else 8.sp, letterSpacing = 1.5.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(if (big) 6.dp else 3.dp))
        Text(
            notice.title,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White, fontSize = if (big) 16.sp else 13.sp, textAlign = align),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (notice.text.isNotBlank()) {
            Spacer(Modifier.height(if (big) 4.dp else 1.dp))
            Text(
                notice.text,
                style = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White.copy(alpha = 0.92f), fontSize = if (big) 13.sp else 11.sp, lineHeight = if (big) 17.sp else 14.sp, textAlign = align),
                maxLines = lines, overflow = TextOverflow.Ellipsis,
            )
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
