package software.ulpgc.cheffskiss.application.port.output

import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe

interface RecipePort {
    suspend fun createRecipe(recipe: Recipe)
    suspend fun updateRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipeId: String)
    suspend fun saveRecipe(savedRecipe: SavedRecipe)
    suspend fun deleteSavedRecipe(savedRecipe: SavedRecipe)
    fun getSavedRecipes(userId: String): kotlinx.coroutines.flow.Flow<List<SavedRecipe>>
}