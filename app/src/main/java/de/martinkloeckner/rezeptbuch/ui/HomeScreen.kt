package de.martinkloeckner.rezeptbuch.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.martinkloeckner.rezeptbuch.HomeTab
import de.martinkloeckner.rezeptbuch.MainViewModel
import de.martinkloeckner.rezeptbuch.domain.Format
import de.martinkloeckner.rezeptbuch.domain.Library
import de.martinkloeckner.rezeptbuch.domain.Options
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic
import de.martinkloeckner.rezeptbuch.domain.Swatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    library: Library,
    tab: HomeTab,
    onOpenRecipe: (String) -> Unit,
    onNewRecipe: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        containerColor = Palette.Background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Rezeptbuch α7R III", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Background),
                    actions = {
                        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Einstellungen") }
                    },
                )
                FilmStrip()
            }
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Palette.Amber,
                selectedTextColor = Palette.Amber,
                indicatorColor = Palette.SurfaceHigh,
                unselectedIconColor = Palette.Muted,
                unselectedTextColor = Palette.Muted,
            )
            NavigationBar(containerColor = Palette.FilmBase) {
                NavigationBarItem(
                    selected = tab == HomeTab.Gallery,
                    onClick = { vm.selectTab(HomeTab.Gallery) },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Galerie", style = MaterialTheme.typography.labelLarge) },
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Slots,
                    onClick = { vm.selectTab(HomeTab.Slots) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("PP-Plätze", style = MaterialTheme.typography.labelLarge) },
                    colors = navColors,
                )
            }
        },
        floatingActionButton = {
            if (tab == HomeTab.Gallery) {
                ExtendedFloatingActionButton(
                    onClick = onNewRecipe,
                    containerColor = Palette.Amber,
                    contentColor = Palette.Background,
                    shape = MaterialTheme.shapes.small,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Rezept", style = MaterialTheme.typography.labelLarge) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                HomeTab.Gallery -> GalleryContent(vm, library, onOpenRecipe)
                HomeTab.Slots -> SlotsContent(vm, library, onOpenRecipe)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Gallery
// ---------------------------------------------------------------------------------------------

fun filterRecipes(recipes: List<Recipe>, tag: String?, query: String): List<Recipe> {
    val q = query.trim().lowercase()
    return recipes.filter { r ->
        (tag == null || tag in r.tags) &&
            (q.isEmpty() || listOf(r.name, r.look, r.tags.joinToString(" "), r.notes).joinToString(" ").lowercase().contains(q))
    }.sortedWith(compareBy<Recipe> { it.order }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

@Composable
private fun GalleryContent(vm: MainViewModel, library: Library, onOpen: (String) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val tag by vm.activeTag.collectAsStateWithLifecycle()
    val tags = library.recipes.flatMap { it.tags }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    val list = filterRecipes(library.recipes, tag, query)

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            singleLine = true,
            placeholder = { Text("Suchen …") },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Amber,
                unfocusedBorderColor = Palette.Line,
                cursorColor = Palette.Amber,
            ),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Clear, "Suche löschen") }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { TagChip("Alle", tag == null) { vm.setTag(null) } }
            items(tags) { t -> TagChip(t, tag == t) { vm.setTag(t) } }
        }
        when {
            library.recipes.isEmpty() -> EmptyNote("Noch keine Rezepte. Leg über „+ Rezept“ das erste an.")
            list.isEmpty() -> EmptyNote("Kein Rezept passt zu dieser Auswahl.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { r ->
                    RecipeCard(vm, r, SlotLogic.slotOf(library.slots, r.id), onClick = { onOpen(r.id) })
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Palette.Background,
            labelColor = Palette.Muted,
            selectedContainerColor = Palette.Amber.copy(alpha = .16f),
            selectedLabelColor = Palette.Amber,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Palette.Line,
            selectedBorderColor = Palette.Amber.copy(alpha = .6f),
        ),
    )
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Palette.Muted)
    }
}

@Composable
private fun RecipeCard(vm: MainViewModel, r: Recipe, slot: String?, onClick: () -> Unit) {
    // A frame on a contact sheet: edge print with frame number and slot above the picture.
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Palette.FilmBase),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EdgePrint("▸ %02d".format(r.order.coerceAtLeast(0)), Modifier.weight(1f))
            if (slot != null) Badge(slot)
        }
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f)) {
            if (r.photos.isNotEmpty()) {
                PhotoImage(vm.photoStore, r.photos.first(), Modifier.fillMaxSize(), maxSide = 600)
            } else {
                SwatchCard(Swatch.of(r), Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                r.look,
                color = Palette.Muted,
                style = MaterialTheme.typography.bodySmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(Format.text(r.pp.gamma), style = ValueStyle.copy(fontSize = 12.sp), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            Text(Format.wb(r.wb), style = ValueStyle.copy(fontSize = 12.sp), maxLines = 1, color = Palette.Amber)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// PP slots
// ---------------------------------------------------------------------------------------------

@Composable
private fun SlotsContent(vm: MainViewModel, library: Library, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val byId = library.recipes.associateBy { it.id }
    val sorted = library.recipes.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Die Kamera speichert nur die Bildprofil-Werte; der Weißabgleich wird separat eingestellt. " +
                    "Wer PP-Plätze für Video nutzt, lässt einige frei.",
                color = Palette.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(Options.slots, key = { it }) { slot ->
            val r = library.slots[slot]?.let(byId::get)
            SlotRow(slot, r, sorted, onPick = { vm.setSlot(slot, it) }, onOpen = onOpen)
        }
        item {
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, vm.slotsShareText())
                    }
                    context.startActivity(Intent.createChooser(send, "Belegung teilen"))
                },
            ) {
                Icon(Icons.Default.Share, null)
                Text("  Belegung teilen")
            }
        }
    }
}

@Composable
private fun SlotRow(
    slot: String,
    recipe: Recipe?,
    options: List<Recipe>,
    onPick: (String?) -> Unit,
    onOpen: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Badge(slot, Modifier.width(52.dp))
        Column(
            Modifier.weight(1f).clickable(enabled = recipe != null) { recipe?.let { onOpen(it.id) } },
        ) {
            if (recipe != null) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Format.text(recipe.pp.gamma)} · ${Format.wb(recipe.wb)}", style = ValueStyle.copy(fontSize = 12.sp), color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text("frei", color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Box {
            TextButton(onClick = { open = true }) { Text(if (recipe == null) "Wählen" else "Ändern") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("– frei –") }, onClick = { open = false; onPick(null) })
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt.name) }, onClick = { open = false; onPick(opt.id) })
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
    }
}
