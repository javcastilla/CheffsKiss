package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import software.ulpgc.cheffskiss.ui.theme.Background

@Composable
fun TabScaffold(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateRecipe: () -> Unit,
    onCreateMealPlan: () -> Unit,
    onCreateList: () -> Unit,
    containerColor: Color = Background,
    content: @Composable (PaddingValues) -> Unit,
) {
    var createSheetVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            bottomBar = {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onHomeClick = onHomeClick,
                    onExploreClick = onExploreClick,
                    onLibraryClick = onLibraryClick,
                    onProfileClick = onProfileClick,
                    createExpanded = createSheetVisible,
                    onCreateToggle = { createSheetVisible = !createSheetVisible },
                )
            },
            content = content,
        )

        Box(Modifier.zIndex(4f)) {
        CreateActionSheetOverlay(
            visible = createSheetVisible,
            onDismiss = { createSheetVisible = false },
            onCreateRecipe = {
                createSheetVisible = false
                onCreateRecipe()
            },
            onCreateMealPlan = {
                createSheetVisible = false
                onCreateMealPlan()
            },
            onCreateList = {
                createSheetVisible = false
                onCreateList()
            },
        )
        }
    }
}
