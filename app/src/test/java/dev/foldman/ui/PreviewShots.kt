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
import dev.foldman.deck.DeckTheme
import dev.foldman.deck.TapeFont
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

    /** A made-up picture for the J-card: a sunset over hills. */
    private val photo: String by lazy {
        val bmp = android.graphics.Bitmap.createBitmap(1200, 800, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        p.shader = android.graphics.LinearGradient(0f, 0f, 0f, 800f, intArrayOf(0xFF2B1B5A.toInt(), 0xFFE0567A.toInt(), 0xFFFFB347.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, 1200f, 800f, p)
        p.shader = null
        p.color = 0xFFFFE08A.toInt(); c.drawCircle(780f, 520f, 120f, p)
        p.color = 0xFF1E1436.toInt()
        val hills = android.graphics.Path().apply { moveTo(0f, 620f); quadTo(300f, 480f, 600f, 600f); quadTo(900f, 700f, 1200f, 560f); lineTo(1200f, 800f); lineTo(0f, 800f); close() }
        c.drawPath(hills, p)
        val f = java.io.File.createTempFile("jcard", ".png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        f.absolutePath
    }

    private val tapes by lazy {
        listOf(
            CustomTape(title = "Road trip 2026", playlistId = "PLabc1234567", shell = 0xFFB8322A, label = 0xFFF2EBDC, font = TapeFont.MARKER, photoPath = photo, photoY = 0.3f, photoZoom = 1.2f),
            CustomTape(title = "Stream hype", playlistId = "PLdef1234567", shell = 0xFF1F5FA8, label = 0xFFFFE58A, font = TapeFont.TYPE_WORN, photoPath = photo, photoY = -0.4f),
            CustomTape(title = "Chill beats", playlistId = "PLghi1234567", shell = 0xFF2E8B57, label = 0xFFC9F2C7, font = TapeFont.PEN),
        )
    }
    private val ui get() = DeckUi(
        source = Source.YT_MUSIC, status = SourceStatus.Ready, tapes = tapes, tape = tapes[0], tapeCued = true,
    )
    private val deck = DeckControls({}, {}, {}, {}, {}, {}, {}, {})
    private val actions = ShelfActions(
        selectSource = {}, pickFolder = {}, playTrack = {}, openAccess = {}, openApp = {}, installApp = {},
        apple = AppleActions({}, {}, {}, {}, {}, {}, {}),
        tapes = TapeActions({}, {}, {}, {}),
    )
    private fun anim() = DeckAnimations().also { runBlocking { it.insert.snapTo(0f) } }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun unfolded() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp))
        }
        rule.onRoot().captureRoboImage("build/shots/unfolded.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun unfoldedEmpty() {
        rule.setContent {
            RackDeckLayout(ui.copy(empty = true, tape = null), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp))
        }
        rule.onRoot().captureRoboImage("build/shots/unfolded-empty.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun draggingOverRack() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to false)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-rack.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun draggingOverDeck() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to true)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-deck.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun folded() {
        rule.setContent {
            CoverWalkman(ui.copy(tapeCued = false), anim(), deck, {}, Modifier.fillMaxSize().background(Color.Black))
        }
        rule.onRoot().captureRoboImage("build/shots/folded.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedNeedsAccess() {
        rule.setContent {
            CoverWalkman(ui.copy(status = SourceStatus.NeedsAccess), anim(), deck, {}, Modifier.fillMaxSize().background(Color.Black))
        }
        rule.onRoot().captureRoboImage("build/shots/folded-access.png")
    }

    private val gold = ui.copy(theme = DeckTheme.BLACK_GOLD, tapeCued = false)

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedBlackGold() {
        rule.setContent { CoverBlackGold(gold, anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-blackgold.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedBlackGoldEmpty() {
        rule.setContent { CoverBlackGold(gold.copy(empty = true, tape = null), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-blackgold-empty.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun draggingOverFullDeck() {
        rule.setContent {
            RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to true)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-full-deck.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun draggingIntoEmptyGold() {
        rule.setContent {
            RackDeckLayout(gold.copy(empty = true, tape = null), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewDrag = 4 to true)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/drag-empty-gold.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun settings() {
        rule.setContent {
            RackDeckLayout(gold, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewSettings = true)
        }
        rule.onRoot().captureRoboImage("build/shots/settings.png")
    }

    // Normal phone (Samsung Note 20 Ultra): closed = portrait, open = landscape.
    private val phone = ui.copy(phoneMode = true, tapeCued = false)

    @Test
    @Config(qualifiers = "w412dp-h883dp-xxhdpi")
    fun phoneClosed() {
        rule.setContent { CoverWalkman(phone, anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/phone-closed.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h883dp-xxhdpi")
    fun phoneClosedBlackGold() {
        rule.setContent { CoverBlackGold(phone.copy(theme = DeckTheme.BLACK_GOLD), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/phone-closed-blackgold.png")
    }

    @Test
    @Config(qualifiers = "w883dp-h412dp-land-xxhdpi")
    fun phoneOpen() {
        rule.setContent {
            RackDeckLayout(phone.copy(deckOpen = true), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(12.dp), onClose = {})
        }
        rule.onRoot().captureRoboImage("build/shots/phone-open.png")
    }

    @Test
    @Config(qualifiers = "w883dp-h412dp-land-xxhdpi")
    fun phoneOpenEmpty() {
        rule.setContent {
            RackDeckLayout(phone.copy(deckOpen = true, empty = true, tape = null), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(12.dp), onClose = {})
        }
        rule.onRoot().captureRoboImage("build/shots/phone-open-empty.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun jcard() {
        rule.setContent {
            RackDeckLayout(ui.copy(title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewCard = 4)
        }
        // Give the picture a moment to load before taking the shot.
        repeat(5) { Thread.sleep(200); rule.mainClock.advanceTimeBy(200); rule.waitForIdle() }
        rule.onRoot().captureRoboImage("build/shots/jcard.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun unfoldedPlaying() {
        rule.setContent {
            RackDeckLayout(ui.copy(title = "Fast Car", artist = "Tracy Chapman", tapeCued = false), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp))
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/unfolded-playing.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun jackPlugged() {
        rule.setContent { CoverWalkman(ui.copy(audioElsewhere = true, tapeCued = false), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/jack-plugged.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun jackPluggedGold() {
        rule.setContent { CoverBlackGold(gold.copy(audioElsewhere = true), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/jack-plugged-gold.png")
    }

    private fun shade(angle: Float, name: String) {
        rule.setContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                RackDeckLayout(ui, anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp))
                HingeShade(verticalFold = true, modifier = Modifier.fillMaxSize(), previewAngle = angle)
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun shade120() = shade(120f, "shade-120")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun shade80() = shade(80f, "shade-080")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun shade40() = shade(40f, "shade-040")

    @Test
    @Config(qualifiers = "w883dp-h412dp-land-xxhdpi")
    fun phoneOpenPlaying() {
        rule.setContent {
            RackDeckLayout(phone.copy(deckOpen = true, title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(12.dp), onClose = {})
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/phone-open-playing.png")
    }

    @Test
    @Config(qualifiers = "w720dp-h900dp-xxhdpi")
    fun buttonStyles() {
        rule.setContent {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(Color(0xFF141210)).padding(12.dp)) {
                listOf(JButtonStyle.METAL, JButtonStyle.ENAMEL, JButtonStyle.STAMP).forEachIndexed { i, st ->
                    androidx.compose.material3.Text(
                        "${'A' + i}: " + listOf("Metal keys", "Enamel badges", "Rubber stamp")[i],
                        color = Color.White, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    )
                    androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                        CardButtonSample(st, DeckTheme.ORIGINAL, Modifier.weight(1f))
                        CardButtonSample(st, DeckTheme.BLACK_GOLD, Modifier.weight(1f))
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("build/shots/button-styles.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun frontShelfOn() {
        rule.setContent { CoverWalkman(ui.copy(frontShelf = true, tapeCued = false), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/front-shelf-on.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun frontShelfOnGold() {
        rule.setContent { CoverBlackGold(gold.copy(frontShelf = true), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/front-shelf-on-gold.png")
    }

    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun jcardGold() {
        rule.setContent {
            RackDeckLayout(gold.copy(title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, actions, Modifier.fillMaxSize().background(DeckColors.Backdrop).padding(16.dp), previewCard = 5)
        }
        repeat(5) { Thread.sleep(200); rule.mainClock.advanceTimeBy(200); rule.waitForIdle() }
        rule.onRoot().captureRoboImage("build/shots/jcard-gold.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedCherryRed() {
        rule.setContent { CoverCherryRed(ui.copy(theme = DeckTheme.CHERRY_RED, tapeCued = false, title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-cherry.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedCherryRedPlugged() {
        rule.setContent { CoverCherryRed(ui.copy(theme = DeckTheme.CHERRY_RED, tapeCued = false, audioElsewhere = true), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-cherry-plugged.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedSilverBlue() {
        rule.setContent { CoverSilverBlue(ui.copy(theme = DeckTheme.SILVER_BLUE, tapeCued = false, title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-silverblue.png")
    }

    private fun inside(theme: DeckTheme, name: String) {
        // No pictures here: the preview tool crashes drawing them in this layout (phones are fine).
        val plain = tapes.map { it.copy(photoPath = null) }
        val u = ui.copy(theme = theme, tape = plain[0], tapes = plain, tapeCued = false, title = "Fast Car", artist = "Tracy Chapman")
        rule.setContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().insideBody(theme)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) { PianoHinge(theme = theme) }
                RackDeckLayout(u, anim(), deck, actions, Modifier.fillMaxSize().padding(16.dp))
            }
        }
        repeat(5) { Thread.sleep(200); rule.mainClock.advanceTimeBy(200); rule.waitForIdle() }
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun insideOriginal() = inside(DeckTheme.ORIGINAL, "inside-original")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun insideBlackGold() = inside(DeckTheme.BLACK_GOLD, "inside-blackgold")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun insideCherry() = inside(DeckTheme.CHERRY_RED, "inside-cherry")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun insideSilver() = inside(DeckTheme.SILVER_BLUE, "inside-silver")

    /** Frames of the J-card sliding in (plain tapes: the preview tool can't draw pictures here). */
    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun jcardSlide() {
        val plain = tapes.map { it.copy(photoPath = null) }
        val u = ui.copy(tapes = plain, tape = plain[0], tapeCued = false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().insideBody(DeckTheme.ORIGINAL)) {
                RackDeckLayout(u, anim(), deck, actions, Modifier.fillMaxSize().padding(16.dp), previewCard = 5)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        var t = 0L
        for (ms in listOf(60L, 140L, 260L, 600L)) {
            rule.mainClock.advanceTimeBy(ms - t); t = ms
            rule.onRoot().captureRoboImage("build/shots/slide-$ms.png")
        }
    }

    /** Dragging "Chill beats" up the rack: the marker line shows where it will land. */
    @Test
    @Config(qualifiers = "w832dp-h628dp-xxhdpi")
    fun reorder() {
        val plain = tapes.map { it.copy(photoPath = null) }
        val u = ui.copy(tapes = plain, tape = plain[0], tapeCued = false)
        rule.setContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().insideBody(DeckTheme.ORIGINAL)) {
                RackDeckLayout(u, anim(), deck, actions, Modifier.fillMaxSize().padding(16.dp), previewReorder = 6 to 0.5f)
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/reorder.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun foldedRedChrome() {
        rule.setContent { CoverRedChrome(ui.copy(theme = DeckTheme.RED_CHROME, tapeCued = false, title = "Fast Car", artist = "Tracy Chapman"), anim(), deck, {}, Modifier.fillMaxSize()) }
        rule.onRoot().captureRoboImage("build/shots/folded-redchrome.png")
    }

    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun insideRedChrome() = inside(DeckTheme.RED_CHROME, "inside-redchrome")

    /** Every theme with PLAY latched down, to compare raised keys with a pressed one. */
    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun keysOriginal() { val u = ui.copy(tapeCued = false, transport = dev.foldman.deck.Transport.PLAYING); rule.setContent { CoverWalkman(u, anim(), deck, {}, Modifier.fillMaxSize()) }; rule.onRoot().captureRoboImage("build/shots/keys-1.png") }
    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun keysBlackGold() { val u = gold.copy(transport = dev.foldman.deck.Transport.PLAYING); rule.setContent { CoverBlackGold(u, anim(), deck, {}, Modifier.fillMaxSize()) }; rule.onRoot().captureRoboImage("build/shots/keys-2.png") }
    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun keysCherry() { val u = ui.copy(theme = DeckTheme.CHERRY_RED, tapeCued = false, transport = dev.foldman.deck.Transport.PLAYING, title = "I Don't Mind", artist = "Lloyd"); rule.setContent { CoverCherryRed(u, anim(), deck, {}, Modifier.fillMaxSize()) }; rule.onRoot().captureRoboImage("build/shots/keys-3.png") }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun chargingCherry() { val u = ui.copy(theme = DeckTheme.CHERRY_RED, tapeCued = false, audioElsewhere = true); rule.setContent { androidx.compose.runtime.CompositionLocalProvider(LocalPreviewCharging provides true) { CoverCherryRed(u, anim(), deck, {}, Modifier.fillMaxSize()) } }; rule.onRoot().captureRoboImage("build/shots/charging-cherry.png") }

    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun keysSilver() { val u = ui.copy(theme = DeckTheme.SILVER_BLUE, tapeCued = false, transport = dev.foldman.deck.Transport.PLAYING); rule.setContent { CoverSilverBlue(u, anim(), deck, {}, Modifier.fillMaxSize()) }; rule.onRoot().captureRoboImage("build/shots/keys-4.png") }
    @Test
    @Config(qualifiers = "w411dp-h650dp-xxhdpi")
    fun keysRedChrome() { val u = ui.copy(theme = DeckTheme.RED_CHROME, tapeCued = false, transport = dev.foldman.deck.Transport.PLAYING); rule.setContent { CoverRedChrome(u, anim(), deck, {}, Modifier.fillMaxSize()) }; rule.onRoot().captureRoboImage("build/shots/keys-5.png") }

    private fun drawer(theme: DeckTheme, name: String) {
        val plain = tapes.map { it.copy(photoPath = null) }
        val u = ui.copy(theme = theme, tapes = plain, tape = plain[0], tapeCued = false, frontShelf = true, title = "Fast Car", artist = "Tracy Chapman")
        rule.setContent { TapeShelf(u, actions, Modifier.fillMaxSize(), settings = SettingsActions.None) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test @Config(qualifiers = "w411dp-h520dp-xxhdpi") fun drawerOriginal() = drawer(DeckTheme.ORIGINAL, "drawer-1")
    @Test @Config(qualifiers = "w411dp-h520dp-xxhdpi") fun drawerGold() = drawer(DeckTheme.BLACK_GOLD, "drawer-2")
    @Test @Config(qualifiers = "w411dp-h520dp-xxhdpi") fun drawerCherry() = drawer(DeckTheme.CHERRY_RED, "drawer-3")
    @Test @Config(qualifiers = "w411dp-h520dp-xxhdpi") fun drawerSilver() = drawer(DeckTheme.SILVER_BLUE, "drawer-4")
    @Test @Config(qualifiers = "w411dp-h520dp-xxhdpi") fun drawerRedChrome() = drawer(DeckTheme.RED_CHROME, "drawer-5")

    private fun card(theme: DeckTheme, dark: Boolean, name: String, blank: Boolean = false) {
        val plain = tapes.map { it.copy(photoPath = null) } + CustomTape(title = "", playlistId = "", shell = 0xFFB8322A, label = 0xFFF2EBDC)
        val u = ui.copy(theme = theme, tapes = plain, tape = plain[0], tapeCued = false, jcardDark = dark)
        rule.setContent {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().insideBody(theme)) {
                RackDeckLayout(u, anim(), deck, actions, Modifier.fillMaxSize().padding(16.dp), previewCard = if (blank) 7 else 5)
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun cardLight() = card(DeckTheme.ORIGINAL, false, "card-light")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun cardDark() = card(DeckTheme.BLACK_GOLD, true, "card-dark")
    @Test @Config(qualifiers = "w832dp-h628dp-xxhdpi") fun cardBlank() = card(DeckTheme.CHERRY_RED, false, "card-blank", blank = true)
}
