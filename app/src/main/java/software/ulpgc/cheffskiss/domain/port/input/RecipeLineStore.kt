package software.ulpgc.cheffskiss.domain.port.input

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine

interface RecipeLineStore {
    fun linesOf(recipe: Recipe): Flow<List<RecipeLine>>
}