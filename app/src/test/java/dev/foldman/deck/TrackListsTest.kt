package dev.foldman.deck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Song matching: whole titles only, so a short title never picks up a longer one that contains it. */
class TrackListsTest {
    private val lloyd = listOf("Lloyd (Intro)", "You", "Certified", "Take You Home", "What You Wanna Do")

    @Test fun shortTitleIsNotPartOfALongerOne() {
        assertFalse(TrackLists.sameSong("You", "Take You Home"))
        assertEquals(1, TrackLists.bestMatch(lloyd, "You"))
        assertEquals(3, TrackLists.bestMatch(lloyd, "Take You Home"))
    }

    @Test fun youtubeExtrasAreIgnored() {
        assertEquals(1, TrackLists.bestMatch(lloyd, "Lloyd - You (Official Video)"))
        assertEquals(3, TrackLists.bestMatch(lloyd, "Take You Home [Remastered]"))
    }

    @Test fun noMatchGivesMinusOne() {
        assertEquals(-1, TrackLists.bestMatch(lloyd, "Fast Car"))
    }
}
