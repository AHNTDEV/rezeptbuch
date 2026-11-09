package de.martinkloeckner.rezeptbuch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.martinkloeckner.rezeptbuch.MainViewModel
import de.martinkloeckner.rezeptbuch.Screen
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic

@Composable
fun AppRoot(vm: MainViewModel) {
    val library by vm.library.collectAsStateWithLifecycle()
    val stack by vm.stack.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val checks by vm.entryChecks.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = stack.isNotEmpty()) { vm.back() }

    Box(Modifier.fillMaxSize().drawBehind { drawRect(Palette.Background) }) {
        val lib = library
        if (lib == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Palette.Amber)
        } else {
            val byId: (String) -> Recipe? = { id -> lib.recipes.firstOrNull { it.id == id } }
            when (val screen = stack.lastOrNull()) {
                null -> HomeScreen(
                    vm = vm,
                    library = lib,
                    tab = tab,
                    onOpenRecipe = { vm.open(Screen.Detail(it)) },
                    onNewRecipe = { vm.open(Screen.Editor(null)) },
                    onSettings = { vm.open(Screen.Settings) },
                )
                is Screen.Detail -> {
                    val r = byId(screen.id)
                    if (r == null) LaunchedEffect(screen) { vm.back() } else DetailScreen(
                        vm = vm,
                        recipe = r,
                        slot = SlotLogic.slotOf(lib.slots, r.id),
                        onBack = { vm.back() },
                        onEdit = { vm.open(Screen.Editor(r.id)) },
                        onEntry = { vm.open(Screen.Entry(r.id)) },
                    )
                }
                is Screen.Entry -> {
                    val r = byId(screen.id)
                    if (r == null) LaunchedEffect(screen) { vm.back() } else EntryScreen(
                        recipe = r,
                        slot = SlotLogic.slotOf(lib.slots, r.id),
                        checks = checks,
                        onToggle = vm::toggleStep,
                        onReset = vm::resetSteps,
                        onBack = { vm.back() },
                    )
                }
                is Screen.Editor -> {
                    val existing = screen.id?.let(byId)
                    EditorScreen(
                        initial = existing ?: Recipe.blank(),
                        isNew = existing == null,
                        onSave = { draft ->
                            vm.save(draft) { saved ->
                                vm.replaceTop(Screen.Detail(saved.id))
                                // Keep a stable stack when editing from detail: detail is already below us.
                                if (existing != null) vm.back()
                            }
                        },
                        onCancel = { vm.back() },
                        onInvalid = vm::say,
                    )
                }
                Screen.Settings -> SettingsScreen(vm, onBack = { vm.back() })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
