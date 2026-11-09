package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.data.RecipeJson
import de.martinkloeckner.rezeptbuch.domain.Options
import de.martinkloeckner.rezeptbuch.domain.Recipe
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Checks the bundled data files against the value ranges of docs/SPEC.md section 3. */
class DataFilesTest {
    private val starter = RecipeJson.parseLibrary(File("../data/recipes.json").readText()).recipes
    private val fuji = RecipeJson.parseLibrary(File("../data/fuji-adaptions.json").readText()).recipes

    private fun Recipe.problems(): List<String> {
        val p = mutableListOf<String>()
        fun int(label: String, v: Int?, lo: Int, hi: Int) {
            if (v != null && v !in lo..hi) p += "$label=$v not in $lo..$hi"
        }
        int("blackLevel", pp.blackLevel, -15, 15)
        int("blackGamma.level", pp.blackGamma.level, -7, 7)
        int("knee.slope", pp.knee.slope, -5, 5)
        int("saturation", pp.saturation, -32, 32)
        int("colorPhase", pp.colorPhase, -7, 7)
        pp.depth.entries.forEach { (k, v) -> int("depth$k", v, -7, 7) }
        int("detail.level", pp.detail.level, -7, 7)
        int("detail.vh", pp.detail.vh, -2, 2)
        int("detail.limit", pp.detail.limit, 0, 7)
        int("detail.crisp", pp.detail.crisp, 0, 7)
        int("detail.hl", pp.detail.hl, 0, 4)
        pp.knee.point?.let { if (it !in 75.0..105.0 || (it * 2) % 5.0 != 0.0) p += "knee.point=$it" }
        wb.kelvin?.let { if (it !in 2500..9900 || it % 100 != 0) p += "kelvin=$it" }
        if (wb.ab !in -7.0..7.0 || (wb.ab * 4) % 1.0 != 0.0) p += "ab=${wb.ab}"
        if (wb.gm !in -7.0..7.0 || (wb.gm * 4) % 1.0 != 0.0) p += "gm=${wb.gm}"
        if (pp.gamma.isNotBlank() && pp.gamma !in Options.gammas && "/" !in pp.gamma) p += "gamma=${pp.gamma}"
        if (pp.colorMode.isNotBlank() && pp.colorMode !in Options.colorModes && "/" !in pp.colorMode) p += "colorMode=${pp.colorMode}"
        if (pp.blackGamma.range.isNotBlank() && pp.blackGamma.range !in Options.blackGammaRanges) p += "bgRange=${pp.blackGamma.range}"
        if (pp.knee.mode.isNotBlank() && pp.knee.mode !in Options.autoManual) p += "kneeMode=${pp.knee.mode}"
        if (pp.detail.mode.isNotBlank() && pp.detail.mode !in Options.autoManual) p += "detailMode=${pp.detail.mode}"
        if (pp.detail.bw.isNotBlank() && pp.detail.bw !in Options.bwBalances) p += "bw=${pp.detail.bw}"
        return p
    }

    @Test fun starterRecipesAreInRange() {
        val bad = starter.associate { it.id to it.problems() }.filterValues { it.isNotEmpty() }
        assertTrue(bad.isEmpty(), bad.toString())
    }

    @Test fun fujiAdaptionsAreInRange() {
        val bad = fuji.associate { it.id to it.problems() }.filterValues { it.isNotEmpty() }
        assertTrue(bad.isEmpty(), bad.toString())
    }

    @Test fun fujiAdaptionsAreLabelledAsUnverifiedAndCreditTheSource() {
        assertTrue(fuji.size > 100)
        fuji.forEach { r ->
            assertTrue("Fuji-Adaption" in r.tags, r.id)
            assertTrue("nicht an der α7R III geprüft" in r.notes, r.id)
            assertTrue(r.sourceUrl.startsWith("https://filmsimrecipes.com/project/"), r.id)
            assertTrue(r.source.contains("filmsimrecipes.com"), r.id)
        }
    }

    @Test fun idsAreUniqueAcrossBothFiles() {
        val ids = (starter + fuji).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(fuji.all { it.id.startsWith("fx-") })
    }

    @Test fun monochromeRecipesHaveNoColourValues() {
        val mono = fuji.filter { it.pp.colorMode == "Black & White" }
        assertTrue(mono.isNotEmpty())
        mono.forEach { assertEquals(null, it.pp.saturation, it.id) }
    }

    @Test fun ordersContinueAfterStarterLibrary() {
        assertEquals(starter.size + 1, fuji.minOf { it.order })
        assertNotNull(fuji.maxByOrNull { it.order })
    }
}
