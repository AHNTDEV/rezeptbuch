package de.martinkloeckner.rezeptbuch.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import de.martinkloeckner.rezeptbuch.MainViewModel
import de.martinkloeckner.rezeptbuch.domain.Format
import de.martinkloeckner.rezeptbuch.domain.Options
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.Swatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Palette.Background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                    actions = { actions() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Background),
                )
                FilmStrip()
            }
        },
        content = content,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    vm: MainViewModel,
    recipe: Recipe,
    slot: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEntry: () -> Unit,
) {
    val context = LocalContext.current
    var heroIdx by remember(recipe.id) { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPhoto by remember { mutableStateOf(false) }
    var slotMenu by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris: List<Uri> ->
        vm.addPhotos(recipe.id, uris)
    }
    val photoIdx = heroIdx.coerceIn(0, (recipe.photos.size - 1).coerceAtLeast(0))

    SubScreenScaffold("Rezept", onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero: first/selected photo or computed colour card
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f).background(Palette.FilmBase, MaterialTheme.shapes.medium)) {
                if (recipe.photos.isNotEmpty()) {
                    PhotoImage(vm.photoStore, recipe.photos[photoIdx], Modifier.fillMaxSize(), maxSide = 1600)
                } else {
                    SwatchCard(Swatch.of(recipe), Modifier.fillMaxSize())
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                EdgePrint("▸ %02d".format(recipe.order.coerceAtLeast(0)), Modifier.weight(1f))
                if (slot != null) Badge(slot)
            }
            Text(recipe.name, style = MaterialTheme.typography.headlineLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                recipe.photos.forEachIndexed { i, name ->
                    Box(
                        Modifier
                            .size(56.dp)
                            .border(2.dp, if (i == photoIdx) Palette.Amber else Palette.Line, MaterialTheme.shapes.small)
                            .clickable { heroIdx = i },
                    ) { PhotoImage(vm.photoStore, name, Modifier.fillMaxSize(), maxSide = 160) }
                }
                TextButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("+ Beispielfoto") }
                if (recipe.photos.isNotEmpty()) {
                    TextButton(onClick = { confirmPhoto = true }) { Text("Foto entfernen", color = Palette.Muted) }
                }
            }

            if (recipe.look.isNotBlank()) Text(recipe.look, style = MaterialTheme.typography.bodyLarge)
            if (recipe.tags.isNotEmpty()) {
                Text(recipe.tags.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
            if (recipe.notes.isNotBlank()) {
                Text(
                    recipe.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Palette.Amber.copy(alpha = .5f), MaterialTheme.shapes.small)
                        .background(Palette.Amber.copy(alpha = .08f), MaterialTheme.shapes.small)
                        .padding(12.dp),
                )
            }

            Button(
                onClick = onEntry,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("An Kamera einstellen", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    TextButton(onClick = { slotMenu = true }) { Text("Platz: ${slot ?: "–"}") }
                    DropdownMenu(expanded = slotMenu, onDismissRequest = { slotMenu = false }) {
                        DropdownMenuItem(text = { Text("–") }, onClick = { slotMenu = false; vm.setSlotForRecipe(recipe.id, null) })
                        Options.slots.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { slotMenu = false; vm.setSlotForRecipe(recipe.id, s) })
                        }
                    }
                }
                TextButton(onClick = onEdit) { Text("Bearbeiten") }
                TextButton(onClick = { vm.duplicate(recipe.id) }) { Text("Duplizieren") }
                TextButton(onClick = { confirmDelete = true }) { Text("Löschen", color = Palette.Danger) }
            }

            ValueGroups(recipe)

            if (recipe.source.isNotBlank()) {
                val url = recipe.sourceUrl
                Text(
                    "Quelle: ${recipe.source}" + if (url.isNotBlank()) "  ↗" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (url.isNotBlank()) Palette.Amber else Palette.Muted,
                    modifier = Modifier.clickable(enabled = url.isNotBlank()) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "„${recipe.name}“ wirklich löschen?",
            text = "Beispielfotos werden ebenfalls gelöscht, ein belegter PP-Platz wird frei.",
            confirmText = "Löschen",
            onConfirm = { confirmDelete = false; vm.delete(recipe.id) },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmPhoto) {
        ConfirmDialog(
            title = "Dieses Foto aus dem Rezept entfernen?",
            confirmText = "Entfernen",
            onConfirm = { confirmPhoto = false; vm.removePhoto(recipe.id, recipe.photos[photoIdx]) },
            onDismiss = { confirmPhoto = false },
        )
    }
}

@Composable
private fun ValueGroups(r: Recipe) {
    val p = r.pp
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Section("Tonwerte") {
            ValueRow("Schwarzwert", Format.signed(p.blackLevel))
            ValueRow("Gamma", Format.text(p.gamma))
            ValueRow("Schwarzgamma · Bereich", Format.text(p.blackGamma.range))
            ValueRow("Schwarzgamma · Stufe", Format.signed(p.blackGamma.level))
            ValueRow("Knie · Modus", Format.text(p.knee.mode))
            if (p.knee.isManual) {
                ValueRow("Knie · Punkt", Format.kneePoint(p.knee.point))
                ValueRow("Knie · Steigung", Format.signed(p.knee.slope))
            }
        }
        Section("Farbe") {
            ValueRow("Farbmodus", Format.text(p.colorMode))
            ValueRow("Sättigung", Format.signed(p.saturation))
            ValueRow("Farbphase", Format.signed(p.colorPhase))
            Text("Farbtiefe", color = Palette.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                p.depth.entries.forEach { (k, v) -> DepthCell(k, v, depthColor(k)) }
            }
        }
        Section("Detail") {
            ValueRow("Stufe", Format.signed(p.detail.level))
            ValueRow("Modus", Format.text(p.detail.mode))
            if (p.detail.isManual) {
                ValueRow("V/H-Balance", Format.signed(p.detail.vh))
                ValueRow("B/W-Balance", Format.text(p.detail.bw))
                ValueRow("Begrenzung", Format.plain(p.detail.limit))
                ValueRow("Crispening", Format.plain(p.detail.crisp))
                ValueRow("Lichter-Detail", Format.plain(p.detail.hl))
            }
        }
        Section("Weißabgleich") {
            WhiteBalanceViz(r.wb, Modifier.padding(bottom = 6.dp))
            ValueRow("Farbtemperatur", Format.kelvin(r.wb.kelvin))
            ValueRow("A-B (Amber/Blau)", Format.ab(r.wb.ab))
            ValueRow("G-M (Grün/Magenta)", Format.gm(r.wb.gm))
        }
    }
}

private fun depthColor(letter: String) = when (letter) {
    "R" -> androidx.compose.ui.graphics.Color(0xFFD14B3B)
    "G" -> androidx.compose.ui.graphics.Color(0xFF4F9A4A)
    "B" -> androidx.compose.ui.graphics.Color(0xFF3D63C9)
    "C" -> androidx.compose.ui.graphics.Color(0xFF3FA7B5)
    "M" -> androidx.compose.ui.graphics.Color(0xFFB8479C)
    else -> androidx.compose.ui.graphics.Color(0xFFD9B43A)
}
