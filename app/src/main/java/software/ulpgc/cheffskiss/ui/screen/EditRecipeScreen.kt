package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.ui.RecipeUiState
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.theme.*
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseIngredientRow(stored: String, idx: Int): IngredientRow {
    val parts = stored.trim().split(" ", limit = 3)
    return when {
        parts.size >= 3 -> IngredientRow(id = idx, amount = parts[0], unit = parts[1], name = parts[2])
        parts.size == 2 -> IngredientRow(id = idx, amount = parts[0], name = parts[1])
        else -> IngredientRow(id = idx, name = stored)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel = viewModel(),
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf(recipe.title) }
    var description by remember { mutableStateOf(recipe.description) }
    var servings by remember { mutableIntStateOf(recipe.servings) }

    val totalMinutes = recipe.duration.inWholeMinutes
    var hours by remember { mutableStateOf(if (totalMinutes >= 60) (totalMinutes / 60).toString() else "") }
    var minutes by remember { mutableStateOf((totalMinutes % 60).let { if (it > 0) it.toString() else "" }) }

    var tagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>().also { it.addAll(recipe.tags) } }

    val ingredients = remember {
        mutableStateListOf<IngredientRow>().also { list ->
            recipe.ingredients.forEachIndexed { idx, it -> list.add(parseIngredientRow(it, idx)) }
            if (list.isEmpty()) list.add(IngredientRow(0))
        }
    }
    val steps = remember {
        mutableStateListOf<StepRow>().also { list ->
            recipe.steps.sortedBy { it.cardinal }.forEachIndexed { idx, step ->
                list.add(
                    StepRow(
                        id = idx,
                        description = step.description,
                        duration = step.duration.inWholeMinutes.let { if (it > 0) it.toString() else "" },
                        existingImageUrl = step.image.takeIf { it.isNotBlank() }
                    )
                )
            }
            if (list.isEmpty()) list.add(StepRow(0))
        }
    }
    var nextIngredId by remember { mutableIntStateOf(ingredients.size) }
    var nextStepId by remember { mutableIntStateOf(steps.size) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverImageUri = it }
    }

    LaunchedEffect(uiState) {
        if (uiState is RecipeUiState.Success) {
            viewModel.resetState()
            onUpdateSuccess()
        }
    }

    val handleUpdate = {
        val mappedIngredients = ingredients.map { "${it.amount} ${it.unit} ${it.name}" }
        val mappedSteps = steps.mapIndexed { index, stepRow ->
            Step(
                id          = UUID.randomUUID(),
                description = stepRow.description,
                duration    = (stepRow.duration.toLongOrNull() ?: 0L).minutes,
                cardinal    = index + 1,
                image       = stepRow.existingImageUrl ?: ""  // will be replaced by upload if imageUri != null
            )
        }
        val stepImageUris = steps.map { it.imageUri }
        viewModel.updateRecipe(
            recipeId         = recipe.id,
            authorId         = recipe.author,
            title            = title,
            description      = description,
            servings         = servings,
            hours            = hours,
            minutes          = minutes,
            ingredients      = mappedIngredients,
            steps            = mappedSteps,
            stepImageUris    = stepImageUris,
            tags             = tags.toList(),
            imageUri         = coverImageUri,
            existingImageUrl = recipe.image,
            createdAt        = recipe.createdAt
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is RecipeUiState.Error) {
            snackbarHostState.showSnackbar((uiState as RecipeUiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar = { CRTopBar(title = "Edit Recipe", onBack = onBack) },
        bottomBar = {
            CRBottomBar(
                onSaveDraft  = onBack,
                onPublish    = handleUpdate,
                isLoading    = uiState is RecipeUiState.Loading,
                isPublishFormComplete = true,
                publishLabel = "Update Recipe"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cover Photo ──────────────────────────────────────────────────
            CoverPhotoCard(
                imageUri    = coverImageUri,
                existingUrl = recipe.image.takeIf { it.isNotBlank() },
                onClick     = { galleryLauncher.launch("image/*") }
            )

            // ── Basic Details ────────────────────────────────────────────────
            CRCard(icon = Icons.Default.Info, title = "Basic Details") {
                CRFieldWithIcon(icon = Icons.Default.RestaurantMenu, label = "Recipe Title") {
                    CRTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = "e.g. Grandma's Apple Pie", singleLine = true,
                        leadingIcon = { Icon(Icons.Default.RestaurantMenu, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) }
                    )
                }
                CRFieldWithIcon(icon = Icons.Default.Description, label = "Description") {
                    CRTextField(
                        value = description, onValueChange = { description = it },
                        placeholder = "Share a little story about this recipe...",
                        minLines = 3, maxLines = 5,
                        leadingIcon = { Icon(Icons.Default.Description, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Servings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                                .background(Surface, CircleShape).padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallCircleButton(icon = Icons.Default.Remove) { if (servings > 1) servings-- }
                            Text("$servings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                            SmallCircleButton(icon = Icons.Default.Add) { servings++ }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CRTextField(value = hours, onValueChange = { hours = it }, placeholder = "0h", singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            CRTextField(value = minutes, onValueChange = { minutes = it }, placeholder = "0m", singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Tags ─────────────────────────────────────────────────────────
            CRCard(icon = Icons.Default.Sell, title = "Tags") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag -> CRTagChip(tag = tag, onRemove = { tags.remove(tag) }) }
                    Row(
                        modifier = Modifier.background(CKSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                        BasicTextField(
                            value = tagInput, onValueChange = { tagInput = it }, singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = OnSurface, fontWeight = FontWeight.SemiBold),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                if (tagInput.isNotBlank()) { tags.add(tagInput.trim()); tagInput = "" }
                            }),
                            modifier = Modifier.widthIn(min = 60.dp, max = 120.dp),
                            decorationBox = { inner ->
                                Box {
                                    inner()
                                    if (tagInput.isEmpty()) Text("Add tag...", fontSize = 12.sp, color = CKOutlineVariant, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }
                }
            }

            // ── Ingredients ──────────────────────────────────────────────────
            CRCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
                ingredients.forEach { ingredient ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DragIndicator, null, tint = CKOutlineVariant, modifier = Modifier.size(20.dp))
                        Box(modifier = Modifier.width(72.dp).background(Background, CircleShape).padding(horizontal = 12.dp, vertical = 12.dp)) {
                            BasicTextField(
                                value = ingredient.amount,
                                onValueChange = { ingredients[ingredients.indexOf(ingredient)] = ingredient.copy(amount = it) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner -> if (ingredient.amount.isEmpty()) Text("Amt", fontSize = 13.sp, color = CKOutlineVariant) else inner() }
                            )
                        }
                        Box(modifier = Modifier.weight(1f).background(Background, CircleShape).padding(horizontal = 14.dp, vertical = 12.dp)) {
                            BasicTextField(
                                value = ingredient.name,
                                onValueChange = { ingredients[ingredients.indexOf(ingredient)] = ingredient.copy(name = it) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner -> if (ingredient.name.isEmpty()) Text("Ingredient", fontSize = 13.sp, color = CKOutlineVariant) else inner() }
                            )
                        }
                        IconButton(onClick = { ingredients.removeAll { it.id == ingredient.id } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RemoveCircleOutline, null, tint = CKOutlineVariant)
                        }
                    }
                }
                DashedAddButton(label = "Add Ingredient") { ingredients.add(IngredientRow(nextIngredId++)) }
            }

            // ── Instructions ─────────────────────────────────────────────────
            CRCard(icon = Icons.Default.FormatListNumbered, title = "Instructions") {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    steps.forEachIndexed { index, step ->
                        CRStepItem(
                            number  = index + 1,
                            isFirst = index == 0,
                            step    = step,
                            onChange = { updated ->
                                val idx = steps.indexOfFirst { it.id == step.id }
                                if (idx != -1) steps[idx] = updated
                            },
                            onRemove = { steps.removeAll { it.id == step.id } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("${steps.size + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CKOutlineVariant) }
                        OutlinedButton(
                            onClick = { steps.add(StepRow(nextStepId++)) },
                            modifier = Modifier.weight(1f).height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                            colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
                        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp)) }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
