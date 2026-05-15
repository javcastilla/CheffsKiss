package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.domain.model.RecipeLine

interface IngredientStore {
    suspend fun ingredientOf(recipeLine: RecipeLine): Ingredient?
}