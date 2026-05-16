package software.ulpgc.cheffskiss.domain.model.mealplan

import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import java.util.UUID

data class MealSlot(
    val id: UUID,
    val day: WeekDay,
    val mealType: MealType,
    val recipe: Recipe? = null,
    val recipeId: UUID? = null,
) {
    fun resolvedRecipeId(): UUID? = recipe?.id ?: recipeId
}