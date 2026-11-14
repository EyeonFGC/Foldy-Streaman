package dev.foldman.deck

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The app's engine must start without crashing (a fresh install, nothing saved yet). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartupTest {
    @Test
    fun viewModelStarts() {
        val vm = DeckViewModel(RuntimeEnvironment.getApplication())
        check(vm.ui.value.hiddenSources.isEmpty())
    }
}
