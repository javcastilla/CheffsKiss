package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.ui.MealPlanDetailViewModel
import software.ulpgc.cheffskiss.ui.SlotFormState
import software.ulpgc.cheffskiss.ui.theme.*

private data class MealSuggestion(val name: String, val icon: ImageVector)

private val SLOT_SUGGESTIONS = listOf(
    MealSuggestion("Breakfast", Icons.Default.WbSunny),
    MealSuggestion("Lunch",     Icons.Default.LightMode),
    MealSuggestion("Dinner",    Icons.Default.Nightlight),
    MealSuggestion("Snack",     Icons.Default.LocalCafe),
    MealSuggestion("Brunch",    Icons.Default.Coffee),
    MealSuggestion("Supper",    Icons.Default.DinnerDining)
)

private fun slotIcon(name: String): ImageVector = when (name.trim().lowercase()) {
    "breakfast"           -> Icons.Default.WbSunny
    "lunch"               -> Icons.Default.LightMode
    "dinner"              -> Icons.Default.Nightlight
    "snack"               -> Icons.Default.LocalCafe
    "brunch"              -> Icons.Default.Coffee
    "supper"              -> Icons.Default.DinnerDining
    else                  -> Icons.Default.Restaurant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanDetailScreen(
    planId: String,
    viewModel: MealPlanDetailViewModel,
    onBack: () -> Unit,
    onRecipeClick: (recipeId: String) -> Unit
) {
    LaunchedEffect(planId) { viewModel.load(planId) }

    val state by viewModel.uiState.collectAsState()
    val plan = state.plan

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            plan?.name ?: "Loading…",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = OnBackground
                        )
                        if (plan != null) {
                            Text(
                                "${plan.days.values.sumOf { it.size }} slots · 7 days",
                                fontSize = 12.sp,
                                color = CKOnSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnBackground)
                    }
                },
                actions = {
                    if (plan != null) {
                        IconButton(
                            onClick = { if (!plan.isActive) viewModel.toggleActive() }
                        ) {
                            Icon(
                                imageVector = if (plan.isActive) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (plan.isActive) "Active plan" else "Activate plan",
                                tint = if (plan.isActive) CKSecondary else CKOutlineVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            if (plan != null) {
                FloatingActionButton(
                    onClick = viewModel::openAddSlot,
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add slot")
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }
        if (plan == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Plan not found", color = CKOnSurfaceVariant)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WeekStrip(
                days        = Weekday.entries,
                selectedDay = state.selectedDay,
                planDays    = plan.days,
                onDaySelect = viewModel::selectDay
            )

            val slotsForDay = plan.days[state.selectedDay] ?: emptyList()

            if (slotsForDay.isEmpty()) {
                DayEmptyState(onAdd = viewModel::openAddSlot)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(slotsForDay, key = { it.id }) { slot ->
                        SlotCard(
                            slot         = slot,
                            recipeTitle  = slot.recipeId?.let { state.recipeTitles[it.toString()] },
                            recipeId     = slot.recipeId?.toString(),
                            onEdit       = { viewModel.openEditSlot(slot) },
                            onDelete     = { viewModel.deleteSlot(slot) },
                            onRecipeClick = onRecipeClick
                        )
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (state.slotForm.isVisible) {
        SlotFormSheet(
            form               = state.slotForm,
            onNameChange       = viewModel::onSlotNameChange,
            onStartChange      = viewModel::onSlotStartTimeChange,
            onEndChange        = viewModel::onSlotEndTimeChange,
            onColorChange      = viewModel::onSlotColorChange,
            onOpenRecipePicker = viewModel::openRecipePicker,
            onClearRecipe      = { viewModel.selectRecipe(null) },
            onSave             = viewModel::saveSlot,
            onDismiss          = viewModel::closeSlotForm,
            isSaving           = state.isSaving
        )
    }

    if (state.slotForm.isRecipePickerVisible) {
        RecipePickerSheet(
            recipes       = state.availableRecipes,
            query         = state.slotForm.recipePickerQuery,
            onQueryChange = viewModel::onRecipePickerQueryChange,
            onSelect      = viewModel::selectRecipe,
            onDismiss     = viewModel::closeRecipePicker
        )
    }
}

// ── Week strip ────────────────────────────────────────────────────────────────
@Composable
private fun WeekStrip(
    days: List<Weekday>,
    selectedDay: Weekday,
    planDays: Map<Weekday, List<MealSlot>>,
    onDaySelect: (Weekday) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Surface),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        days.forEach { day ->
            val isSelected = day == selectedDay
            val hasSlots   = (planDays[day]?.size ?: 0) > 0

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isSelected) Primary else Color.Transparent)
                    .clickable { onDaySelect(day) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        day.shortName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) OnPrimary else CKOnSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !hasSlots  -> Color.Transparent
                                    isSelected -> OnPrimary.copy(alpha = 0.6f)
                                    else       -> CKSecondary
                                }
                            )
                    )
                }
            }
        }
    }
}

// ── Slot card ─────────────────────────────────────────────────────────────────
@Composable
private fun SlotCard(
    slot: MealSlot,
    recipeTitle: String?,
    recipeId: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecipeClick: (String) -> Unit
) {
    val color = SLOT_COLORS.getOrElse(slot.colorIndex) { SLOT_COLORS[0] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color bar stretches to full row height
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(topStart = 12.dp))
            )

            // Start / end times
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .widthIn(min = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    slot.startTime.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(color.copy(alpha = 0.4f))
                )
                Text(
                    slot.endTime.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = color.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Name
            Text(
                slot.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = OnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Edit button
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "Edit slot",
                    tint = CKOnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete slot",
                    tint = CKOutlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Recipe / hint row ─────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Continuation of the color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            if (recipeTitle != null && recipeId != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(color.copy(alpha = 0.08f))
                        .clickable { onRecipeClick(recipeId) }
                        .padding(start = 64.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        recipeTitle,
                        fontSize = 13.sp,
                        color = color,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = "View recipe", tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 64.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text("No recipe assigned", fontSize = 12.sp, color = CKOutlineVariant)
                }
            }
        }
    }
}

// ── Day empty state ───────────────────────────────────────────────────────────
@Composable
private fun DayEmptyState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(CKSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.RestaurantMenu, null, tint = Primary, modifier = Modifier.size(30.dp))
            }
            Text("No slots for this day", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackground)
            Text("Tap + to add meals", fontSize = 13.sp, color = CKOnSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onAdd,
                shape  = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add slot")
            }
        }
    }
}

// ── Slot form bottom sheet ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotFormSheet(
    form: SlotFormState,
    onNameChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onOpenRecipePicker: () -> Unit,
    onClearRecipe: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                if (form.editingSlotId != null) "Edit slot" else "New slot",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface
            )

            // Name + quick chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    isError = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary, cursorColor = Primary),
                    modifier = Modifier.fillMaxWidth()
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SLOT_SUGGESTIONS.forEach { suggestion ->
                        val active = form.name == suggestion.name
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) CKPrimary else CKSurfaceVariant)
                                .clickable { onNameChange(suggestion.name) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = suggestion.icon,
                                contentDescription = null,
                                tint = if (active) Color.White else CKOnSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                suggestion.name,
                                fontSize = 12.sp,
                                color = if (active) Color.White else CKOnSurfaceVariant,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Times
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.startTime,
                    onValueChange = onStartChange,
                    label = { Text("Start") },
                    placeholder = { Text("08:00") },
                    singleLine = true,
                    isError = form.timeError != null,
                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary, cursorColor = Primary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.endTime,
                    onValueChange = onEndChange,
                    label = { Text("End") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    isError = form.timeError != null,
                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary, cursorColor = Primary),
                    modifier = Modifier.weight(1f)
                )
            }
            if (form.timeError != null) {
                Text(form.timeError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // Color
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SLOT_COLORS.forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (form.colorIndex == i) 2.5.dp else 0.dp, OnSurface.copy(alpha = if (form.colorIndex == i) 0.8f else 0f), CircleShape)
                                .clickable { onColorChange(i) }
                        )
                    }
                }
            }

            // Recipe
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recipe (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                if (form.selectedRecipeTitle.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CKSurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Restaurant, null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(form.selectedRecipeTitle, fontSize = 14.sp, color = OnSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = onClearRecipe, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenRecipePicker,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Assign recipe")
                    }
                }
            }

            // Save
            Button(
                onClick  = onSave,
                enabled  = !isSaving,
                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnPrimary, strokeWidth = 2.dp)
                else Text(if (form.editingSlotId != null) "Save changes" else "Add slot", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Recipe picker ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(
    recipes: List<Recipe>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Recipe?) -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = recipes.filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Select recipe", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)

            OutlinedTextField(
                value = query, onValueChange = onQueryChange,
                placeholder = { Text("Search…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant) },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = CKSurfaceVariant, cursorColor = Primary),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )

            // "No recipe" row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(null) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CKSurfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, null, tint = CKOnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                Text("No recipe", fontSize = 14.sp, color = CKOnSurfaceVariant)
            }

            HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.heightIn(max = 380.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No recipes found", color = CKOutlineVariant, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { recipe ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(recipe) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(CKSurfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Restaurant, null, tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${recipe.duration.inWholeMinutes}m · ${recipe.servings} servings", fontSize = 11.sp, color = CKOnSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
