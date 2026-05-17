package software.ulpgc.cheffskiss.ui.navigation

object MainBottomNavigation {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val PROFILE = "profile"

    const val LIBRARY_ROUTE = "library?tab={tab}&createMealPlan={createMealPlan}"

    fun libraryRoute(tab: Int = 0, createMealPlan: Boolean = false): String =
        "library?tab=$tab&createMealPlan=$createMealPlan"
}
