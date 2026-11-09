package de.martinkloeckner.rezeptbuch.domain

/** Logic for the ten picture-profile slots PP1–PP10. A recipe occupies at most one slot. */
object SlotLogic {

    /** Assigns [recipeId] to [slot] (or frees it when null), removing the recipe from any other slot. */
    fun assign(slots: Map<String, String>, slot: String, recipeId: String?): Map<String, String> {
        require(slot in Options.slots) { "Unknown slot $slot" }
        val result = slots.toMutableMap()
        if (recipeId != null) result.entries.removeAll { it.value == recipeId }
        if (recipeId == null) result.remove(slot) else result[slot] = recipeId
        return result
    }

    /** Frees whatever slot the recipe occupies. */
    fun release(slots: Map<String, String>, recipeId: String): Map<String, String> =
        slots.filterValues { it != recipeId }

    fun slotOf(slots: Map<String, String>, recipeId: String): String? =
        Options.slots.firstOrNull { slots[it] == recipeId }

    /** Drops entries for unknown slots or unknown recipes. */
    fun sanitize(slots: Map<String, String>, recipeIds: Set<String>): Map<String, String> {
        val seen = mutableSetOf<String>()
        val out = LinkedHashMap<String, String>()
        for (slot in Options.slots) {
            val id = slots[slot] ?: continue
            if (id in recipeIds && seen.add(id)) out[slot] = id
        }
        return out
    }

    /** Share text: `PP1: Kodak Gold — WB 8000 K · B3 / M1.75` or `PP2: frei`. */
    fun shareText(slots: Map<String, String>, recipes: List<Recipe>): String {
        val byId = recipes.associateBy { it.id }
        return Options.slots.joinToString("\n") { slot ->
            val r = slots[slot]?.let(byId::get)
            if (r == null) "$slot: frei" else "$slot: ${r.name} — WB ${Format.wb(r.wb)}"
        }
    }
}
