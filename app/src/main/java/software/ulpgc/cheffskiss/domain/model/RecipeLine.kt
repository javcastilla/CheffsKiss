package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

data class RecipeLine(
    val id: UUID,
    val amount: Int,
    val measurement:Measurement,
    val ingredients: Ingredient,
    val recipeState: RecipeState
)

enum class Measurement {
    UNIT,
    KILOGRAM,
    GRAM,
    LITRE,
    MILLILITRE,
    CUP,
    TABLESPOON,
    TEASPOON,
    SLICE,
    PINCH,
    TO_TASTE,
    SMALL,
    MEDIUM,
    LARGE
}
