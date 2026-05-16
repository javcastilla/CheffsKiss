package software.ulpgc.cheffskiss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import software.ulpgc.cheffskiss.ui.navigation.MealPlanNavigation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.LibraryViewModel
import software.ulpgc.cheffskiss.ui.MealPlanDetailViewModel
import software.ulpgc.cheffskiss.ui.MealPlanViewModel
import software.ulpgc.cheffskiss.ui.RecipeDetailViewModel
import software.ulpgc.cheffskiss.ui.ExploreViewModel
import software.ulpgc.cheffskiss.ui.theme.CheffsKissTheme
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.screen.AllRecipesScreen
import software.ulpgc.cheffskiss.ui.screen.EditRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.ExploreScreen
import software.ulpgc.cheffskiss.ui.screen.CreateRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.HomeRoute
import software.ulpgc.cheffskiss.ui.screen.LibraryScreen
import software.ulpgc.cheffskiss.ui.screen.LoginScreen
import software.ulpgc.cheffskiss.ui.screen.MealPlanDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RecipeDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheffsKissTheme {
                val navController = rememberNavController()
                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                            },
                            onGoToRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                            },
                            onGoToLogin = { navController.popBackStack() }
                        )
                    }

                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel()
                        HomeRoute(
                            viewModel       = homeViewModel,
                            onCreateRecipe  = { navController.navigate("create_recipe") },
                            onLibraryClick  = { navController.navigate("library") { launchSingleTop = true } },
                            onExploreClick  = { navController.navigate("explore") { launchSingleTop = true } },
                            onLogout        = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                            onRecipeClick   = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onViewAll       = { navController.navigate("all_recipes") },
                            onMealPlanClick = { planId -> navController.navigate("meal_plan_detail/$planId") }
                        )
                    }

                    composable("all_recipes") {
                        val homeEntry = remember { navController.getBackStackEntry("home") }
                        val homeViewModel: HomeViewModel = viewModel(homeEntry)
                        val state by homeViewModel.uiState.collectAsState()
                        val authorNames by homeViewModel.authorNames.collectAsState()
                        AllRecipesScreen(
                            recipes        = state.recipes,
                            savedRecipeIds = state.savedRecipeIds,
                            authorNames    = authorNames,
                            currentUserId  = state.currentUserId,
                            onBack         = { navController.popBackStack() },
                            onRecipeClick  = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onToggleSave   = homeViewModel::toggleSave
                        )
                    }

                    composable("library") {
                        LibraryScreen(
                            viewModel         = viewModel<LibraryViewModel>(),
                            mealPlanViewModel = viewModel<MealPlanViewModel>(),
                            onGoHome          = { navController.navigate("home") { launchSingleTop = true } },
                            onExploreClick    = { navController.navigate("explore") { launchSingleTop = true } },
                            onCreateRecipe    = { navController.navigate("create_recipe") },
                            onRecipeClick     = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onMealPlanClick   = { plan -> navController.navigate("meal_plan_detail/${plan.id}") }
                        )
                    }

                    composable("explore") {
                        ExploreScreen(
                            viewModel     = viewModel<ExploreViewModel>(),
                            onRecipeClick = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onHomeClick   = { navController.navigate("home") { launchSingleTop = true } },
                            onCreateClick = { navController.navigate("create_recipe") },
                            onSavedClick  = { navController.navigate("library") { launchSingleTop = true } }
                        )
                    }

                    composable("create_recipe") {
                        CreateRecipeScreen(
                            onBack           = { navController.popBackStack() },
                            onPublishSuccess  = {
                                navController.navigate("home") { popUpTo("home") { inclusive = false } }
                            },
                            onSaveDraft      = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "recipe_detail/{recipeId}?pickForMealSlot={pickForMealSlot}",
                        arguments = listOf(
                            navArgument("recipeId") { type = NavType.StringType },
                            navArgument("pickForMealSlot") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                        val pickForMealSlot = backStackEntry.arguments?.getBoolean("pickForMealSlot") ?: false
                        val detailViewModel: RecipeDetailViewModel = viewModel()
                        val recipe     by detailViewModel.recipe.collectAsState()
                        val authorName by detailViewModel.authorName.collectAsState()
                        val isSaved    by detailViewModel.isSaved.collectAsState()
                        val isOwner    by detailViewModel.isOwner.collectAsState()
                        val lines      by detailViewModel.lines.collectAsState()
                        val steps      by detailViewModel.steps.collectAsState()

                        LaunchedEffect(recipeId) { detailViewModel.load(recipeId) }

                        if (recipe == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else {
                            RecipeDetailScreen(
                                recipe     = recipe!!,
                                lines      = lines,
                                steps      = steps,
                                authorName = authorName,
                                isSaved    = isSaved,
                                isOwner    = isOwner,
                                onBack     = {
                                    if (pickForMealSlot) {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set(MealPlanNavigation.PICK_FLOW_CANCELLED_KEY, true)
                                    }
                                    navController.popBackStack()
                                },
                                onSave     = { detailViewModel.toggleSave() },
                                onDelete   = { detailViewModel.deleteRecipe { navController.popBackStack() } },
                                onEdit     = { navController.navigate("edit_recipe/${recipe!!.id}") },
                                pickForMealSlot = pickForMealSlot,
                                onAddToMealSlot = {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(MealPlanNavigation.PICKED_RECIPE_ID_KEY, recipeId)
                                    navController.popBackStack()
                                },
                            )
                        }
                    }

                    composable(
                        route = "edit_recipe/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                        val detailEntry = remember(recipeId) {
                            navController.getBackStackEntry("recipe_detail/$recipeId")
                        }
                        val detailViewModel: RecipeDetailViewModel = viewModel(detailEntry)
                        val recipe by detailViewModel.recipe.collectAsState()
                        val lines  by detailViewModel.lines.collectAsState()
                        val steps  by detailViewModel.steps.collectAsState()

                        if (recipe == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else {
                            EditRecipeScreen(
                                recipe          = recipe!!,
                                initialLines    = lines,
                                initialSteps    = steps,
                                onBack          = { navController.popBackStack() },
                                onUpdateSuccess = { navController.popBackStack() }
                            )
                        }
                    }

                    composable(
                        route = "meal_plan_detail/{planId}",
                        arguments = listOf(navArgument("planId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val planId = backStackEntry.arguments?.getString("planId") ?: return@composable
                        val mealPlanEntry = remember(planId) {
                            navController.getBackStackEntry("meal_plan_detail/$planId")
                        }
                        val mealPlanDetailViewModel: MealPlanDetailViewModel = viewModel()
                        LaunchedEffect(mealPlanEntry) {
                            val handle = mealPlanEntry.savedStateHandle
                            launch {
                                handle.getStateFlow<String?>(MealPlanNavigation.PICKED_RECIPE_ID_KEY, null)
                                    .collect { pickedId ->
                                        if (pickedId != null) {
                                            mealPlanDetailViewModel.applyPickedRecipe(pickedId)
                                            handle[MealPlanNavigation.PICKED_RECIPE_ID_KEY] = null
                                        }
                                    }
                            }
                            launch {
                                handle.getStateFlow(MealPlanNavigation.PICK_FLOW_CANCELLED_KEY, false)
                                    .collect { cancelled ->
                                        if (cancelled) {
                                            mealPlanDetailViewModel.restoreSlotFormFromPickFlow()
                                            handle[MealPlanNavigation.PICK_FLOW_CANCELLED_KEY] = false
                                        }
                                    }
                            }
                        }
                        MealPlanDetailScreen(
                            planId        = planId,
                            viewModel     = mealPlanDetailViewModel,
                            onBack        = { navController.popBackStack() },
                            onRecipeClick = { recipeId ->
                                navController.navigate(MealPlanNavigation.recipeDetailRoute(recipeId))
                            },
                            onPickerRecipeClick = { recipeId ->
                                navController.navigate(
                                    MealPlanNavigation.recipeDetailRoute(recipeId, pickForMealSlot = true)
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}