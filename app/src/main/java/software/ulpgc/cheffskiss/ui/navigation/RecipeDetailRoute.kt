package software.ulpgc.cheffskiss.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import software.ulpgc.cheffskiss.ui.RecipeDetailViewModel
import software.ulpgc.cheffskiss.ui.components.SaveRecipeToListHost
import software.ulpgc.cheffskiss.ui.screen.RecipeDetailScreen
import software.ulpgc.cheffskiss.ui.theme.Primary

@Composable
fun RecipeDetailRoute(
    recipeId: String,
    pickForMealSlot: Boolean,
    navController: NavHostController,
) {
    val detailViewModel: RecipeDetailViewModel = viewModel()

    val recipe by detailViewModel.recipe.collectAsState()
    val authorName by detailViewModel.authorName.collectAsState()
    val isSaved by detailViewModel.isSaved.collectAsState()
    val isOwner by detailViewModel.isOwner.collectAsState()
    val lines by detailViewModel.lines.collectAsState()
    val steps by detailViewModel.steps.collectAsState()
    val savePickerState by detailViewModel.savePickerState.collectAsState()

    LaunchedEffect(recipeId) {
        detailViewModel.load(recipeId)
    }

    if (recipe == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
    } else {
        SaveRecipeToListHost(
            pickerState = savePickerState,
            onDismiss = detailViewModel::closeSavePicker,
            onSelect = detailViewModel::selectSaveDestination,
            onConfirm = detailViewModel::confirmSaveToList,
            onConsumeMessage = detailViewModel::consumeSavePickerMessage,
        ) {
            RecipeDetailScreen(
                recipe = recipe!!,
                lines = lines,
                steps = steps,
                authorName = authorName,
                isSaved = isSaved,
                isOwner = isOwner,
                onBack = {
                    if (pickForMealSlot) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(MealPlanNavigation.PICK_FLOW_CANCELLED_KEY, true)
                    }
                    navController.popBackStack()
                },
                onSave = { detailViewModel.openSavePicker() },
                onDelete = { detailViewModel.deleteRecipe { navController.popBackStack() } },
                onEdit = { navController.navigate("edit_recipe/${recipe!!.id}") },
                onStartFocus = {
                    navController.navigate(FocusModeNavigation.route(recipeId))
                },
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
}
