package de.martinkloeckner.rezeptbuch.domain

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Computed colour card shown when a recipe has no photo (SPEC section 5). Port of the prototype's
 * `castColor` / `swatchHTML`. This is only a rough approximation of the look and is always labelled so.
 */
data class Swatch(
    /** Shadow → mid → highlight colours as 0xRRGGBB. */
    val shadow: Int,
    val mid: Int,
    val highlight: Int,
    /** Colour chips R, G, B, C, M, Y: base colour 0xRRGGBB, plus saturation and brightness factors. */
    val chips: List<Chip>,
) {
    data class Chip(val letter: String, val rgb: Int, val saturation: Float, val brightness: Float)

    companion object {
        private val chipColors = mapOf(
            "R" to 0xD14B3B, "G" to 0x4F9A4A, "B" to 0x3D63C9,
            "C" to 0x3FA7B5, "M" to 0xB8479C, "Y" to 0xD9B43A,
        )

        fun of(r: Recipe): Swatch {
            val c = castColor(r)
            val bl = r.pp.blackLevel ?: 0
            val shadowV = max(4.0, min(60.0, 18 + bl * 2.2))
            fun rgb(a: Double, b: Double, c2: Double) = pack(a, b, c2)
            val mid = rgb(c[0] * .62, c[1] * .62, c[2] * .62)
            val hi = rgb(c[0] * .35 + 165, c[1] * .35 + 165, c[2] * .35 + 165)
            val sh = rgb(shadowV, shadowV * .95, shadowV * .9)
            val chips = r.pp.depth.entries.map { (k, v) ->
                val d = v ?: 0
                Chip(
                    letter = k,
                    rgb = chipColors.getValue(k),
                    saturation = max(.35, min(1.25, .85 + d * .05)).toFloat(),
                    brightness = (1 - d * .035).toFloat(),
                )
            }
            return Swatch(sh, mid, hi, chips)
        }

        /** Approximate colour cast from white balance and saturation; each channel 0..255. */
        fun castColor(r: Recipe): List<Int> {
            val k = (r.wb.kelvin ?: 5500).toDouble()
            // A higher camera WB value renders a warmer image.
            val base = kelvinRgb(max(2000.0, min(12000.0, 11000 - k)))
            val a = r.wb.ab
            val g = r.wb.gm
            var c = listOf(base[0] + a * 7 - g * 3, base[1] + g * 7, base[2] - a * 8 - g * 3)
            val sat = 1 + (r.pp.saturation ?: 0) / 50.0
            val avg = (c[0] + c[1] + c[2]) / 3
            c = c.map { avg + (it - avg) * sat }
            return c.map { max(0.0, min(255.0, it)).roundToInt() }
        }

        private fun kelvinRgb(kelvin: Double): List<Double> {
            val k = max(1500.0, min(15000.0, kelvin)) / 100
            val r = if (k <= 66) 255.0 else 329.7 * (k - 60).pow(-0.1332)
            val g = if (k <= 66) 99.47 * ln(k) - 161.12 else 288.12 * (k - 60).pow(-0.0755)
            val b = if (k >= 66) 255.0 else if (k <= 19) 0.0 else 138.52 * ln(k - 10) - 305.04
            return listOf(r, g, b).map { max(0.0, min(255.0, it)) }
        }

        private fun pack(r: Double, g: Double, b: Double): Int {
            fun ch(v: Double) = max(0, min(255, v.roundToInt()))
            return (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
        }
    }
}
