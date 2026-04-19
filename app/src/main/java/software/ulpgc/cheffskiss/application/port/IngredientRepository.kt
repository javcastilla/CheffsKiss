package software.ulpgc.cheffskiss.application.port

import software.ulpgc.cheffskiss.domain.model.Ingredient

interface IngredientRepository {
    suspend fun search(query: String): List<Ingredient>
    suspend fun getAll(): List<Ingredient>
    suspend fun getById(id: String): Ingredient?
}