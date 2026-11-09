package de.martinkloeckner.rezeptbuch.data

import android.content.Context
import android.net.Uri
import de.martinkloeckner.rezeptbuch.domain.Library
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

enum class ImportMode { Merge, Replace }

/** Single access point for recipes, slots and photos (UI → ViewModel → Repository → Room). */
class RecipeRepository(
    private val context: Context,
    private val db: AppDatabase,
    val photos: PhotoStore,
) {
    private val dao = db.dao()
    private val prefs = context.getSharedPreferences("rezeptbuch", Context.MODE_PRIVATE)

    val recipes: Flow<List<Recipe>> = dao.observeRecipes().map { list -> list.map { it.toDomain() } }

    val slots: Flow<Map<String, String>> = dao.observeSlots().map { list -> list.associate { it.slot to it.recipeId } }

    val library: Flow<Library> = combine(recipes, slots) { r, s -> Library(r, s) }

    /** Imports the bundled starter library on first launch only (deleting everything later does not re-seed). */
    suspend fun seedIfNeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        if (dao.count() == 0) {
            importLibrary(readLibrary(STARTER_ASSET), ImportMode.Merge)
            importLibrary(readLibrary(FUJI_ASSET), ImportMode.Merge)
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private suspend fun readLibrary(asset: String): Library = withContext(Dispatchers.IO) {
        val text = context.assets.open(asset).bufferedReader().use { it.readText() }
        RecipeJson.parseLibrary(text)
    }

    /** Restores the starter recipes (overwrites same ids, keeps their photos, leaves other recipes alone). */
    suspend fun restoreStarter() = importLibrary(readLibrary(STARTER_ASSET).copy(slots = emptyMap()), ImportMode.Merge)

    /** Adds the recipes adapted from Fujifilm recipes (same rule as above: same ids are reset, photos stay). */
    suspend fun addFujiAdaptions(): Int = importLibrary(readLibrary(FUJI_ASSET).copy(slots = emptyMap()), ImportMode.Merge)

    suspend fun get(id: String): Recipe? = dao.getRecipe(id)?.toDomain()

    /** Saves a recipe; assigns id and order for new ones. Returns the stored recipe. */
    suspend fun save(recipe: Recipe): Recipe {
        val existing = dao.getRecipes()
        val ids = existing.map { it.id }.toSet()
        val id = recipe.id.ifBlank { uniqueId(recipe.name, ids) }
        val order = if (recipe.id.isBlank() || recipe.order <= 0) (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1 else recipe.order
        val stored = recipe.copy(id = id, order = order, updatedAt = System.currentTimeMillis())
        dao.upsert(stored.toEntity())
        return stored
    }

    suspend fun duplicate(id: String): Recipe? {
        val src = get(id) ?: return null
        return save(src.copy(id = "", order = 0, name = src.name + " (Variante)", photos = emptyList()))
    }

    /** Deletes a recipe, frees its slot and removes its photo files. */
    suspend fun delete(id: String) {
        val r = get(id)
        val current = dao.getSlots().associate { it.slot to it.recipeId }
        dao.delete(id)
        dao.replaceSlots(SlotLogic.release(current, id).map { SlotEntity(it.key, it.value) })
        r?.photos?.forEach(photos::delete)
    }

    suspend fun setSlot(slot: String, recipeId: String?) {
        val current = dao.getSlots().associate { it.slot to it.recipeId }
        val next = SlotLogic.assign(current, slot, recipeId)
        dao.replaceSlots(next.map { SlotEntity(it.key, it.value) })
    }

    /** Moves a recipe to [slot], or frees its slot when [slot] is null. */
    suspend fun setRecipeSlot(recipeId: String, slot: String?) {
        val current = dao.getSlots().associate { it.slot to it.recipeId }
        val next = if (slot == null) SlotLogic.release(current, recipeId) else SlotLogic.assign(current, slot, recipeId)
        dao.replaceSlots(next.map { SlotEntity(it.key, it.value) })
    }

    // ---------- photos ----------

    suspend fun addPhotos(recipeId: String, uris: List<Uri>): Int {
        var added = 0
        var r = get(recipeId) ?: return 0
        for (uri in uris) {
            val name = photos.import(uri) ?: continue
            r = r.copy(photos = r.photos + name)
            added++
        }
        if (added > 0) dao.upsert(r.copy(updatedAt = System.currentTimeMillis()).toEntity())
        return added
    }

    suspend fun removePhoto(recipeId: String, name: String) {
        val r = get(recipeId) ?: return
        dao.upsert(r.copy(photos = r.photos - name, updatedAt = System.currentTimeMillis()).toEntity())
        photos.delete(name)
    }

    // ---------- export / import ----------

    suspend fun exportJson(): String {
        val lib = Library(dao.getRecipes().map { it.toDomain() }, dao.getSlots().associate { it.slot to it.recipeId })
        return RecipeJson.writeLibrary(lib, notes = "Export aus Rezeptbuch α7R III")
    }

    /** Parses JSON text without touching the database; throws [RecipeJson.FormatException]. */
    fun parse(text: String): Library = RecipeJson.parseLibrary(text)

    /**
     * Merge: recipes with the same id are overwritten (existing photos are kept), others are added,
     * slots from the file replace colliding slots. Replace: wipes recipes and slots first.
     * Photo names in a JSON file are only kept when the file exists locally.
     */
    suspend fun importLibrary(library: Library, mode: ImportMode): Int {
        if (mode == ImportMode.Replace) {
            dao.getRecipes().forEach { e -> e.toDomain().photos.forEach(photos::delete) }
            dao.deleteAllRecipes()
            dao.clearSlots()
        }
        val existing = dao.getRecipes().associateBy { it.id }
        var nextOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: 0)
        library.recipes.forEach { incoming ->
            val old = existing[incoming.id]?.toDomain()
            val importedPhotos = incoming.photos.filter(photos::exists)
            val merged = incoming.copy(
                order = old?.order ?: if (mode == ImportMode.Replace) incoming.order else ++nextOrder,
                photos = ((old?.photos.orEmpty()) + importedPhotos).distinct(),
                updatedAt = if (incoming.updatedAt > 0) incoming.updatedAt else System.currentTimeMillis(),
            )
            dao.upsert(merged.toEntity())
        }
        var slots = dao.getSlots().associate { it.slot to it.recipeId }
        library.slots.forEach { (slot, id) -> slots = SlotLogic.assign(slots, slot, id) }
        val allIds = dao.getRecipes().map { it.id }.toSet()
        dao.replaceSlots(SlotLogic.sanitize(slots, allIds).map { SlotEntity(it.key, it.value) })
        return library.recipes.size
    }

    private fun uniqueId(name: String, taken: Set<String>): String {
        val base = RecipeJson.slug(name)
        if (base !in taken) return base
        var i = 2
        while ("$base-$i" in taken) i++
        return "$base-$i"
    }

    // ---------- mapping ----------

    private fun Recipe.toEntity(): RecipeEntity {
        val j = RecipeJson.recipeToJson(this)
        return RecipeEntity(
            id = id, sortOrder = order, name = name, look = look, notes = notes, source = source, sourceUrl = sourceUrl,
            tagsJson = JsonArray(tags.map(::JsonPrimitive)).toString(),
            photosJson = JsonArray(photos.map(::JsonPrimitive)).toString(),
            ppJson = j["pp"].toString(), wbJson = j["wb"].toString(), updatedAt = updatedAt,
        )
    }

    private fun RecipeEntity.toDomain(): Recipe {
        val o = JsonObject(
            mapOf(
                "id" to JsonPrimitive(id), "order" to JsonPrimitive(sortOrder), "name" to JsonPrimitive(name),
                "look" to JsonPrimitive(look), "notes" to JsonPrimitive(notes), "source" to JsonPrimitive(source),
                "sourceUrl" to JsonPrimitive(sourceUrl),
                "tags" to Json.parseToJsonElement(tagsJson), "photos" to Json.parseToJsonElement(photosJson),
                "pp" to Json.parseToJsonElement(ppJson), "wb" to Json.parseToJsonElement(wbJson),
            ),
        )
        return RecipeJson.parseRecipe(o).copy(updatedAt = updatedAt)
    }

    private companion object {
        const val KEY_SEEDED = "seeded"
        const val STARTER_ASSET = "recipes.json"
        const val FUJI_ASSET = "fuji-adaptions.json"
    }
}
