package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.ui.screen.CRCard
import software.ulpgc.cheffskiss.ui.screen.IngredientRow
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import java.util.UUID

@Composable
fun RecipeIngredientsSection(
    ingredients: SnapshotStateList<IngredientRow>,
    ingredientCatalog: List<Ingredient>,
    isCatalogLoading: Boolean,
    allocateRowId: () -> Int,
) {
    LaunchedEffect(Unit) {
        ingredients.removeAll { row -> row.ingredientId == null }
    }

    val selectedRows = ingredients.filter { it.ingredientId != null }
    val selectedIds = selectedRows.mapNotNull { it.ingredientId }.toSet()

    CRCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
        IngredientMultiSelectPicker(
            selectedIds = selectedIds,
            options = ingredientCatalog,
            isLoading = isCatalogLoading,
            onSelectionChange = { newIds ->
                ingredients.removeAll { row ->
                    row.ingredientId != null && row.ingredientId !in newIds
                }
                newIds.forEach { id ->
                    if (ingredients.none { it.ingredientId == id }) {
                        val catalogItem = ingredientCatalog.firstOrNull { it.id == id } ?: return@forEach
                        ingredients.add(
                            IngredientRow(
                                id = allocateRowId(),
                                ingredientId = catalogItem.id,
                                name = catalogItem.name,
                            ),
                        )
                    }
                }
            },
        )

        if (selectedRows.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedRows.forEach { ingredient ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = CKOutlineVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .background(Background, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            BasicTextField(
                                value = ingredient.amount,
                                onValueChange = { value ->
                                    val idx = ingredients.indexOfFirst { it.id == ingredient.id }
                                    if (idx >= 0) ingredients[idx] = ingredient.copy(amount = value)
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Medium,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box {
                                        if (ingredient.amount.isEmpty()) {
                                            Text("Amt", fontSize = 13.sp, color = CKOutlineVariant)
                                        }
                                        inner()
                                    }
                                },
                            )
                        }
                        MeasurementUnitDropdown(
                            selected = ingredient.measurement,
                            onSelected = { measurement ->
                                val idx = ingredients.indexOfFirst { it.id == ingredient.id }
                                if (idx >= 0) ingredients[idx] = ingredient.copy(measurement = measurement)
                            },
                        )
                        Text(
                            text = ingredient.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        IconButton(
                            onClick = {
                                ingredients.removeAll { it.id == ingredient.id }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = null,
                                tint = CKOutlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
