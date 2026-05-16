package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.port.input.IngredientCatalogReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader

class IngredientCatalogService(
    private val reader: IngredientCatalogReader = FirebaseRecipeReader(),
) {
    private var cachedCatalog: List<Ingredient>? = null

    suspend fun loadCatalog(): List<Ingredient> {
        cachedCatalog?.let { return it }
        return reader.getAll().also { cachedCatalog = it }
    }

    suspend fun getById(id: String): Ingredient? {
        cachedCatalog?.firstOrNull { it.id.toString() == id }?.let { return it }
        return reader.getIngredientById(id)?.also { ingredient ->
            cachedCatalog = (cachedCatalog.orEmpty() + ingredient).distinctBy { it.id }
        }
    }

    fun filterCatalog(catalog: List<Ingredient>, query: String): List<Ingredient> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return catalog
        return catalog.filter { ingredient -> ingredient.matchesQuery(normalized) }
    }

    suspend fun searchRemote(query: String, limit: Int = 20): List<Ingredient> {
        if (query.isBlank()) return emptyList()
        return reader.searchByPrefix(query.trim(), limit)
    }
}

private fun Ingredient.matchesQuery(query: String): Boolean =
    name.lowercase().contains(query) ||
        normalizedName.contains(query) ||
        category.lowercase().contains(query) ||
        subcategory.lowercase().contains(query) ||
        aliases.any { it.lowercase().contains(query) } ||
        tags.any { it.lowercase().contains(query) }
