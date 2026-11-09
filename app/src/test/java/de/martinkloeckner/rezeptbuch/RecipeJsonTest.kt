package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.data.RecipeJson
import de.martinkloeckner.rezeptbuch.domain.Library
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeJsonTest {
    private val starter: String by lazy { File("../data/recipes.json").readText() }

    @Test fun starterLibraryHasEightRecipes() {
        val lib = RecipeJson.parseLibrary(starter)
        assertEquals(8, lib.recipes.size)
        assertTrue(lib.slots.isEmpty())
        assertEquals(8, lib.recipes.map { it.id }.distinct().size)
    }

    @Test fun kodakGoldValues() {
        val gold = RecipeJson.parseLibrary(starter).recipes.first { it.id == "kodak-gold" }
        assertEquals("Movie", gold.pp.gamma)
        assertEquals(-5, gold.pp.blackLevel)
        assertEquals("Wide", gold.pp.blackGamma.range)
        assertEquals(75.0, gold.pp.knee.point)
        assertEquals(-4, gold.pp.depth.r)
        assertEquals("Type3", gold.pp.detail.bw)
        assertEquals(8000, gold.wb.kelvin)
        assertEquals(-3.0, gold.wb.ab)
        assertEquals(-1.75, gold.wb.gm)
    }

    @Test fun emptyStringsBecomeNull() {
        val ev = RecipeJson.parseLibrary(starter).recipes.first { it.id == "ev-pro-plus" }
        assertEquals("Auto", ev.pp.knee.mode)
        assertNull(ev.pp.knee.point)
        assertNull(ev.pp.knee.slope)
    }

    @Test fun fractionalKneePoint() {
        val v = RecipeJson.parseLibrary(starter).recipes.first { it.id == "vektro-100" }
        assertEquals(92.5, v.pp.knee.point)
    }

    @Test fun roundTripKeepsEverything() {
        val lib = RecipeJson.parseLibrary(starter)
        val withSlots = Library(lib.recipes, mapOf("PP1" to "kodak-gold", "PP4" to "fuji-400h"))
        val again = RecipeJson.parseLibrary(RecipeJson.writeLibrary(withSlots))
        assertEquals(withSlots.recipes, again.recipes)
        assertEquals(withSlots.slots, again.slots)
    }

    @Test fun exportUsesEmptyStringForUnset() {
        val lib = RecipeJson.parseLibrary(starter)
        val text = RecipeJson.writeLibrary(lib)
        assertTrue("\"point\": \"\"" in text)
        assertTrue("\"schemaVersion\": 1" in text)
    }

    @Test fun toleratesNumbersAsStringsAndMissingParts() {
        val lib = RecipeJson.parseLibrary(
            """{"recipes":[{"name":"Mein Rezept","pp":{"blackLevel":"-3","saturation":"+4"},"wb":{"kelvin":"5600","ab":"1,5"}}]}""",
        )
        val r = lib.recipes.single()
        assertEquals("mein-rezept", r.id)
        assertEquals(-3, r.pp.blackLevel)
        assertEquals(4, r.pp.saturation)
        assertEquals(5600, r.wb.kelvin)
        assertEquals(1.5, r.wb.ab)
        assertEquals(0.0, r.wb.gm)
    }

    @Test fun invalidInputThrowsFormatException() {
        assertFailsWith<RecipeJson.FormatException> { RecipeJson.parseLibrary("no json") }
        assertFailsWith<RecipeJson.FormatException> { RecipeJson.parseLibrary("""{"foo":1}""") }
    }

    @Test fun slotsOfUnknownRecipesAreDropped() {
        val lib = RecipeJson.parseLibrary("""{"recipes":[{"id":"a","name":"A"}],"slots":{"PP1":"a","PP2":"ghost"}}""")
        assertEquals(mapOf("PP1" to "a"), lib.slots)
    }

    @Test fun slugFoldsUmlautsAndSymbols() {
        assertEquals("ev-pro", RecipeJson.slug("Ev Pro+"))
        assertEquals("kraftige-farben", RecipeJson.slug("Kräftige Farben"))
        assertEquals("rezept", RecipeJson.slug("+++"))
    }
}
