package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.Ingredient

interface IngredientSearchPort {
    suspend fun searchByName(query: String): List<Ingredient>
    suspend fun getAll(): List<Ingredient>
}