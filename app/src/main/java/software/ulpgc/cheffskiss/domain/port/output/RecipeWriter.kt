package software.ulpgc.cheffskiss.domain.port.output

import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeLine
import software.ulpgc.cheffskiss.domain.model.Step

interface RecipeWriter {
    suspend fun createRecipe(
        recipe: Recipe,
        steps: List<Step>,
        recipeLines: List<RecipeLine>
    )
}