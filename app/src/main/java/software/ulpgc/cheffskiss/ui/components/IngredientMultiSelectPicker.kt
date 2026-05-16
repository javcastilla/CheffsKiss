package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.ui.screen.displayCategory
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface
import java.util.UUID

@Composable
fun IngredientMultiSelectPicker(
    selectedIds: Set<UUID>,
    options: List<Ingredient>,
    onSelectionChange: (Set<UUID>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    val sortedOptions = remember(options) {
        options
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }
    val filteredOptions = remember(sortedOptions, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) sortedOptions
        else sortedOptions.filter { ingredient ->
            ingredient.name.lowercase().contains(query) ||
                ingredient.normalizedName.contains(query) ||
                ingredient.displayCategory.lowercase().contains(query) ||
                ingredient.aliases.any { it.lowercase().contains(query) }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            highlightedIndex = 0
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            searchQuery = ""
            highlightedIndex = 0
            keyboardController?.hide()
        }
    }

    LaunchedEffect(filteredOptions.size) {
        if (highlightedIndex >= filteredOptions.size) {
            highlightedIndex = (filteredOptions.size - 1).coerceAtLeast(0)
        }
    }

    val summaryText = when (selectedIds.size) {
        0 -> "Buscar ingrediente"
        1 -> "Un ingrediente seleccionado"
        else -> "${selectedIds.size} ingredientes seleccionados"
    }

    fun toggleIngredient(ingredient: Ingredient) {
        val next = selectedIds.toMutableSet()
        if (next.contains(ingredient.id)) next.remove(ingredient.id) else next.add(ingredient.id)
        onSelectionChange(next)
    }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            anchorWidthPx = coords.size.width
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CKOutlineVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .background(Surface)
                .then(
                    if (!expanded) {
                        Modifier.clickable(
                            enabled = enabled && !isLoading,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { expanded = true }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = CKOutlineVariant, modifier = Modifier.size(20.dp))
            Text(
                text = if (expanded && searchQuery.isNotBlank()) searchQuery else summaryText,
                fontSize = 15.sp,
                fontWeight = if (selectedIds.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (selectedIds.isEmpty() && searchQuery.isBlank()) CKOutlineVariant else OnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
            } else {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CKOutlineVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier
                        .then(
                            if (anchorWidthPx > 0) {
                                Modifier.size(
                                    width = with(density) { anchorWidthPx.toDp() },
                                    height = androidx.compose.ui.unit.Dp.Unspecified,
                                )
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        )
                        .border(1.dp, CKOutlineVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Surface,
                    shadowElevation = 6.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                highlightedIndex = 0
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            if (filteredOptions.isNotEmpty()) {
                                                highlightedIndex = (highlightedIndex + 1)
                                                    .coerceAtMost(filteredOptions.lastIndex)
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (filteredOptions.isNotEmpty()) {
                                                highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0)
                                            }
                                            true
                                        }
                                        Key.Enter -> {
                                            filteredOptions.getOrNull(highlightedIndex)?.let(::toggleIngredient)
                                            true
                                        }
                                        Key.Escape -> {
                                            expanded = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurface,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    filteredOptions.getOrNull(highlightedIndex)?.let(::toggleIngredient)
                                },
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text("Buscar ingrediente", fontSize = 15.sp, color = CKOutlineVariant)
                                    }
                                    inner()
                                }
                            },
                        )
                        HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)

                        if (filteredOptions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text("Sin resultados", fontSize = 14.sp, color = CKOnSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                            ) {
                                itemsIndexed(filteredOptions, key = { _, item -> item.id }) { index, ingredient ->
                                    val isSelected = selectedIds.contains(ingredient.id)
                                    val isHighlighted = index == highlightedIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .background(
                                                when {
                                                    isSelected -> CKSurfaceVariant.copy(alpha = 0.65f)
                                                    isHighlighted -> Background.copy(alpha = 0.7f)
                                                    else -> Surface
                                                },
                                            )
                                            .clickable { toggleIngredient(ingredient) }
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ingredient.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = OnSurface,
                                                maxLines = 1,
                                            )
                                            val subtitle = ingredient.displayCategory.takeIf { it.isNotBlank() }
                                            if (subtitle != null) {
                                                Text(
                                                    subtitle,
                                                    fontSize = 12.sp,
                                                    color = CKOnSurfaceVariant,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    if (index < filteredOptions.lastIndex) {
                                        HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
