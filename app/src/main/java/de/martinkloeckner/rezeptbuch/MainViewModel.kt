package de.martinkloeckner.rezeptbuch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.martinkloeckner.rezeptbuch.data.ImportMode
import de.martinkloeckner.rezeptbuch.data.RecipeJson
import de.martinkloeckner.rezeptbuch.data.RecipeRepository
import de.martinkloeckner.rezeptbuch.domain.Library
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    data class Detail(val id: String) : Screen
    data class Editor(val id: String?) : Screen
    data class Entry(val id: String) : Screen
    data object Settings : Screen
}

enum class HomeTab { Gallery, Slots }

class MainViewModel(private val repo: RecipeRepository) : ViewModel() {

    /** null until the first emission from the database. */
    val library: StateFlow<Library?> = repo.library
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val photoStore get() = repo.photos

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _tab = MutableStateFlow(HomeTab.Gallery)
    val tab: StateFlow<HomeTab> = _tab.asStateFlow()

    private val _stack = MutableStateFlow<List<Screen>>(emptyList())
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    /** Entry-mode ticks; kept in memory only, as specified. */
    private val _entryChecks = MutableStateFlow<Set<Int>>(emptySet())
    val entryChecks: StateFlow<Set<Int>> = _entryChecks.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch { repo.seedIfNeeded() }
    }

    // ---------- navigation ----------

    fun open(screen: Screen) {
        if (screen is Screen.Entry) _entryChecks.value = emptySet()
        _stack.update { it + screen }
    }

    fun back(): Boolean {
        if (_stack.value.isEmpty()) return false
        _stack.update { it.dropLast(1) }
        return true
    }

    /** Replaces the top screen (e.g. editor → detail after saving). */
    fun replaceTop(screen: Screen) = _stack.update { it.dropLast(1) + screen }

    fun selectTab(tab: HomeTab) { _tab.value = tab }
    fun setQuery(q: String) { _query.value = q }
    fun setTag(tag: String?) { _activeTag.value = tag }

    fun toggleStep(index: Int) = _entryChecks.update { if (index in it) it - index else it + index }
    fun resetSteps() { _entryChecks.value = emptySet() }

    // ---------- recipes ----------

    fun save(recipe: Recipe, onSaved: (Recipe) -> Unit) {
        viewModelScope.launch {
            try {
                onSaved(repo.save(recipe))
                say("Gespeichert")
            } catch (e: Exception) {
                say("Speichern fehlgeschlagen")
            }
        }
    }

    fun duplicate(id: String) {
        viewModelScope.launch {
            val copy = repo.duplicate(id)
            if (copy != null) {
                replaceTop(Screen.Detail(copy.id))
                say("Variante angelegt")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            _stack.update { st -> st.filterNot { it is Screen.Detail && it.id == id || it is Screen.Entry && it.id == id } }
            say("Rezept gelöscht")
        }
    }

    fun setSlotForRecipe(recipeId: String, slot: String?) {
        viewModelScope.launch { repo.setRecipeSlot(recipeId, slot) }
    }

    fun setSlot(slot: String, recipeId: String?) {
        viewModelScope.launch { repo.setSlot(slot, recipeId) }
    }

    fun slotsShareText(): String {
        val lib = library.value ?: return ""
        return SlotLogic.shareText(lib.slots, lib.recipes)
    }

    // ---------- photos ----------

    fun addPhotos(recipeId: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val n = repo.addPhotos(recipeId, uris)
            say(if (n > 0) "Foto gespeichert" else "Foto konnte nicht gelesen werden")
        }
    }

    fun removePhoto(recipeId: String, name: String) {
        viewModelScope.launch { repo.removePhoto(recipeId, name) }
    }

    // ---------- backup ----------

    suspend fun exportJson(): String = repo.exportJson()

    fun importJson(text: String, mode: ImportMode) {
        viewModelScope.launch {
            try {
                val n = repo.importLibrary(repo.parse(text), mode)
                say("$n Rezepte importiert")
            } catch (e: RecipeJson.FormatException) {
                say(e.message ?: "Import fehlgeschlagen")
            } catch (e: Exception) {
                say("Import fehlgeschlagen")
            }
        }
    }

    fun restoreStarter() {
        viewModelScope.launch {
            try {
                repo.restoreStarter()
                say("Startbestand wiederhergestellt")
            } catch (e: Exception) {
                say("Wiederherstellen fehlgeschlagen")
            }
        }
    }

    fun addFujiAdaptions() {
        viewModelScope.launch {
            try {
                say("${repo.addFujiAdaptions()} Fuji-Adaptionen hinzugefügt")
            } catch (e: Exception) {
                say("Hinzufügen fehlgeschlagen")
            }
        }
    }

    fun say(text: String) { _messages.tryEmit(text) }

    class Factory(private val repo: RecipeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
    }
}
