package de.martinkloeckner.rezeptbuch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.martinkloeckner.rezeptbuch.domain.Format
import de.martinkloeckner.rezeptbuch.domain.Options
import de.martinkloeckner.rezeptbuch.domain.PictureProfile
import de.martinkloeckner.rezeptbuch.domain.Recipe

@Composable
fun EditorScreen(
    initial: Recipe,
    isNew: Boolean,
    onSave: (Recipe) -> Unit,
    onCancel: () -> Unit,
    onInvalid: (String) -> Unit,
) {
    var r by remember(initial.id) { mutableStateOf(initial) }
    var tagsText by remember(initial.id) { mutableStateOf(initial.tags.joinToString(", ")) }
    val pp = r.pp

    fun setPp(f: (PictureProfile) -> PictureProfile) { r = r.copy(pp = f(r.pp)) }

    SubScreenScaffold(if (isNew) "Neues Rezept" else "Rezept bearbeiten", onCancel) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Allgemein") {
                TextInput("Name *", r.name) { r = r.copy(name = it) }
                TextInput("Look (kurz beschrieben)", r.look) { r = r.copy(look = it) }
                TextInput("Stichworte (mit Komma getrennt)", tagsText) { tagsText = it }
                TextInput("Notizen", r.notes, singleLine = false) { r = r.copy(notes = it) }
                TextInput("Quelle", r.source) { r = r.copy(source = it) }
                TextInput("Quell-Link", r.sourceUrl, keyboard = KeyboardType.Uri) { r = r.copy(sourceUrl = it) }
            }

            Section("Tonwerte") {
                IntStepper("Schwarzwert", pp.blackLevel, -15, 15) { v -> setPp { it.copy(blackLevel = v) } }
                ComboField("Gamma", pp.gamma, Options.gammas) { v -> setPp { it.copy(gamma = v) } }
                ChoiceField("Schwarzgamma · Bereich", pp.blackGamma.range, Options.blackGammaRanges) { v ->
                    setPp { it.copy(blackGamma = it.blackGamma.copy(range = v)) }
                }
                IntStepper("Schwarzgamma · Stufe", pp.blackGamma.level, -7, 7) { v ->
                    setPp { it.copy(blackGamma = it.blackGamma.copy(level = v)) }
                }
                ChoiceField("Knie · Modus", pp.knee.mode, Options.autoManual) { v -> setPp { it.copy(knee = it.knee.copy(mode = v)) } }
                if (pp.knee.isManual) {
                    DoubleStepper("Knie · Punkt (%)", pp.knee.point, 75.0, 105.0, 2.5, suffix = " %") { v ->
                        setPp { it.copy(knee = it.knee.copy(point = v)) }
                    }
                    IntStepper("Knie · Steigung", pp.knee.slope, -5, 5) { v -> setPp { it.copy(knee = it.knee.copy(slope = v)) } }
                }
            }

            Section("Farbe") {
                ComboField("Farbmodus", pp.colorMode, Options.colorModes) { v -> setPp { it.copy(colorMode = v) } }
                IntStepper("Sättigung", pp.saturation, -32, 32) { v -> setPp { it.copy(saturation = v) } }
                IntStepper("Farbphase", pp.colorPhase, -7, 7) { v -> setPp { it.copy(colorPhase = v) } }
                pp.depth.entries.forEach { (k, v) ->
                    IntStepper("Farbtiefe $k", v, -7, 7) { nv -> setPp { it.copy(depth = it.depth.with(k, nv)) } }
                }
            }

            Section("Detail") {
                IntStepper("Stufe", pp.detail.level, -7, 7) { v -> setPp { it.copy(detail = it.detail.copy(level = v)) } }
                ChoiceField("Modus", pp.detail.mode, Options.autoManual) { v -> setPp { it.copy(detail = it.detail.copy(mode = v)) } }
                if (pp.detail.isManual) {
                    IntStepper("V/H-Balance", pp.detail.vh, -2, 2) { v -> setPp { it.copy(detail = it.detail.copy(vh = v)) } }
                    ChoiceField("B/W-Balance", pp.detail.bw, Options.bwBalances) { v -> setPp { it.copy(detail = it.detail.copy(bw = v)) } }
                    IntStepper("Begrenzung", pp.detail.limit, 0, 7) { v -> setPp { it.copy(detail = it.detail.copy(limit = v)) } }
                    IntStepper("Crispening", pp.detail.crisp, 0, 7) { v -> setPp { it.copy(detail = it.detail.copy(crisp = v)) } }
                    IntStepper("Lichter-Detail", pp.detail.hl, 0, 4) { v -> setPp { it.copy(detail = it.detail.copy(hl = v)) } }
                }
            }

            Section("Weißabgleich") {
                IntStepper("Farbtemperatur (K)", r.wb.kelvin, 2500, 9900, step = 100, signed = false) { v ->
                    r = r.copy(wb = r.wb.copy(kelvin = v))
                }
                DoubleStepper("A-B (+ Amber / − Blau)", r.wb.ab, -7.0, 7.0, 0.25, display = Format::ab) { v ->
                    r = r.copy(wb = r.wb.copy(ab = v ?: 0.0))
                }
                DoubleStepper("G-M (+ Grün / − Magenta)", r.wb.gm, -7.0, 7.0, 0.25, display = Format::gm) { v ->
                    r = r.copy(wb = r.wb.copy(gm = v ?: 0.0))
                }
                Text("Beispiel: „A2-M1“ = A-B +2, G-M −1.", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    if (r.name.isBlank()) {
                        onInvalid("Bitte einen Namen eingeben")
                    } else {
                        onSave(r.copy(name = r.name.trim(), tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
                    }
                }) { Text("Speichern") }
                OutlinedButton(onClick = onCancel) { Text("Abbrechen") }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Amber,
    unfocusedBorderColor = Palette.Line,
    cursorColor = Palette.Amber,
    focusedLabelColor = Palette.Amber,
)

@Composable
private fun TextInput(
    label: String,
    value: String,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        shape = MaterialTheme.shapes.small,
        colors = fieldColors(),
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** − value + stepper; tapping the value clears it (shown as "–"). */
@Composable
private fun IntStepper(
    label: String,
    value: Int?,
    min: Int,
    max: Int,
    step: Int = 1,
    signed: Boolean = true,
    onChange: (Int?) -> Unit,
) {
    StepperRow(
        label = label,
        text = if (signed) Format.signed(value) else Format.plain(value),
        onMinus = { onChange(((value ?: (min + step)) - step).coerceIn(min, max)) },
        onPlus = { onChange(((value ?: (min - step)) + step).coerceIn(min, max)) },
        onClear = { onChange(null) },
    )
}

@Composable
private fun DoubleStepper(
    label: String,
    value: Double?,
    min: Double,
    max: Double,
    step: Double,
    suffix: String = "",
    display: ((Double) -> String)? = null,
    onChange: (Double?) -> Unit,
) {
    fun clamp(v: Double) = v.coerceIn(min, max)
    StepperRow(
        label = label,
        text = when {
            value == null -> Format.DASH
            display != null -> display(value)
            else -> Format.number(value) + suffix
        },
        onMinus = { onChange(clamp((value ?: (min + step)) - step)) },
        onPlus = { onChange(clamp((value ?: (min - step)) + step)) },
        onClear = { onChange(null) },
    )
}

@Composable
private fun StepperRow(label: String, text: String, onMinus: () -> Unit, onPlus: () -> Unit, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) { Text("−") }
        Box(Modifier.size(width = 76.dp, height = 40.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.TextButton(onClick = onClear, contentPadding = PaddingValues(0.dp)) {
                Text(text, style = ValueStyle, color = Palette.Text)
            }
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) { Text("+") }
    }
}

/** Read-only dropdown for fixed menu choices. */
@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            shape = MaterialTheme.shapes.small,
            colors = fieldColors(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent click layer, because a read-only text field swallows taps itself.
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onChange(o) }) }
        }
    }
}

/** Free text with menu suggestions (the sources write e.g. "S-Gamut3/S-Gamut"). */
@Composable
private fun ComboField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            shape = MaterialTheme.shapes.small,
            colors = fieldColors(),
            onValueChange = onChange,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Default.ArrowDropDown, "Vorschläge") } },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onChange(o) }) }
        }
    }
}
