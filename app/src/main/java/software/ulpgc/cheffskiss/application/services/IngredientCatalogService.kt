package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import java.util.UUID
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

    fun filterCatalog(
        catalog: List<Ingredient>,
        query: String,
        mode: IngredientSearchMode = IngredientSearchMode.DIRECT,
    ): List<Ingredient> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return catalog
        return catalog.filter { ingredient ->
            when (mode) {
                IngredientSearchMode.DIRECT -> ingredient.matchesDirectQuery(normalized)
                IngredientSearchMode.REVERSE -> ingredient.matchesReverseQuery(normalized)
            }
        }
    }

    fun recipeContainsAllIngredients(recipe: Recipe, ingredientIds: Set<UUID>): Boolean {
        if (ingredientIds.isEmpty()) return true
        val recipeIds = recipe.recipeLines.mapNotNull { it.ingredient?.id }.toSet()
        return ingredientIds.all { it in recipeIds }
    }

    suspend fun searchRemote(query: String, limit: Int = 20): List<Ingredient> {
        if (query.isBlank()) return emptyList()
        return reader.searchByPrefix(query.trim(), limit)
    }
}

private fun Ingredient.searchableText(): String = buildString {
    append(name.lowercase())
    append(' ')
    append(normalizedName)
    append(' ')
    append(category.lowercase())
    append(' ')
    append(subcategory.lowercase())
    aliases.forEach { append(it.lowercase()).append(' ') }
    tags.forEach { append(it.lowercase()).append(' ') }
}

private fun Ingredient.matchesDirectQuery(query: String): Boolean =
    name.lowercase().contains(query) ||
        normalizedName.contains(query) ||
        category.lowercase().contains(query) ||
        subcategory.lowercase().contains(query) ||
        aliases.any { it.lowercase().contains(query) } ||
        tags.any { it.lowercase().contains(query) }

/** Every token in the query must appear somewhere in the ingredient fields (any order). */
private fun Ingredient.matchesReverseQuery(query: String): Boolean {
    val tokens = query.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val haystack = searchableText()
    return tokens.all { token -> haystack.contains(token) }
}
