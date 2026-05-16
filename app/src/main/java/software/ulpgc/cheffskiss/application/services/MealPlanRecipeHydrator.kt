package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.mealplan.sortedBySchedule
import software.ulpgc.cheffskiss.domain.model.mealplan.sortedSlots
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader

class MealPlanRecipeHydrator(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
) {
    suspend fun hydrate(plan: MealPlan): MealPlan =
        plan.copy(mealSlots = plan.mealSlots.map { hydrateSlot(it) }.sortedBySchedule())

    suspend fun hydrateSlot(slot: MealSlot): MealSlot {
        if (slot.recipe != null) return slot
        val recipeId = slot.recipeId ?: return slot
        val recipe = recipeReader.getById(recipeId.toString()) ?: return slot
        return slot.copy(recipe = recipe)
    }

    suspend fun recipeTitles(plan: MealPlan): Map<String, String> {
        val titles = mutableMapOf<String, String>()
        plan.mealSlots.forEach { slot ->
            val id = slot.resolvedRecipeId()?.toString() ?: return@forEach
            val title = slot.recipe?.title ?: recipeReader.getById(id)?.title
            if (!title.isNullOrBlank()) titles[id] = title
        }
        return titles
    }
}
