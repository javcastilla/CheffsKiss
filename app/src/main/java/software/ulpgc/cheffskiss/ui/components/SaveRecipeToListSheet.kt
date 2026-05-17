package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.RecipeLibraryDestination
import software.ulpgc.cheffskiss.ui.SaveRecipePickerUiState
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnPrimary
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveRecipeToListSheet(
    state: SaveRecipePickerUiState,
    onDismiss: () -> Unit,
    onSelect: (RecipeLibraryDestination) -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = state.selected
    val selectedAlreadyPresent = when (selected) {
        RecipeLibraryDestination.Saved -> state.isInSaved
        is RecipeLibraryDestination.Collection ->
            selected.collectionId in state.collectionIdsContainingRecipe
        null -> false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Save to list",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
            )
            if (state.recipeTitle.isNotBlank()) {
                Text(
                    text = state.recipeTitle,
                    fontSize = 14.sp,
                    color = CKOnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)

            SaveDestinationRow(
                title = "Guardados",
                subtitle = if (state.isInSaved) "Already in your saved recipes" else "Your saved recipes library",
                icon = Icons.Default.Bookmark,
                selected = selected is RecipeLibraryDestination.Saved,
                alreadyPresent = state.isInSaved,
                onClick = { onSelect(RecipeLibraryDestination.Saved) },
            )

            if (state.collections.isNotEmpty()) {
                Text(
                    text = "Your lists",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CKOnSurfaceVariant,
                    letterSpacing = 0.6.sp,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.collections, key = { it.id }) { collection ->
                        val destination = RecipeLibraryDestination.Collection(collection.id)
                        SaveDestinationRow(
                            title = collection.name,
                            subtitle = "${collection.recipes.size} recipes",
                            icon = Icons.Default.CollectionsBookmark,
                            selected = selected == destination,
                            alreadyPresent = collection.id in state.collectionIdsContainingRecipe,
                            onClick = { onSelect(destination) },
                        )
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = selected != null && !state.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = CKSurfaceVariant,
                    disabledContentColor = CKOutlineVariant,
                ),
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (selectedAlreadyPresent) "Remove from list" else "Add to list",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveDestinationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    alreadyPresent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CKSurfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CKSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = CKOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (alreadyPresent) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = CKOutlineVariant,
            ),
        )
    }
}

@Composable
fun SaveRecipeToListHost(
    pickerState: SaveRecipePickerUiState,
    onDismiss: () -> Unit,
    onSelect: (RecipeLibraryDestination) -> Unit,
    onConfirm: () -> Unit,
    onConsumeMessage: () -> Unit,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(pickerState.resultMessage) {
        pickerState.resultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (pickerState.visible) {
        SaveRecipeToListSheet(
            state = pickerState,
            onDismiss = onDismiss,
            onSelect = onSelect,
            onConfirm = onConfirm,
        )
    }
}
