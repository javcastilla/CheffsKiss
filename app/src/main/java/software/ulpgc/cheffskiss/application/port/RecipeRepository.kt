package software.ulpgc.cheffskiss.application.port

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeVersion

interface RecipeRepository {
    suspend fun createRecipe(recipe: Recipe, lines: List<RecipeLine>, steps: List<Step>)
    suspend fun updateRecipe(
        recipe: Recipe,
        lines: List<RecipeLine>,
        steps: List<Step>,
        versionSnapshot: RecipeVersion? = null,
    )
    suspend fun deleteRecipe(recipeId: String)
    suspend fun saveRecipe(savedRecipe: SavedRecipe)
    suspend fun deleteSavedRecipe(savedRecipe: SavedRecipe)
    fun getSavedRecipes(userId: String): Flow<List<SavedRecipe>>
}