package de.martinkloeckner.rezeptbuch.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.martinkloeckner.rezeptbuch.R

/**
 * Darkroom palette: warm brown-black neutrals, safelight amber as the single accent, and the
 * orange-on-film-base look of edge prints for frame numbers.
 */
object Palette {
    val Background = Color(0xFF12100E)
    val Surface = Color(0xFF1A1714)
    val SurfaceHigh = Color(0xFF231F1B)
    val FilmBase = Color(0xFF0C0A09)
    val Line = Color(0xFF332D27)
    val Text = Color(0xFFEDE4D6)
    val Muted = Color(0xFF9C9183)
    val Amber = Color(0xFFE8A33D)
    val Danger = Color(0xFFE5735F)
}

/**
 * Young Serif for names and titles (a warm, heavy serif in the spirit of film packaging),
 * IBM Plex Sans for running text and IBM Plex Mono for every camera value (dotted zero, so "0" and
 * "O" never get confused on a camera menu). Plex Sans is a variable font, supported from API 26.
 */
val SerifFamily = FontFamily(Font(R.font.young_serif_regular, FontWeight.Normal))

@OptIn(ExperimentalTextApi::class)
val SansFamily = FontFamily(
    listOf(400, 500, 600).map { w ->
        Font(
            R.font.ibm_plex_sans,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

val MonoFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private val scheme = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.Background,
    secondary = Palette.Amber,
    onSecondary = Palette.Background,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
    onError = Palette.Background,
)

private fun sans(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = SansFamily, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

private fun serif(size: Int, lineHeight: Int) =
    // Young Serif defaults to old-style figures; lining figures keep "Ektar 100" and "400H" readable.
    TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontFeatureSettings = "lnum",
    )

private fun mono(size: Int, lineHeight: Int, spacing: Double = 0.8, weight: FontWeight = FontWeight.Medium) =
    TextStyle(
        fontFamily = MonoFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = spacing.sp,
    )

// Type scale: 12 / 14 / 16 for text, 17 / 22 / 30 for serif titles, 11 / 13 mono labels.
private val typography = Typography(
    headlineLarge = serif(30, 36),
    headlineMedium = serif(26, 32),
    titleLarge = serif(22, 28),
    titleMedium = serif(17, 22),
    titleSmall = sans(14, 20, FontWeight.Medium),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 21),
    bodySmall = sans(12, 17),
    labelLarge = sans(14, 20, FontWeight.Medium),
    labelMedium = mono(12, 16),
    labelSmall = mono(11, 14, spacing = 1.2),
)

/** Camera values: always monospace, always tabular. */
val ValueStyle = mono(14, 20, spacing = 0.0)

/** Edge-print style used for frame numbers and slot codes. */
val EdgeStyle = mono(11, 14, spacing = 1.6)

// Film frames have almost square corners.
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun RezeptbuchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
