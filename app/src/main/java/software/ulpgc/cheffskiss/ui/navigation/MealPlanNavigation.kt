package software.ulpgc.cheffskiss.ui.navigation

object MealPlanNavigation {
    const val PICKED_RECIPE_ID_KEY = "meal_plan_picked_recipe_id"
    const val PICK_FLOW_CANCELLED_KEY = "meal_plan_pick_flow_cancelled"

    const val RECIPE_DETAIL_PICK_ROUTE = "recipe_detail_pick/{recipeId}"

    fun recipeDetailRoute(recipeId: String): String = "recipe_detail/$recipeId"

    fun recipeDetailPickRoute(recipeId: String): String = "recipe_detail_pick/$recipeId"
}
