package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.application.services.IngredientCatalogService
import software.ulpgc.cheffskiss.application.services.IngredientSearchMode
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
    var searchMode by remember { mutableStateOf(IngredientSearchMode.DIRECT) }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    val catalogFilter = remember { IngredientCatalogService() }
    var listTopInWindow by remember { mutableFloatStateOf(0f) }
    val searchFocusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val listMaxHeight by remember(expanded, listTopInWindow, imeBottomPx, density, configuration) {
        derivedStateOf {
            if (!expanded || listTopInWindow <= 0f) {
                220.dp
            } else {
                with(density) {
                    val screenHeightPx = configuration.screenHeightDp.dp.toPx()
                    val bottomLimit = screenHeightPx - imeBottomPx - 8.dp.toPx()
                    val availablePx = (bottomLimit - listTopInWindow).coerceAtLeast(48.dp.toPx())
                    availablePx.toDp().coerceIn(48.dp, 280.dp)
                }
            }
        }
    }

    val sortedOptions = remember(options) {
        options
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }
    val filteredOptions = remember(sortedOptions, searchQuery, searchMode) {
        catalogFilter.filterCatalog(sortedOptions, searchQuery, searchMode)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            highlightedIndex = 0
            searchFocusRequester.requestFocus()
            bringIntoViewRequester.bringIntoView()
        } else {
            searchQuery = ""
            highlightedIndex = 0
            listTopInWindow = 0f
        }
    }

    LaunchedEffect(filteredOptions.size) {
        if (highlightedIndex >= filteredOptions.size) {
            highlightedIndex = (filteredOptions.size - 1).coerceAtLeast(0)
        }
    }

    val summaryText = when (selectedIds.size) {
        0 -> "Search ingredient"
        1 -> "1 ingredient selected"
        else -> "${selectedIds.size} ingredients selected"
    }

    fun toggleIngredient(ingredient: Ingredient) {
        val next = selectedIds.toMutableSet()
        if (next.contains(ingredient.id)) next.remove(ingredient.id) else next.add(ingredient.id)
        onSelectionChange(next)
    }

    fun commitSearchAndClose() {
        expanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CKOutlineVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .background(Surface)
                .clickable(
                    enabled = enabled && !isLoading,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { expanded = !expanded }
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
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CKOutlineVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CKOutlineVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .background(Surface),
            ) {
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
                                    commitSearchAndClose()
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
                    keyboardActions = KeyboardActions(onSearch = { commitSearchAndClose() }),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (searchMode == IngredientSearchMode.REVERSE) {
                                        "e.g. pechuga pollo"
                                    } else {
                                        "Type to search…"
                                    },
                                    fontSize = 15.sp,
                                    color = CKOutlineVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                IngredientSearchModeRow(
                    mode = searchMode,
                    onModeChange = {
                        searchMode = it
                        highlightedIndex = 0
                    },
                )

                if (filteredOptions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("No results", fontSize = 14.sp, color = CKOnSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                listTopInWindow = coordinates.positionInWindow().y
                            }
                            .heightIn(max = listMaxHeight),
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

@Composable
private fun IngredientSearchModeRow(
    mode: IngredientSearchMode,
    onModeChange: (IngredientSearchMode) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        IngredientSearchModeTab(
            label = "Contains",
            selected = mode == IngredientSearchMode.DIRECT,
            onClick = { onModeChange(IngredientSearchMode.DIRECT) },
            modifier = Modifier.weight(1f),
        )
        IngredientSearchModeTab(
            label = "Has",
            selected = mode == IngredientSearchMode.REVERSE,
            onClick = { onModeChange(IngredientSearchMode.REVERSE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IngredientSearchModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Primary else CKOnSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Primary else Color.Transparent),
        )
    }
}
