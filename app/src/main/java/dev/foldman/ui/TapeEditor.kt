// Foldy Streaman addition (not in the original Foldman): making and decorating your own tapes.
package dev.foldman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.CustomTape
import dev.foldman.deck.TapeColors
import dev.foldman.deck.playlistIdFrom

/** What the rack needs to make, load, change and remove your tapes. */
class TapeActions(
    val load: (CustomTape) -> Unit,
    val save: (CustomTape) -> Unit,
    val delete: (String) -> Unit,
)

private val HandFont = TextStyle(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold)

/**
 * The editor for one tape. [tape] null = a new blank tape. Paste a YouTube Music (or YouTube)
 * playlist link, write the label, and pick the shell and label colours.
 */
@Composable
fun TapeEditorDialog(tape: CustomTape?, actions: TapeActions, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(tape?.title ?: "") }
    var link by remember { mutableStateOf(tape?.let { "https://music.youtube.com/playlist?list=${it.playlistId}" } ?: "") }
    var shell by remember { mutableLongStateOf(tape?.shell ?: TapeColors.shells[1]) }
    var label by remember { mutableLongStateOf(tape?.label ?: TapeColors.labels[0]) }
    var confirmDelete by remember { mutableStateOf(false) }
    val playlistId = playlistIdFrom(link)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tape == null) "New tape" else "Edit tape") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // A live preview of the spine as it will sit on the rack.
                TapeSpineView(
                    title = title.ifBlank { "my tape" },
                    shell = Color(shell),
                    label = Color(label),
                    trailing = null,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(40) },
                    label = { Text("Write on the label") },
                    singleLine = true,
                    textStyle = HandFont.copy(fontSize = 20.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("YouTube Music playlist link") },
                    placeholder = { Text("Share → Copy link, then paste here") },
                    singleLine = true,
                    isError = link.isNotBlank() && playlistId == null,
                    supportingText = {
                        Text(
                            when {
                                link.isBlank() -> "In YT Music open the playlist, tap ⋮ → Share → Copy link."
                                playlistId == null -> "That doesn't look like a playlist link (it needs list=… in it)."
                                else -> "Playlist found ✓"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Shell", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                Swatches(TapeColors.shells, shell) { shell = it }
                Spacer(Modifier.height(8.dp))
                Text("Label", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                Swatches(TapeColors.labels, label) { label = it }
                if (tape != null) {
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = {
                        if (confirmDelete) {
                            actions.delete(tape.id)
                            onDismiss()
                        } else confirmDelete = true
                    }) {
                        Text(if (confirmDelete) "Tap again to throw this tape away" else "Delete tape", color = Color(0xFFD0453A))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = playlistId != null,
                onClick = {
                    val id = playlistId ?: return@TextButton
                    val saved = (tape ?: CustomTape(title = "", playlistId = id, shell = shell, label = label))
                        .copy(title = title.trim(), playlistId = id, shell = shell, label = label)
                    actions.save(saved)
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Swatches(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { c ->
            val on = c == selected
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (on) 3.dp else 1.dp, if (on) DeckColors.Accent else Color.Black.copy(alpha = 0.25f), CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

/** A cassette spine: shell-coloured edge with a paper strip and your handwriting on it. */
@Composable
fun TapeSpineView(
    title: String,
    shell: Color,
    label: Color,
    trailing: String?,
    modifier: Modifier = Modifier,
    trailingColor: Color = Color.White.copy(alpha = 0.7f),
) {
    val ink = if (label.red * 0.2126f + label.green * 0.7152f + label.blue * 0.0722f > 0.45f) DeckColors.Ink else DeckColors.Paper
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(lerp(shell, Color.White, 0.15f), shell, lerp(shell, Color.Black, 0.25f))))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(2.dp))
                .background(label)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        ) {
            Text(title, style = HandFont.copy(fontSize = 18.sp, color = ink, fontWeight = FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.size(10.dp))
            Text(trailing, style = EngravedCaps.copy(color = trailingColor, fontSize = 8.sp))
        }
    }
}
