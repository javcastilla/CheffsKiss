package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine

interface IngredientStore {
    suspend fun ingredientOf(recipeLine: RecipeLine): Ingredient?
}