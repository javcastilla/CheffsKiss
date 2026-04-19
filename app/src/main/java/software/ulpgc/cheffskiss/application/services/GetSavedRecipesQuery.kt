package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.SavedRecipe

class GetSavedRecipesQuery(private val recipeRepository: RecipeRepository) {
    operator fun invoke(userId: String): Flow<List<SavedRecipe>> =
        recipeRepository.getSavedRecipes(userId)
}
