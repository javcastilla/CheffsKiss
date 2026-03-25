package software.ulpgc.cheffskiss.application.port.output

import software.ulpgc.cheffskiss.domain.model.Recipe

interface RecipePort {
    suspend fun createRecipe(recipe: Recipe)
}