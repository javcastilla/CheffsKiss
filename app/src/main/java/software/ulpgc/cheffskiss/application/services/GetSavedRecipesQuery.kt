package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.model.SavedRecipe

class GetSavedRecipesQuery(private val recipePort: RecipePort) {
    operator fun invoke(userId: String): Flow<List<SavedRecipe>> =
        recipePort.getSavedRecipes(userId)
}
