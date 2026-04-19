package software.ulpgc.cheffskiss.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.ui.IngredientViewModel
import software.ulpgc.cheffskiss.ui.theme.*

@Composable
fun IngredientSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onIngredientSelected: (Ingredient) -> Unit,
    placeholder: String = "Search ingredient...",
    modifier: Modifier = Modifier,
    ingredientViewModel: IngredientViewModel = viewModel()
) {
    val suggestions by ingredientViewModel.suggestions.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    val showSuggestions = isFocused && suggestions.isNotEmpty()

    LaunchedEffect(value) {
        ingredientViewModel.onQueryChange(value)
    }

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = { onValueChange(it) },
            placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                AnimatedVisibility(value.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = {
                        onValueChange("")
                        ingredientViewModel.clearSuggestions()
                    }) {
                        Icon(Icons.Default.Close, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Background,
                unfocusedContainerColor = Background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface
            )
        )

        AnimatedVisibility(showSuggestions, enter = fadeIn(), exit = fadeOut()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(suggestions) { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIngredientSelected(ingredient)
                                    onValueChange(ingredient.name)
                                    ingredientViewModel.clearSuggestions()
                                    isFocused = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ingredient.name.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ingredient.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                if (ingredient.category.isNotBlank()) {
                                    Text(ingredient.category, fontSize = 11.sp, color = CKOutlineVariant)
                                }
                            }
                            if (ingredient.tags.isNotEmpty()) {
                                Text(
                                    text = ingredient.tags.first(),
                                    fontSize = 10.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .background(Primary.copy(alpha = 0.1f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (suggestions.last() != ingredient) {
                            HorizontalDivider(
                                color = CKOutlineVariant.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
