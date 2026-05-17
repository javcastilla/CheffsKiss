package software.ulpgc.cheffskiss.ui.navigation

object FocusModeNavigation {
    const val ROUTE = "recipe_focus/{recipeId}"

    fun route(recipeId: String): String = "recipe_focus/$recipeId"
}
