package software.ulpgc.cheffskiss.domain.port.output

import software.ulpgc.cheffskiss.domain.model.Recipe

interface RecipeUpdater {
    suspend fun update(recipe: Recipe);
}