package de.martinkloeckner.rezeptbuch.data

import de.martinkloeckner.rezeptbuch.domain.BlackGamma
import de.martinkloeckner.rezeptbuch.domain.Depth
import de.martinkloeckner.rezeptbuch.domain.Detail
import de.martinkloeckner.rezeptbuch.domain.Knee
import de.martinkloeckner.rezeptbuch.domain.Library
import de.martinkloeckner.rezeptbuch.domain.Options
import de.martinkloeckner.rezeptbuch.domain.PictureProfile
import de.martinkloeckner.rezeptbuch.domain.Recipe
import de.martinkloeckner.rezeptbuch.domain.SlotLogic
import de.martinkloeckner.rezeptbuch.domain.WhiteBalance
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reads and writes the library JSON format (identical to `data/recipes.json`).
 * Empty values (`""`) mean "not set" and map to null. Numbers may be given as strings.
 */
object RecipeJson {
    const val SCHEMA_VERSION = 1
    const val CAMERA = "Sony ILCE-7RM3 (α7R III)"

    private val pretty = Json { prettyPrint = true; prettyPrintIndent = "  " }

    class FormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

    // ---------- reading ----------

    fun parseLibrary(text: String): Library {
        val root = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw FormatException("Keine gültige Rezept-Datei", e)
        }
        val recipes = (root["recipes"] as? JsonArray)
            ?: throw FormatException("Die Datei enthält keine Rezepte")
        val parsed = recipes.mapIndexedNotNull { i, el -> (el as? JsonObject)?.let { parseRecipe(it, i + 1) } }
        val slots = (root["slots"] as? JsonObject).orEmpty().mapNotNull { (k, v) ->
            val id = (v as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            if (id != null) k to id else null
        }.toMap()
        return Library(parsed, SlotLogic.sanitize(slots, parsed.map { it.id }.toSet()))
    }

    fun parseRecipe(o: JsonObject, fallbackOrder: Int = 0): Recipe {
        val name = o.str("name")
        val pp = (o["pp"] as? JsonObject).orEmpty()
        val bg = (pp["blackGamma"] as? JsonObject).orEmpty()
        val kn = (pp["knee"] as? JsonObject).orEmpty()
        val dp = (pp["depth"] as? JsonObject).orEmpty()
        val dt = (pp["detail"] as? JsonObject).orEmpty()
        val wb = (o["wb"] as? JsonObject).orEmpty()
        return Recipe(
            id = o.str("id").ifBlank { slug(name) },
            order = o.int("order") ?: fallbackOrder,
            name = name,
            look = o.str("look"),
            tags = o.strList("tags"),
            notes = o.str("notes"),
            source = o.str("source"),
            sourceUrl = o.str("sourceUrl"),
            photos = o.strList("photos"),
            pp = PictureProfile(
                blackLevel = pp.int("blackLevel"),
                gamma = pp.str("gamma"),
                blackGamma = BlackGamma(bg.str("range"), bg.int("level")),
                knee = Knee(kn.str("mode"), kn.dbl("point"), kn.int("slope")),
                colorMode = pp.str("colorMode"),
                saturation = pp.int("saturation"),
                colorPhase = pp.int("colorPhase"),
                depth = Depth(dp.int("R"), dp.int("G"), dp.int("B"), dp.int("C"), dp.int("M"), dp.int("Y")),
                detail = Detail(
                    level = dt.int("level"), mode = dt.str("mode"), vh = dt.int("vh"), bw = dt.str("bw"),
                    limit = dt.int("limit"), crisp = dt.int("crisp"), hl = dt.int("hl"),
                ),
            ),
            wb = WhiteBalance(wb.int("kelvin"), wb.dbl("ab") ?: 0.0, wb.dbl("gm") ?: 0.0),
            updatedAt = parseInstant(o.str("updatedAt").ifBlank { o.str("updated") }),
        )
    }

    // ---------- writing ----------

    fun writeLibrary(library: Library, notes: String = ""): String = pretty.encodeToString(
        JsonElement.serializer(),
        buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("camera", CAMERA)
            put("notes", notes)
            put("recipes", JsonArray(library.recipes.sortedBy { it.order }.map(::recipeToJson)))
            putJsonObject("slots") {
                Options.slots.forEach { s -> library.slots[s]?.let { put(s, it) } }
            }
        },
    )

    fun recipeToJson(r: Recipe): JsonObject = buildJsonObject {
        put("id", r.id)
        put("order", r.order)
        put("name", r.name)
        put("look", r.look)
        put("tags", JsonArray(r.tags.map(::JsonPrimitive)))
        put("notes", r.notes)
        put("source", r.source)
        put("sourceUrl", r.sourceUrl)
        put("photos", JsonArray(r.photos.map(::JsonPrimitive)))
        putJsonObject("pp") {
            put("blackLevel", intOrEmpty(r.pp.blackLevel))
            put("gamma", r.pp.gamma)
            putJsonObject("blackGamma") {
                put("range", r.pp.blackGamma.range)
                put("level", intOrEmpty(r.pp.blackGamma.level))
            }
            putJsonObject("knee") {
                put("mode", r.pp.knee.mode)
                put("point", dblOrEmpty(r.pp.knee.point))
                put("slope", intOrEmpty(r.pp.knee.slope))
            }
            put("colorMode", r.pp.colorMode)
            put("saturation", intOrEmpty(r.pp.saturation))
            put("colorPhase", intOrEmpty(r.pp.colorPhase))
            putJsonObject("depth") { r.pp.depth.entries.forEach { (k, v) -> put(k, intOrEmpty(v)) } }
            putJsonObject("detail") {
                put("level", intOrEmpty(r.pp.detail.level))
                put("mode", r.pp.detail.mode)
                put("vh", intOrEmpty(r.pp.detail.vh))
                put("bw", r.pp.detail.bw)
                put("limit", intOrEmpty(r.pp.detail.limit))
                put("crisp", intOrEmpty(r.pp.detail.crisp))
                put("hl", intOrEmpty(r.pp.detail.hl))
            }
        }
        putJsonObject("wb") {
            put("kelvin", intOrEmpty(r.wb.kelvin))
            put("ab", dblOrEmpty(r.wb.ab))
            put("gm", dblOrEmpty(r.wb.gm))
        }
        if (r.updatedAt > 0) put("updatedAt", Instant.ofEpochMilli(r.updatedAt).toString())
    }

    // ---------- helpers ----------

    /** URL-safe id from a name; falls back to "rezept". */
    fun slug(name: String): String {
        val folded = java.text.Normalizer.normalize(name.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return folded.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).ifEmpty { "rezept" }
    }

    private fun parseInstant(s: String): Long =
        if (s.isBlank()) 0L else runCatching { Instant.parse(s).toEpochMilli() }.getOrDefault(0L)

    private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())

    private fun JsonObject.raw(key: String): String? {
        val p = this[key] as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        return p.content.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)
        ?.takeIf { it !is JsonNull }?.content.orEmpty()

    private fun JsonObject.dbl(key: String): Double? = raw(key)?.replace(',', '.')?.toDoubleOrNull()

    private fun JsonObject.int(key: String): Int? = dbl(key)?.roundToInt()

    private fun JsonObject.strList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()

    private fun intOrEmpty(v: Int?): JsonPrimitive = if (v == null) JsonPrimitive("") else JsonPrimitive(v)

    private fun dblOrEmpty(v: Double?): JsonPrimitive = when {
        v == null -> JsonPrimitive("")
        v == Math.rint(v) -> JsonPrimitive(v.toLong())
        else -> JsonPrimitive(v)
    }
}
