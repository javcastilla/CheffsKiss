package software.ulpgc.cheffskiss

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import coil.compose.LocalImageLoader
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.ui.ExploreViewModel
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.LibraryViewModel
import software.ulpgc.cheffskiss.ui.MealPlanDetailViewModel
import software.ulpgc.cheffskiss.ui.MealPlanViewModel
import software.ulpgc.cheffskiss.ui.RecipeCollectionDetailViewModel
import software.ulpgc.cheffskiss.ui.RecipeCollectionViewModel
import software.ulpgc.cheffskiss.ui.RecipeDetailViewModel
import software.ulpgc.cheffskiss.ui.FocusModeViewModel
import software.ulpgc.cheffskiss.ui.navigation.FocusModeNavigation
import software.ulpgc.cheffskiss.ui.navigation.MainBottomNavigation
import software.ulpgc.cheffskiss.ui.navigation.MealPlanNavigation
import software.ulpgc.cheffskiss.ui.screen.focus.FocusModeScreen
import software.ulpgc.cheffskiss.ui.screen.SocialProfileScreen
import software.ulpgc.cheffskiss.ui.screen.AllRecipesScreen
import software.ulpgc.cheffskiss.ui.screen.CreateRecipeCollectionScreen
import software.ulpgc.cheffskiss.ui.screen.CreateRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.EditRecipeCollectionScreen
import software.ulpgc.cheffskiss.ui.screen.EditRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.ExploreScreen
import software.ulpgc.cheffskiss.ui.screen.HomeRoute
import software.ulpgc.cheffskiss.ui.screen.LibraryScreen
import software.ulpgc.cheffskiss.ui.screen.LoginScreen
import software.ulpgc.cheffskiss.ui.screen.MealPlanDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RecipeCollectionDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RecipeDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RegisterScreen
import software.ulpgc.cheffskiss.infrastructure.coil.RecipePhotoImageLoaderFactory
import software.ulpgc.cheffskiss.ui.theme.CheffsKissTheme
import software.ulpgc.cheffskiss.ui.theme.Primary

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) {
                RecipePhotoImageLoaderFactory.create(context.applicationContext).also {
                    Coil.setImageLoader(it)
                }
            }
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
            CheffsKissTheme {
                val navController = rememberNavController()
                val startDestination =
                    if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startDestination) {

                    val navigateHome = {
                        navController.navigate(MainBottomNavigation.HOME) {
                            launchSingleTop = true
                        }
                    }
                    val navigateExplore = {
                        navController.navigate(MainBottomNavigation.EXPLORE) {
                            launchSingleTop = true
                        }
                    }
                    val navigateLibrary = {
                        navController.navigate(MainBottomNavigation.libraryRoute()) {
                            launchSingleTop = true
                        }
                    }
                    val navigateProfile = {
                        navController.navigate(MainBottomNavigation.PROFILE) {
                            launchSingleTop = true
                        }
                    }
                    val navigateCreateRecipe = { navController.navigate("create_recipe") }
                    val navigateCreateList = { navController.navigate("create_collection") }
                    val navigateCreateMealPlan = {
                        navController.navigate(MainBottomNavigation.libraryRoute(tab = 2, createMealPlan = true)) {
                            launchSingleTop = true
                        }
                    }

                    // ── Auth ──────────────────────────────────────────────────
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToLogin = { navController.popBackStack() }
                        )
                    }

                    // ── Home ──────────────────────────────────────────────────
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel()
                        HomeRoute(
                            viewModel        = homeViewModel,
                            onCreateRecipe   = navigateCreateRecipe,
                            onCreateMealPlan = navigateCreateMealPlan,
                            onCreateList     = navigateCreateList,
                            onLibraryClick   = navigateLibrary,
                            onExploreClick   = navigateExplore,
                            onProfileClick   = navigateProfile,
                            onLogout         = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                            onRecipeClick   = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onViewAll       = { navController.navigate("all_recipes") },
                            onMealPlanClick = { planId -> navController.navigate("meal_plan_detail/$planId") }
                        )
                    }

                    composable("all_recipes") {
                        val homeEntry = remember(navController) {
                            navController.getBackStackEntry("home")
                        }
                        val homeViewModel: HomeViewModel = viewModel(homeEntry)
                        val state       by homeViewModel.uiState.collectAsState()
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
                    // ── Library ───────────────────────────────────────────────
                    composable(
                        route = MainBottomNavigation.LIBRARY_ROUTE,
                        arguments = listOf(
                            navArgument("tab") {
                                type = NavType.IntType
                                defaultValue = 0
                            },
                            navArgument("createMealPlan") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                    ) { backStackEntry ->
                        val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                        val openMealPlanCreate =
                            backStackEntry.arguments?.getBoolean("createMealPlan") ?: false
                        LibraryScreen(
                            viewModel          = viewModel<LibraryViewModel>(),
                            mealPlanViewModel  = viewModel<MealPlanViewModel>(),
                            onGoHome           = navigateHome,
                            onExploreClick     = navigateExplore,
                            onProfileClick     = navigateProfile,
                            onCreateRecipe     = navigateCreateRecipe,
                            onCreateMealPlan   = navigateCreateMealPlan,
                            onCreateList       = navigateCreateList,
                            onRecipeClick      = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onMealPlanClick    = { plan -> navController.navigate("meal_plan_detail/${plan.id}") },
                            onCollectionClick  = { collection -> navController.navigate("collectiondetail/${collection.id}") },
                            initialTab         = tab,
                            openMealPlanCreate = openMealPlanCreate,
                        )
                    }

                    // ── Collections ───────────────────────────────────────────
                    composable("create_collection") {
                        val collectionViewModel: RecipeCollectionViewModel = viewModel()
                        CreateRecipeCollectionScreen(
                            viewModel       = collectionViewModel,
                            onBack          = { navController.popBackStack() },
                            onCreateSuccess = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "collectiondetail/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId")
                            ?: return@composable

                        val detailViewModel: RecipeCollectionDetailViewModel = viewModel()
                        val collectionViewModel: RecipeCollectionViewModel = viewModel()

                        val state by detailViewModel.uiState.collectAsState()

                        RecipeCollectionDetailScreen(
                            collectionId  = collectionId,
                            viewModel     = detailViewModel,
                            onBack        = { navController.popBackStack() },
                            onRecipeClick = { recipeId -> navController.navigate("recipe_detail/$recipeId") },
                            onEdit        = { navController.navigate("edit_collection/$collectionId") },
                            onDelete      = {
                                val collection = state.collection
                                if (collection != null) {
                                    collectionViewModel.deleteCollection(
                                        collectionID = collection.id,
                                        userId       = collection.userId
                                    )
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(
                        route = "edit_collection/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId")
                            ?: return@composable
                        val vm: RecipeCollectionDetailViewModel = viewModel()
                        EditRecipeCollectionScreen(
                            collectionId = collectionId,
                            viewModel    = vm,
                            onBack       = { navController.popBackStack() },
                            onSaveSuccess = { navController.popBackStack() }
                        )
                    }

                    // ── Explore ───────────────────────────────────────────────
                    composable("explore") {
                        ExploreScreen(
                            viewModel        = viewModel<ExploreViewModel>(),
                            onRecipeClick    = { recipe -> navController.navigate("recipe_detail/${recipe.id}") },
                            onHomeClick      = navigateHome,
                            onLibraryClick   = navigateLibrary,
                            onProfileClick   = navigateProfile,
                            onCreateRecipe   = navigateCreateRecipe,
                            onCreateMealPlan = navigateCreateMealPlan,
                            onCreateList     = navigateCreateList,
                        )
                    }

                    composable(MainBottomNavigation.PROFILE) {
                        SocialProfileScreen(
                            onHomeClick      = navigateHome,
                            onExploreClick   = navigateExplore,
                            onLibraryClick   = navigateLibrary,
                            onCreateRecipe   = navigateCreateRecipe,
                            onCreateMealPlan = navigateCreateMealPlan,
                            onCreateList     = navigateCreateList,
                            onLogout         = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                        )
                    }

                    // ── Recipes ───────────────────────────────────────────────
                    composable("create_recipe") {
                        CreateRecipeScreen(
                            onBack          = { navController.popBackStack() },
                            onPublishSuccess = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onSaveDraft = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "recipe_detail/{recipeId}?pickForMealSlot={pickForMealSlot}",
                        arguments = listOf(
                            navArgument("recipeId") { type = NavType.StringType },
                            navArgument("pickForMealSlot") {
                                type         = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val recipeId        = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                        val pickForMealSlot = backStackEntry.arguments?.getBoolean("pickForMealSlot") ?: false
                        val detailViewModel: RecipeDetailViewModel = viewModel()

                        val recipe     by detailViewModel.recipe.collectAsState()
                        val authorName by detailViewModel.authorName.collectAsState()
                        val isSaved    by detailViewModel.isSaved.collectAsState()
                        val isOwner    by detailViewModel.isOwner.collectAsState()
                        val lines      by detailViewModel.lines.collectAsState()
                        val steps      by detailViewModel.steps.collectAsState()

                        LaunchedEffect(recipeId) {
                            detailViewModel.load(recipeId)
                        }

                        if (recipe == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else {
                            RecipeDetailScreen(
                                recipe          = recipe!!,
                                lines           = lines,
                                steps           = steps,
                                authorName      = authorName,
                                isSaved         = isSaved,
                                isOwner         = isOwner,
                                onBack          = {
                                    if (pickForMealSlot) {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set(MealPlanNavigation.PICK_FLOW_CANCELLED_KEY, true)
                                    }
                                    navController.popBackStack()
                                },
                                onSave          = { detailViewModel.toggleSave() },
                                onDelete        = { detailViewModel.deleteRecipe { navController.popBackStack() } },
                                onEdit          = { navController.navigate("edit_recipe/${recipe!!.id}") },
                                onStartFocus    = {
                                    navController.navigate(FocusModeNavigation.route(recipeId))
                                },
                                pickForMealSlot = pickForMealSlot,
                                onAddToMealSlot = {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(MealPlanNavigation.PICKED_RECIPE_ID_KEY, recipeId)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    composable(
                        route = FocusModeNavigation.ROUTE,
                        arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val focusRecipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                        LaunchedEffect(focusRecipeId) {
                            backStackEntry.savedStateHandle[FocusModeViewModel.ARG_RECIPE_ID] = focusRecipeId
                        }
                        val focusViewModel: FocusModeViewModel = viewModel(backStackEntry)
                        val detailEntry = remember(focusRecipeId) {
                            runCatching {
                                navController.getBackStackEntry(
                                    "recipe_detail/$focusRecipeId?pickForMealSlot=false",
                                )
                            }.getOrNull()
                        }
                        val detailViewModel: RecipeDetailViewModel? = detailEntry?.let { viewModel(it) }

                        if (detailViewModel != null) {
                            val isSaved by detailViewModel.isSaved.collectAsState()
                            val seedRecipe by detailViewModel.recipe.collectAsState()
                            val seedLines by detailViewModel.lines.collectAsState()
                            val seedSteps by detailViewModel.steps.collectAsState()
                            FocusModeScreen(
                                viewModel = focusViewModel,
                                onExit = { navController.popBackStack() },
                                onViewRecipeDetail = { navController.popBackStack() },
                                seedRecipe = seedRecipe,
                                seedLines = seedLines,
                                seedSteps = seedSteps,
                                isSaved = isSaved,
                                onToggleSave = { detailViewModel.toggleSave() },
                            )
                        } else {
                            FocusModeScreen(
                                viewModel = focusViewModel,
                                onExit = { navController.popBackStack() },
                                onViewRecipeDetail = { navController.popBackStack() },
                            )
                        }
                    }

                    composable(
                        route = "edit_recipe/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable

                        val detailEntry = remember(recipeId) {
                            navController.getBackStackEntry(
                                "recipe_detail/{recipeId}?pickForMealSlot={pickForMealSlot}"
                            )
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
                    // ── Meal Plan ─────────────────────────────────────────────
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
                            planId    = planId,
                            viewModel = mealPlanDetailViewModel,
                            onBack    = { navController.popBackStack() },
                            onRecipeClick = { recipeId ->
                                navController.navigate(MealPlanNavigation.recipeDetailRoute(recipeId))
                            },
                            onPickerRecipeClick = { recipeId ->
                                navController.navigate(
                                    MealPlanNavigation.recipeDetailRoute(recipeId, pickForMealSlot = true)
                                )
                            }
                        )
                    }
                }
            }
            }
        }
    }
}