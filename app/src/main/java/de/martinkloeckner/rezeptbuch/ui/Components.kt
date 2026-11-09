package de.martinkloeckner.rezeptbuch.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.martinkloeckner.rezeptbuch.data.PhotoStore
import de.martinkloeckner.rezeptbuch.domain.Swatch
import de.martinkloeckner.rezeptbuch.domain.WhiteBalance

private fun Int.toColor() = Color(0xFF000000.toInt() or this)

/** Computed colour card used when a recipe has no photo; always labelled as a rough approximation. */
@Composable
fun SwatchCard(swatch: Swatch, modifier: Modifier = Modifier, showNote: Boolean = true) {
    Column(modifier.background(Palette.FilmBase)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(swatch.shadow.toColor(), swatch.mid.toColor(), swatch.highlight.toColor()),
                    ),
                ),
        ) {
            if (showNote) {
                Text(
                    "Farbstimmung · grobe Annäherung",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    color = Color.White.copy(alpha = .9f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = .4f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(14.dp)) {
            swatch.chips.forEach { chip ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(chip.rgb.toColor().modulate(chip.saturation, chip.brightness)),
                )
            }
        }
    }
}

private fun Color.modulate(saturation: Float, brightness: Float): Color {
    val grey = (red + green + blue) / 3f
    fun ch(v: Float) = ((grey + (v - grey) * saturation) * brightness).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue))
}

/** Loads a stored photo off the main thread; shows a dark placeholder while loading or if missing. */
@Composable
fun PhotoImage(
    store: PhotoStore,
    name: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 800,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var bitmap by remember(name) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(name, maxSide) { bitmap = store.load(name, maxSide) }
    val bm = bitmap
    if (bm != null) {
        Image(bm.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(Palette.SurfaceHigh))
    }
}

/** Film-strip perforation motif under the app bar. */
@Composable
fun FilmStrip(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp).background(Palette.FilmBase)) {
        val w = 9.dp.toPx()
        val h = 5.dp.toPx()
        val gap = 9.dp.toPx()
        var x = gap / 2
        while (x < size.width) {
            drawRoundRect(
                color = Palette.Line,
                topLeft = Offset(x, (size.height - h) / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
            x += w + gap
        }
    }
}

/** Orange-on-film-base edge print, used for frame numbers like "▸ 05". */
@Composable
fun EdgePrint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = EdgeStyle, color = Palette.Amber.copy(alpha = .8f), modifier = modifier)
}

/** PP slot tag: the one filled amber element, so a taken slot is visible at a glance. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Palette.Background,
        style = EdgeStyle.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        modifier = modifier
            .background(Palette.Amber, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Section of the value sheet: small mono heading with a hairline, no box around the content. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
            Box(Modifier.weight(1f).height(1.dp).background(Palette.Line))
        }
        content()
    }
}

/** Label … dotted leader … value, like a lab sheet. */
@Composable
fun ValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(label, color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
        Canvas(Modifier.weight(1f).height(18.dp).padding(horizontal = 8.dp)) {
            val y = size.height - 5.dp.toPx()
            drawLine(
                color = Palette.Line,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx())),
            )
        }
        Text(value, style = ValueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One colour-depth channel: letter, value and a small bar growing from the centre (−7 … +7). */
@Composable
fun DepthCell(letter: String, value: Int?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(letter, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        Text(de.martinkloeckner.rezeptbuch.domain.Format.signed(value), style = ValueStyle)
        Canvas(Modifier.size(width = 36.dp, height = 4.dp)) {
            drawRect(Palette.Line)
            val mid = size.width / 2
            if (value != null && value != 0) {
                val len = mid * (value.coerceIn(-7, 7) / 7f)
                drawRect(color, topLeft = Offset(if (len > 0) mid else mid + len, 0f), size = Size(kotlin.math.abs(len), size.height))
            }
            drawRect(Palette.Text.copy(alpha = .6f), topLeft = Offset(mid - .5.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height))
        }
    }
}

/**
 * White balance as the camera sets it: the Kelvin scale 2500–9900 K with a marker, and the A–B / G–M
 * grid (B left, A right, M down, G up) with a dot at the recipe's shift.
 */
@Composable
fun WhiteBalanceViz(wb: WhiteBalance, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val barH = 8.dp.toPx()
                val top = size.height - barH
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF6F93C7), Color(0xFFD9D3C9), Color(0xFFE8A33D))),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, barH),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                wb.kelvin?.let { k ->
                    val x = ((k - 2500) / (9900f - 2500f)).coerceIn(0f, 1f) * size.width
                    drawLine(Palette.Text, Offset(x, 2.dp.toPx()), Offset(x, size.height), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("2500 K", style = EdgeStyle, color = Palette.Muted)
                Box(Modifier.weight(1f))
                Text("9900 K", style = EdgeStyle, color = Palette.Muted)
            }
        }
        Box(Modifier.size(84.dp)) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                drawRect(Palette.FilmBase)
                drawRect(Palette.Line, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                val c = Offset(size.width / 2, size.height / 2)
                drawLine(Palette.Line, Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 1.dp.toPx())
                drawLine(Palette.Line, Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 1.dp.toPx())
                val dx = (wb.ab / 7.0).coerceIn(-1.0, 1.0).toFloat() * (size.width / 2)
                val dy = -(wb.gm / 7.0).coerceIn(-1.0, 1.0).toFloat() * (size.height / 2)
                drawCircle(Palette.Amber, radius = 4.dp.toPx(), center = Offset(c.x + dx, c.y + dy))
            }
            val edge = EdgeStyle.copy(fontSize = 10.sp)
            Text("G", style = edge, color = Palette.Muted, modifier = Modifier.align(Alignment.TopCenter))
            Text("M", style = edge, color = Palette.Muted, modifier = Modifier.align(Alignment.BottomCenter))
            Text("B", style = edge, color = Palette.Muted, modifier = Modifier.align(Alignment.CenterStart))
            Text("A", style = edge, color = Palette.Muted, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    text: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = Palette.Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
