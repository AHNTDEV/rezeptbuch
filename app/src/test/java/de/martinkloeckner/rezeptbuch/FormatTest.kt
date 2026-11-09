package de.martinkloeckner.rezeptbuch

import de.martinkloeckner.rezeptbuch.domain.Format
import de.martinkloeckner.rezeptbuch.domain.WhiteBalance
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test fun signedShowsSignAndTrueMinus() {
        assertEquals("+6", Format.signed(6))
        assertEquals("−7", Format.signed(-7))
        assertEquals("0", Format.signed(0))
        assertEquals("–", Format.signed(null))
    }

    @Test fun abNotation() {
        assertEquals("A2", Format.ab(2.0))
        assertEquals("B3", Format.ab(-3.0))
        assertEquals("A3.5", Format.ab(3.5))
        assertEquals("0", Format.ab(0.0))
        assertEquals("A7", Format.ab(7.0))
    }

    @Test fun gmNotation() {
        assertEquals("M1.75", Format.gm(-1.75))
        assertEquals("G1", Format.gm(1.0))
        assertEquals("M0.25", Format.gm(-0.25))
        assertEquals("0", Format.gm(0.0))
    }

    @Test fun whiteBalanceLineMatchesSpecExample() {
        assertEquals("8000 K · B3 / M1.75", Format.wb(WhiteBalance(8000, -3.0, -1.75)))
        assertEquals("5000 K · A2 / M1", Format.wb(WhiteBalance(5000, 2.0, -1.0)))
        assertEquals("– · 0 / 0", Format.wb(WhiteBalance(null, 0.0, 0.0)))
    }

    @Test fun kneePointAndNumbers() {
        assertEquals("92.5 %", Format.kneePoint(92.5))
        assertEquals("75 %", Format.kneePoint(75.0))
        assertEquals("–", Format.kneePoint(null))
        assertEquals("3", Format.number(3.0))
        assertEquals("1.75", Format.number(1.75))
    }

    @Test fun blankTextBecomesDash() {
        assertEquals("–", Format.text(""))
        assertEquals("Still", Format.text("Still"))
    }
}
