package de.martinkloeckner.rezeptbuch.domain

import java.util.Locale

/** Display formatting. UI language is German; numbers are shown with sign (+6, −7, 0). */
object Format {
    const val DASH = "–"
    private const val MINUS = "−" // U+2212

    /** Signed integer; null → "–". */
    fun signed(n: Int?): String = when {
        n == null -> DASH
        n > 0 -> "+$n"
        n < 0 -> "$MINUS${-n}"
        else -> "0"
    }

    /** Plain integer or "–". */
    fun plain(n: Int?): String = n?.toString() ?: DASH

    /** Number without trailing zeros: 3.0 → "3", 1.75 → "1.75". Uses '.' as in the spec. */
    fun number(d: Double): String {
        val s = String.format(Locale.ROOT, "%.2f", d).trimEnd('0').trimEnd('.')
        return if (s == "-0" || s.isEmpty()) "0" else s
    }

    /** A–B shift: "A2", "B3", "0". Fractions kept (A3.5). */
    fun ab(v: Double): String = shift(v, "A", "B")

    /** G–M shift: "G1", "M1.75", "0". */
    fun gm(v: Double): String = shift(v, "G", "M")

    private fun shift(v: Double, pos: String, neg: String): String {
        if (v == 0.0) return "0"
        return (if (v > 0) pos else neg) + number(kotlin.math.abs(v))
    }

    fun kelvin(k: Int?): String = if (k == null) DASH else "$k K"

    /** `8000 K · B3 / M1.75` */
    fun wb(w: WhiteBalance): String = "${kelvin(w.kelvin)} · ${ab(w.ab)} / ${gm(w.gm)}"

    /** Knee point, e.g. "92.5 %". */
    fun kneePoint(p: Double?): String = if (p == null) DASH else "${number(p)} %"

    /** Menu text value: blank → "–". */
    fun text(s: String): String = s.ifBlank { DASH }
}
