package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader

class GetAllRecipesQuery(private val recipeReader: RecipeReader) {
    operator fun invoke(): Flow<List<Recipe>> = recipeReader.getAll()
}