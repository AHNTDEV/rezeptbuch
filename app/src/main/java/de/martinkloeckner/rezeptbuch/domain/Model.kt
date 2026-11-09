package de.martinkloeckner.rezeptbuch.domain

/** Menu values of the α7R III picture profile (see docs/SPEC.md section 4). Verify on the camera. */
object Options {
    val gammas = listOf(
        "Movie", "Still", "Cine1", "Cine2", "Cine3", "Cine4", "ITU709", "ITU709(800%)",
        "S-Log2", "S-Log3", "HLG", "HLG1", "HLG2", "HLG3",
    )
    val colorModes = listOf(
        "Movie", "Still", "Cinema", "Pro", "ITU709 Matrix", "Black & White",
        "S-Gamut", "S-Gamut3.Cine", "S-Gamut3",
    )
    val blackGammaRanges = listOf("Wide", "Middle", "Narrow")
    val autoManual = listOf("Auto", "Manuell")
    val bwBalances = listOf("Type1", "Type2", "Type3", "Type4", "Type5")
    const val KNEE_MANUAL = "Manuell"
    const val DETAIL_MANUAL = "Manuell"
    val slots = (1..10).map { "PP$it" }
}

data class BlackGamma(val range: String = "", val level: Int? = null)

data class Knee(val mode: String = "", val point: Double? = null, val slope: Int? = null) {
    val isManual get() = mode == Options.KNEE_MANUAL
}

data class Depth(
    val r: Int? = null, val g: Int? = null, val b: Int? = null,
    val c: Int? = null, val m: Int? = null, val y: Int? = null,
) {
    /** Letter/value pairs in menu order R, G, B, C, M, Y. */
    val entries: List<Pair<String, Int?>>
        get() = listOf("R" to r, "G" to g, "B" to b, "C" to c, "M" to m, "Y" to y)

    fun with(letter: String, value: Int?): Depth = when (letter) {
        "R" -> copy(r = value)
        "G" -> copy(g = value)
        "B" -> copy(b = value)
        "C" -> copy(c = value)
        "M" -> copy(m = value)
        "Y" -> copy(y = value)
        else -> this
    }
}

data class Detail(
    val level: Int? = null,
    val mode: String = "",
    val vh: Int? = null,
    val bw: String = "",
    val limit: Int? = null,
    val crisp: Int? = null,
    val hl: Int? = null,
) {
    val isManual get() = mode == Options.DETAIL_MANUAL
}

data class PictureProfile(
    val blackLevel: Int? = null,
    val gamma: String = "",
    val blackGamma: BlackGamma = BlackGamma(),
    val knee: Knee = Knee(),
    val colorMode: String = "",
    val saturation: Int? = null,
    val colorPhase: Int? = null,
    val depth: Depth = Depth(),
    val detail: Detail = Detail(),
)

/** ab: + = A (amber), − = B (blue). gm: + = G (green), − = M (magenta). */
data class WhiteBalance(val kelvin: Int? = null, val ab: Double = 0.0, val gm: Double = 0.0)

data class Recipe(
    val id: String,
    val order: Int = 0,
    val name: String = "",
    val look: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val photos: List<String> = emptyList(),
    val pp: PictureProfile = PictureProfile(),
    val wb: WhiteBalance = WhiteBalance(),
    val updatedAt: Long = 0L,
) {
    companion object {
        /** Defaults for a new recipe, mirroring the prototype's `blank()`. */
        fun blank(id: String = "", order: Int = 0) = Recipe(
            id = id,
            order = order,
            pp = PictureProfile(
                blackLevel = 0,
                gamma = "Still",
                blackGamma = BlackGamma("Middle", 0),
                knee = Knee("Auto"),
                colorMode = "Still",
                saturation = 0,
                colorPhase = 0,
                depth = Depth(0, 0, 0, 0, 0, 0),
                detail = Detail(level = 0, mode = "Auto", vh = 0, bw = "Type3", limit = 7, crisp = 0, hl = 0),
            ),
            wb = WhiteBalance(5500, 0.0, 0.0),
        )
    }
}

/** A whole library: recipes plus slot assignment (`PP1`..`PP10` → recipe id). */
data class Library(val recipes: List<Recipe>, val slots: Map<String, String> = emptyMap())
