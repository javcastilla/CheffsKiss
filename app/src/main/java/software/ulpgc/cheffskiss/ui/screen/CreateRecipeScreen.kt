package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.theme.*

// 🔌 Nuevas importaciones necesarias para conectar con tu ViewModel y Dominio
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.RecipeUiState
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

// ── Data classes locales ──────────────────────────────────────────────────────

data class IngredientRow(
    val id: Int,
    val name: String = "",
    val amount: String = "",
    val unit: String = "UNIT"
)

data class StepRow(
    val id: Int,
    val description: String = "",
    val duration: String = ""
)

val unitOptions = listOf("UNIT", "GRAM", "KG", "ML", "LITRE",
    "CUP", "TBSP", "TSP", "SLICE", "PINCH")

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun CreateRecipeScreen(
    viewModel: RecipeViewModel = viewModel(),
    authViewModel: AuthenticantionViewModel = viewModel(),
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    onSaveDraft: () -> Unit
) {
    // 🔌 3. Observamos lo que nos dice Firebase (Cargando, Éxito, Error)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var title          by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }
    var servings       by remember { mutableIntStateOf(4) }
    var hours          by remember { mutableStateOf("") }
    var minutes        by remember { mutableStateOf("") }
    var tagInput       by remember { mutableStateOf("") }
    val tags           = remember { mutableStateListOf("Vegetarian", "Dessert") }
    val ingredients    = remember { mutableStateListOf(IngredientRow(0), IngredientRow(1)) }
    val steps          = remember { mutableStateListOf(StepRow(0)) }
    var nextIngredId   by remember { mutableIntStateOf(2) }
    var nextStepId     by remember { mutableIntStateOf(1) }

    // 🔌 4. Si Firebase dice "Éxito", reseteamos el estado y volvemos al Home
    LaunchedEffect(uiState) {
        if (uiState is RecipeUiState.Success) {
            viewModel.resetState()
            onPublishSuccess()
        }
    }

    // 🔌 5. Función que empaqueta todo y se lo manda al ViewModel
    val handlePublish = {
        val authorId = authViewModel.getCurrentUser()

        if (authorId == null) {
            // Opcional: mostrar error si no hay sesión
        } else {
            val mappedIngredients = ingredients.map { "${it.amount} ${it.unit} ${it.name}" }
            val mappedSteps = steps.mapIndexed { index, stepRow ->
                Step(
                    id = UUID.randomUUID(),
                    description = stepRow.description,
                    duration = (stepRow.duration.toLongOrNull() ?: 0L).minutes,
                    cardinal = index + 1
                )
            }
            viewModel.createRecipe(
                authorId = authorId, // ← ya no es random
                title = title,
                description = description,
                hours = hours,
                minutes = minutes,
                ingredients = mappedIngredients,
                steps = mappedSteps,
                tags = tags.toList(),
                image = ""
            )
        }
    }

    Scaffold(
        topBar = { CreateRecipeTopBar(onBack, onSaveDraft) },
        // 🔌 6. Pasamos nuestra nueva función handlePublish al botón
        bottomBar = { CreateRecipeBottomBar(onSaveDraft, onPublish = handlePublish) },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            CoverPhotoPlaceholder()

            // ── Basic Info ────────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.Info, title = "Basic Info")
            BasicInfoSection(
                title = title, onTitleChange = { title = it },
                description = description, onDescriptionChange = { description = it },
                servings = servings,
                onServingsInc = { servings++ },
                onServingsDec = { if (servings > 1) servings-- },
                hours = hours, onHoursChange = { hours = it },
                minutes = minutes, onMinutesChange = { minutes = it },
                tags = tags, tagInput = tagInput,
                onTagInputChange = { tagInput = it },
                onTagAdd = {
                    if (tagInput.isNotBlank()) {
                        tags.add(tagInput.trim())
                        tagInput = ""
                    }
                },
                onTagRemove = { tags.remove(it) }
            )

            // ── Ingredients ───────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.Restaurant, title = "Ingredients")
            IngredientsSection(
                ingredients = ingredients,
                onIngredientChange = { id, updated ->
                    val idx = ingredients.indexOfFirst { it.id == id }
                    if (idx != -1) ingredients[idx] = updated
                },
                onIngredientRemove = { id -> ingredients.removeAll { it.id == id } },
                onAddIngredient = {
                    ingredients.add(IngredientRow(nextIngredId++))
                }
            )

            // ── Steps ─────────────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.FormatListNumbered, title = "Steps")
            StepsSection(
                steps = steps,
                onStepChange = { id, updated ->
                    val idx = steps.indexOfFirst { it.id == id }
                    if (idx != -1) steps[idx] = updated
                },
                onAddStep = { steps.add(StepRow(nextStepId++)) }
            )
        }
    }
}

// ── TopAppBar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRecipeTopBar(onBack: () -> Unit, onSaveDraft: () -> Unit) {
    TopAppBar(
        title = {
            Text("New Recipe", fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = Primary)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Primary)
            }
        },
        actions = {
            TextButton(onClick = onSaveDraft) {
                Text("Save Draft", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
private fun CreateRecipeBottomBar(onSaveDraft: () -> Unit, onPublish: () -> Unit) {
    Surface(
        color = Surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = outlinedButtonColors(
                    containerColor = CKSurface,
                    contentColor = Primary
                ),
                border = null
            ) {
                Text("Save Draft", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPublish,
                modifier = Modifier.weight(2f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Primary, Color(0xFF14532D)),
                                start = Offset.Zero,
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Publish, null, tint = OnPrimary)
                        Text("Publish Recipe", color = OnPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun CoverPhotoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, null,
                    tint = Primary, modifier = Modifier.size(36.dp))
            }
            Text("Add Cover Photo", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Upload high-quality food photography",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(24.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnSurface)
    }
}

// ── Basic Info ────────────────────────────────────────────────────────────────

@Composable
private fun BasicInfoSection(
    title: String, onTitleChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    servings: Int, onServingsInc: () -> Unit, onServingsDec: () -> Unit,
    hours: String, onHoursChange: (String) -> Unit,
    minutes: String, onMinutesChange: (String) -> Unit,
    tags: List<String>, tagInput: String,
    onTagInputChange: (String) -> Unit,
    onTagAdd: () -> Unit,
    onTagRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Title
        RecipeField(label = "Title") {
            TextField(
                value = title, onValueChange = onTitleChange,
                placeholder = { Text("e.g. Grandma's Spiced Apple Cake",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = transparentFieldColors(),
                singleLine = true
            )
        }

        // Description
        RecipeField(label = "Description (Optional)") {
            TextField(
                value = description, onValueChange = onDescriptionChange,
                placeholder = { Text("Tell the story behind this dish...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = transparentFieldColors(),
                minLines = 3, maxLines = 5
            )
        }

        // Servings + Duration
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Servings
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Servings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onServingsDec) {
                        Icon(Icons.Default.Remove, null, tint = Primary)
                    }
                    Text(servings.toString(), fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = OnSurface)
                    IconButton(onClick = onServingsInc) {
                        Icon(Icons.Default.Add, null, tint = Primary)
                    }
                }
            }

            // Duration
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Total Duration", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    // ── Horas ──
                    androidx.compose.foundation.text.BasicTextField(
                        value = hours,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) onHoursChange(it) },
                        modifier = Modifier.width(28.dp),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (hours.isEmpty()) {
                                    Text(
                                        "0", textAlign = TextAlign.Center,
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Text(
                        "h", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).height(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    // ── Minutos ──
                    androidx.compose.foundation.text.BasicTextField(
                        value = minutes,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) onMinutesChange(it) },
                        modifier = Modifier.width(28.dp),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (minutes.isEmpty()) {
                                    Text(
                                        "45", textAlign = TextAlign.Center,
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Text(
                        "m", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Tags
        RecipeField(label = "Tags") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.forEach { tag ->
                    TagChip(tag = tag, onRemove = { onTagRemove(tag) })
                }
                TextField(
                    value = tagInput, onValueChange = onTagInputChange,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp),
                    colors = transparentFieldColors(),
                    placeholder = { Text("Add tag...", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onTagAdd() }
                    )
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background( CKSecondary.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        Icon(Icons.Default.Close, null,
            modifier = Modifier.size(14.dp).clickable { onRemove() },
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Ingredients ───────────────────────────────────────────────────────────────

@Composable
private fun IngredientsSection(
    ingredients: List<IngredientRow>,
    onIngredientChange: (Int, IngredientRow) -> Unit,
    onIngredientRemove: (Int) -> Unit,
    onAddIngredient: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ingredients.forEach { ingredient ->
            IngredientRowItem(
                ingredient = ingredient,
                onChange = { onIngredientChange(ingredient.id, it) },
                onRemove = { onIngredientRemove(ingredient.id) }
            )
        }

        DashedAddButton(label = "Add Ingredient", onClick = onAddIngredient)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientRowItem(
    ingredient: IngredientRow,
    onChange: (IngredientRow) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Name
        TextField(
            value = ingredient.name,
            onValueChange = { onChange(ingredient.copy(name = it)) },
            modifier = Modifier.weight(2f).height(50.dp),
            placeholder = { Text("Ingredient", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            colors = containerFieldColors(),
            singleLine = true
        )

        // Amount
        TextField(
            value = ingredient.amount,
            onValueChange = { onChange(ingredient.copy(amount = it)) },
            modifier = Modifier.width(64.dp).height(50.dp),
            placeholder = { Text("Amt", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center) },
            colors = containerFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Unit dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(88.dp)
        ) {
            TextField(
                value = ingredient.unit,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().height(50.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Surface)
            ) {
                unitOptions.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit) },
                        onClick = {
                            onChange(ingredient.copy(unit = unit))
                            expanded = false
                        }
                    )
                }
            }
        }

        // Delete button
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFBA1A1A))
        }
    }
}

// ── Steps ─────────────────────────────────────────────────────────────────────

@Composable
private fun StepsSection(
    steps: List<StepRow>,
    onStepChange: (Int, StepRow) -> Unit,
    onAddStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEachIndexed { index, step ->
            StepRowItem(
                index = index + 1,
                step = step,
                onChange = { onStepChange(step.id, it) }
            )
        }
        DashedAddButton(label = "Add Step", onClick = onAddStep)
    }
}

@Composable
private fun StepRowItem(index: Int, step: StepRow, onChange: (StepRow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = OnPrimary,
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Column(modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = step.description,
                onValueChange = { onChange(step.copy(description = it)) },
                placeholder = { Text("Describe this step...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = containerFieldColors(),
                minLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Timer, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                TextField(
                    value = step.duration,
                    onValueChange = { onChange(step.copy(duration = it)) },
                    placeholder = { Text("Duration (optional)", fontSize = 12.sp) },
                    modifier = Modifier.width(140.dp).height(40.dp),
                    colors = transparentFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

// ── Components Reutilizables ──────────────────────────────────────────────────

@Composable
private fun RecipeField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun DashedAddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = outlinedButtonColors(contentColor = Primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

// ── Colors ────────────────────────────────────────────────────────────────────

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface
)

@Composable
private fun containerFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface
)