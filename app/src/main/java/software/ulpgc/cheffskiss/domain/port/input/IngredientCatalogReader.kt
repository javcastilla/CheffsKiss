package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient

interface IngredientCatalogReader {
    suspend fun getAll(limit: Int = 500): List<Ingredient>
    suspend fun getIngredientById(id: String): Ingredient?
    suspend fun searchByPrefix(prefix: String, limit: Int = 20): List<Ingredient>
}
