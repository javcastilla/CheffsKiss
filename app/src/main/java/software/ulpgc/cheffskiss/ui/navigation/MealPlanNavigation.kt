package software.ulpgc.cheffskiss.ui.navigation

object MealPlanNavigation {
    const val PICKED_RECIPE_ID_KEY = "meal_plan_picked_recipe_id"
    const val PICK_FLOW_CANCELLED_KEY = "meal_plan_pick_flow_cancelled"

    fun recipeDetailRoute(recipeId: String, pickForMealSlot: Boolean = false): String =
        if (pickForMealSlot) "recipe_detail/$recipeId?pickForMealSlot=true"
        else "recipe_detail/$recipeId"
}
