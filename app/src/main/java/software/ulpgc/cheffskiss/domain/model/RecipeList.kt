package software.ulpgc.cheffskiss.domain.model

import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import java.util.UUID

data class RecipeList(
    val id: UUID,
    val name: String,
    val recipes: List<Recipe> = emptyList(),
) {
    fun add(recipe: Recipe): RecipeList = copy(recipes = recipes + recipe)
    fun remove(recipe: Recipe): RecipeList = copy(recipes = recipes - recipe)
}