package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic
import de.martinkloeckner.rezeptbuch.domain.WhiteBalance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SlotLogicTest {
    @Test fun assignMovesRecipeFromOldSlot() {
        val s = SlotLogic.assign(mapOf("PP1" to "a"), "PP3", "a")
        assertEquals(mapOf("PP3" to "a"), s)
    }

    @Test fun assignReplacesOccupant() {
        val s = SlotLogic.assign(mapOf("PP1" to "a", "PP2" to "b"), "PP1", "b")
        assertEquals(mapOf("PP1" to "b"), s)
    }

    @Test fun assignNullFreesSlot() {
        assertEquals(emptyMap(), SlotLogic.assign(mapOf("PP1" to "a"), "PP1", null))
    }

    @Test fun unknownSlotRejected() {
        assertFailsWith<IllegalArgumentException> { SlotLogic.assign(emptyMap(), "PP11", "a") }
    }

    @Test fun releaseAndLookup() {
        val slots = mapOf("PP2" to "a", "PP5" to "b")
        assertEquals("PP2", SlotLogic.slotOf(slots, "a"))
        assertNull(SlotLogic.slotOf(slots, "zzz"))
        assertEquals(mapOf("PP5" to "b"), SlotLogic.release(slots, "a"))
    }

    @Test fun sanitizeDropsUnknownAndDuplicates() {
        val out = SlotLogic.sanitize(mapOf("PP1" to "a", "PP2" to "a", "PP3" to "ghost", "PPX" to "b"), setOf("a", "b"))
        assertEquals(mapOf("PP1" to "a"), out)
    }

    @Test fun shareTextListsAllTenSlots() {
        val gold = Recipe(id = "kodak-gold", name = "Kodak Gold", wb = WhiteBalance(8000, -3.0, -1.75))
        val lines = SlotLogic.shareText(mapOf("PP1" to "kodak-gold"), listOf(gold)).lines()
        assertEquals(10, lines.size)
        assertEquals("PP1: Kodak Gold — WB 8000 K · B3 / M1.75", lines[0])
        assertEquals("PP2: frei", lines[1])
        assertEquals("PP10: frei", lines[9])
    }
}
