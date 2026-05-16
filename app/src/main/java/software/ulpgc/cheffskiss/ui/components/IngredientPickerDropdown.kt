package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerDropdown(
    selected: Ingredient?,
    options: List<Ingredient>,
    onSelected: (Ingredient) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    placeholder: String = "Select ingredient",
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(options, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) options
        else options.filter { ingredient ->
            ingredient.name.lowercase().contains(query) ||
                ingredient.normalizedName.contains(query) ||
                ingredient.category.lowercase().contains(query) ||
                ingredient.aliases.any { it.lowercase().contains(query) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled && !isLoading) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled && !isLoading,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Primary,
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = CKOutlineVariant.copy(alpha = 0.4f),
                focusedContainerColor = Background,
                unfocusedContainerColor = Background,
                disabledContainerColor = Background,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
            ),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 320.dp)
                .background(Background),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = CKOutlineVariant.copy(alpha = 0.4f),
                    focusedContainerColor = Background,
                    unfocusedContainerColor = Background,
                ),
            )

            if (filteredOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No ingredients found", color = CKOnSurfaceVariant, fontSize = 13.sp) },
                    onClick = {},
                    enabled = false,
                )
            } else {
                filteredOptions.forEach { ingredient ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    ingredient.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                )
                                val subtitle = listOf(ingredient.category, ingredient.subcategory)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, fontSize = 11.sp, color = CKOnSurfaceVariant)
                                }
                            }
                        },
                        onClick = {
                            onSelected(ingredient)
                            searchQuery = ""
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
