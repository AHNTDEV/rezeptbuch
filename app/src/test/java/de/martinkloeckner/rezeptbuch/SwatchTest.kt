package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.domain.PictureProfile
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.Swatch
import de.martinkloeckner.rezeptbuch.domain.WhiteBalance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwatchTest {
    private fun cast(k: Int) = Swatch.castColor(Recipe(id = "x", wb = WhiteBalance(k, 0.0, 0.0)))

    @Test fun higherKelvinSettingIsWarmer() {
        val warm = cast(8000)
        val cool = cast(3500)
        assertTrue(warm[0] - warm[2] > cool[0] - cool[2])
    }

    @Test fun channelsStayInRange() {
        val c = Swatch.castColor(Recipe(id = "x", pp = PictureProfile(saturation = 32), wb = WhiteBalance(9900, 7.0, -7.0)))
        assertTrue(c.all { it in 0..255 })
    }

    @Test fun sixChipsInOrder() {
        val s = Swatch.of(Recipe.blank("x"))
        assertEquals(listOf("R", "G", "B", "C", "M", "Y"), s.chips.map { it.letter })
    }

    @Test fun lowerBlackLevelGivesDarkerShadows() {
        fun shadow(bl: Int) = (Swatch.of(Recipe(id = "x", pp = PictureProfile(blackLevel = bl))).shadow shr 16) and 0xFF
        assertTrue(shadow(-15) < shadow(10))
    }
}
