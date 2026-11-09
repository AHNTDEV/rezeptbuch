package de.martinkloeckner.rezeptbuch.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.martinkloeckner.rezeptbuch.domain.EntryItem
import de.martinkloeckner.rezeptbuch.domain.EntrySteps
import de.martinkloeckner.rezeptbuch.domain.Recipe

/** "An Kamera einstellen": big checklist for use at the camera. Ticks live in memory only. */
@Composable
fun EntryScreen(
    recipe: Recipe,
    slot: String?,
    checks: Set<Int>,
    onToggle: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    KeepScreenOn()
    val items = EntrySteps.build(recipe)
    val total = EntrySteps.stepCount(items)
    val done = checks.count { it in 0 until total }
    val pp = slot ?: "PPx"

    SubScreenScaffold("${recipe.name} einstellen", onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Palette.Line, MaterialTheme.shapes.small)
                        .background(Palette.Surface, MaterialTheme.shapes.small)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("MENU → Kameraeinstellungen 1 → Bildprofil → $pp → ►", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Weißabgleich: MENU → Kameraeinstellungen 1 → Weißabgleich → Farbtemp./Filter, dann ► für A-B / G-M",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Menübezeichnungen können je nach Sprache und Firmware leicht abweichen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted,
                    )
                }
            }
            item {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("$done von $total erledigt", color = Palette.Muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = onReset) { Text("Zurücksetzen") }
                    }
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Palette.Amber,
                        trackColor = Palette.Line,
                    )
                }
            }
            itemsIndexed(items) { _, item ->
                when (item) {
                    is EntryItem.Section -> Text(
                        item.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Amber,
                        modifier = Modifier.padding(top = 20.dp, bottom = 2.dp),
                    )
                    is EntryItem.Step -> StepRow(item, item.index in checks) { onToggle(item.index) }
                }
            }
            if (total > 0 && done == total) {
                item {
                    Text(
                        "Fertig. Mach ein Testfoto und lade es als Beispielfoto hoch.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .border(1.dp, Palette.Amber.copy(alpha = .5f), MaterialTheme.shapes.small)
                            .background(Palette.Amber.copy(alpha = .08f), MaterialTheme.shapes.small)
                            .padding(14.dp),
                    )
                }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                    Text("Zurück zum Rezept")
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: EntryItem.Step, done: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .border(2.dp, if (done) Palette.Amber else Palette.Muted, CircleShape)
                    .background(if (done) Palette.Amber else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Text("✓", color = Palette.Background, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(
                step.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                color = if (done) Palette.Muted else Palette.Text,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            Text(
                step.value,
                style = ValueStyle.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
                color = if (done) Palette.Muted else Palette.Amber,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
    }
}

/** Keeps the display on while this composable is in the composition (FLAG_KEEP_SCREEN_ON). */
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
