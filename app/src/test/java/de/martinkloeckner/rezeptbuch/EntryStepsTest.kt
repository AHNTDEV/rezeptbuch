package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.data.RecipeJson
import de.martinkloeckner.rezeptbuch.domain.EntryItem
import de.martinkloeckner.rezeptbuch.domain.EntrySteps
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntryStepsTest {
    private val recipes = RecipeJson.parseLibrary(File("../data/recipes.json").readText()).recipes.associateBy { it.id }

    @Test fun manualKneeAndDetailGiveAllSteps() {
        val items = EntrySteps.build(recipes.getValue("kodak-gold"))
        assertEquals(26, EntrySteps.stepCount(items))
    }

    @Test fun autoKneeOmitsPointAndSlope() {
        val items = EntrySteps.build(recipes.getValue("ev-pro-plus"))
        assertEquals(24, EntrySteps.stepCount(items))
        assertTrue(items.filterIsInstance<EntryItem.Step>().none { it.label.startsWith("Knie · Punkt") })
    }

    @Test fun sectionsInMenuOrderAndIndicesAreContiguous() {
        val items = EntrySteps.build(recipes.getValue("kodak-gold"))
        assertEquals(
            listOf("Bildprofil", "Farbtiefe", "Detail", "Weißabgleich"),
            items.filterIsInstance<EntryItem.Section>().map { it.title },
        )
        assertEquals((0 until 26).toList(), items.filterIsInstance<EntryItem.Step>().map { it.index })
    }

    @Test fun valuesAreFormatted() {
        val steps = EntrySteps.build(recipes.getValue("kodak-gold")).filterIsInstance<EntryItem.Step>().associate { it.label to it.value }
        assertEquals("−5", steps["Schwarzwert"])
        assertEquals("Movie", steps["Gamma"])
        assertEquals("75 %", steps["Knie · Punkt"])
        assertEquals("8000 K", steps["Farbtemperatur"])
        assertEquals("B3", steps["A-B"])
        assertEquals("M1.75", steps["G-M"])
    }
}
