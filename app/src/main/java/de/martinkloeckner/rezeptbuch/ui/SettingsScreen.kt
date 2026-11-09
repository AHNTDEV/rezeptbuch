package de.martinkloeckner.rezeptbuch.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.martinkloeckner.rezeptbuch.MainViewModel
import de.martinkloeckner.rezeptbuch.data.ImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                val text = vm.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                vm.say("Bibliothek exportiert")
            } catch (e: Exception) {
                vm.say("Export fehlgeschlagen")
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }
            } catch (e: Exception) {
                vm.say("Datei konnte nicht gelesen werden")
            }
        }
    }

    SubScreenScaffold("Einstellungen", onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Sicherung") {
                Text(
                    "Die Bibliothek (Rezepte und PP-Plätze) wird als JSON gesichert – im selben Format wie der Startbestand. " +
                        "Beispielfotos sind in dieser Datei nicht enthalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                )
                OutlinedButton(
                    onClick = { exporter.launch("rezeptbuch.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Bibliothek exportieren") }
                OutlinedButton(
                    onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Bibliothek importieren") }
            }
            Section("Startbestand") {
                Text(
                    "Stellt die 8 mitgelieferten Rezepte wieder her. Gleichnamige Rezepte werden auf den Originalstand zurückgesetzt, " +
                        "eigene Rezepte und Fotos bleiben erhalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                )
                OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Startbestand wiederherstellen")
                }
            }
            Section("Fuji-Adaptionen") {
                Text(
                    "Fujifilm-Rezepte von filmsimrecipes.com, nach festen Regeln auf Sony-Bildprofile übertragen " +
                        "(siehe docs/FUJI-ADAPTION.md). Sie sind Näherungen und nicht an der Kamera geprüft. " +
                        "Bereits vorhandene Adaptionen werden auf den Originalstand zurückgesetzt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                )
                OutlinedButton(onClick = { vm.addFujiAdaptions() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fuji-Adaptionen hinzufügen")
                }
            }
            Section("Hinweis") {
                Text(
                    "Alle Kameraangaben stammen aus Quellen und Sonys Hilfe-Guide und sind bis zum Test an einer echten α7R III unbestätigt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                )
            }
        }
    }

    if (pendingImport != null && !confirmReplace) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Bibliothek importieren") },
            text = { Text("Zusammenführen: gleiche Rezepte werden überschrieben, alle anderen bleiben.\nErsetzen: die bisherige Bibliothek wird vollständig gelöscht.") },
            confirmButton = {
                TextButton(onClick = { pendingImport?.let { vm.importJson(it, ImportMode.Merge) }; pendingImport = null }) {
                    Text("Zusammenführen")
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { confirmReplace = true }) { Text("Ersetzen …", color = Palette.Danger) }
                    TextButton(onClick = { pendingImport = null }) { Text("Abbrechen") }
                }
            },
        )
    }
    if (pendingImport != null && confirmReplace) {
        ConfirmDialog(
            title = "Bibliothek wirklich ersetzen?",
            text = "Alle bisherigen Rezepte, Fotos und PP-Plätze werden gelöscht. Das lässt sich nicht rückgängig machen.",
            confirmText = "Ersetzen",
            onConfirm = { pendingImport?.let { vm.importJson(it, ImportMode.Replace) }; pendingImport = null; confirmReplace = false },
            onDismiss = { pendingImport = null; confirmReplace = false },
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = "Startbestand wiederherstellen?",
            text = "Änderungen an den 8 mitgelieferten Rezepten gehen verloren.",
            confirmText = "Wiederherstellen",
            onConfirm = { confirmRestore = false; vm.restoreStarter() },
            onDismiss = { confirmRestore = false },
        )
    }
}
