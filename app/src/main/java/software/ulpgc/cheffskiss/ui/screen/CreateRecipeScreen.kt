package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.runtime.Composable
@Composable
fun CreateRecipeScreen(
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    RecipeFormScreen(
        mode = RecipeFormMode.Create,
        onBack = onBack,
        onSuccess = onPublishSuccess,
        onSaveDraft = onSaveDraft,
    )
}
