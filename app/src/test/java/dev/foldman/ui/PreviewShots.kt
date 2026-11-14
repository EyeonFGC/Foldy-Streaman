package dev.foldman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.foldman.deck.CustomTape
import dev.foldman.deck.DeckUi
import dev.foldman.deck.Source
import dev.foldman.deck.SourceStatus
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PreviewShots {
    @get:Rule val rule = createComposeRule()

    private val tapes = listOf(
        CustomTape(title = "Road trip 2026", playlistId = "PLabc1234567", shell = 0xFFB8322A, label = 0xFFF2EBDC),
        CustomTape(title = "Stream hype", playlistId = "PLdef1234567", shell = 0xFF1F5FA8, label = 0xFFFFE58A),
        CustomTape(title = "Chill beats", playlistId = "PLghi1234567", shell = 0xFF2E8B57, label = 0xFFC9F2C7),
    )
    private val ui = DeckUi(
        source = Source.YT_MUSIC, status = SourceStatus.Ready, tapes = tapes, tape = tapes[0], tapeCued = true,
    )
    private val deck = DeckControls({}, {}, {}, {}, {}, {}, {}, {})
    private val actions = ShelfActions(
        selectSource = {}, pickFolder = {}, playTrack = {}, openAccess = {}, openApp = {}, installApp = {},
        apple = AppleActions({}, {}, {}, {}, {}, {}, {}),
        tapes = TapeActions({}, {}, {}),
    )
    private fun anim() = DeckAnimations().also { runBlocking { it.insert.snapTo(0f) } }

    @Test
    @Config(qualifiers = "w740dp-h830dp-xhdpi")
    fun unfolded() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp))
        }
        rule.onRoot().captureRoboImage("build/shots/unfolded.png")
    }

    @Test
    @Config(qualifiers = "w740dp-h830dp-xhdpi")
    fun draggingOverRack() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to false)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-rack.png")
    }

    @Test
    @Config(qualifiers = "w740dp-h830dp-xhdpi")
    fun draggingOverDeck() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to true)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-deck.png")
    }

    @Test
    @Config(qualifiers = "w380dp-h860dp-xhdpi")
    fun folded() {
        rule.setContent {
            CoverWalkman(ui.copy(tapeCued = false), anim(), deck, {}, Modifier.fillMaxSize().background(Color.Black))
        }
        rule.onRoot().captureRoboImage("build/shots/folded.png")
    }

    @Test
    @Config(qualifiers = "w380dp-h860dp-xhdpi")
    fun foldedNeedsAccess() {
        rule.setContent {
            CoverWalkman(ui.copy(status = SourceStatus.NeedsAccess), anim(), deck, {}, Modifier.fillMaxSize().background(Color.Black))
        }
        rule.onRoot().captureRoboImage("build/shots/folded-access.png")
    }
}
