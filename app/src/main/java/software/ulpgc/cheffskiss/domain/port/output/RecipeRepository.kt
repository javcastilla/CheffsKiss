package software.ulpgc.cheffskiss.domain.port.output

import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader

interface RecipeRepository : RecipeWriter, RecipeReader, RecipeUpdater {

    suspend fun delete(recipe: Recipe)
}