package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import software.ulpgc.cheffskiss.application.services.IngredientDraft
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import software.ulpgc.cheffskiss.ui.RecipeUiState
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.theme.*
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

@Composable
fun CreateRecipeScreen(
    viewModel: RecipeViewModel = viewModel(),
    authViewModel: AuthenticantionViewModel = viewModel(),
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    onSaveDraft: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(0) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    val ingredients = remember { mutableStateListOf<IngredientRow>() }
    val steps = remember { mutableStateListOf<StepRow>() }
    var nextIngredId by remember { mutableIntStateOf(0) }
    var nextStepId by remember { mutableIntStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverImageUri = it }
    }

    val isRecipeFormComplete =
        title.isNotBlank() &&
                servings >= 1 &&
                !((hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0")) &&
                ingredients.any { it.name.isNotBlank() } &&
                steps.any { it.description.isNotBlank() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is RecipeUiState.Success -> { viewModel.resetState(); onPublishSuccess() }
            is RecipeUiState.Error   -> { snackbarHostState.showSnackbar(state.message); viewModel.resetState() }
            else -> Unit
        }
    }

    val handlePublish = {
        val authorId = authViewModel.getCurrentUid()
        if (authorId != null) {
            val ingredientDrafts = ingredients
                .filter { it.name.isNotBlank() }
                .map { IngredientDraft(name = it.name, amount = it.amount, unit = it.unit) }
            val mappedSteps = steps
                .filter { it.description.isNotBlank() }
                .mapIndexed { index, stepRow ->
                    Step(
                        id          = UUID.randomUUID(),
                        description = stepRow.description.trim(),
                        duration    = (stepRow.duration.toLongOrNull() ?: 0L).minutes,
                        cardinal    = index + 1
                    )
                }
            viewModel.createRecipe(
                authorId         = authorId,
                title            = title,
                description      = description.trim(),
                servings         = servings,
                hours            = hours,
                minutes          = minutes,
                ingredientDrafts = ingredientDrafts,
                steps            = mappedSteps,
                stepImageUris    = steps.map { it.imageUri },
                tags             = tags.toList(),
                imageUri         = coverImageUri
            )
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar         = { CRTopBar(onBack = onBack, onSaveDraft = onSaveDraft) },
        bottomBar      = {
            CRBottomBar(
                onSaveDraft           = onSaveDraft,
                onPublish             = handlePublish,
                isLoading             = uiState is RecipeUiState.Loading,
                isPublishFormComplete = isRecipeFormComplete
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
            CoverPhotoCard(imageUri = coverImageUri, onClick = { galleryLauncher.launch("image/*") })

            CRCard(icon = Icons.Default.Info, title = "Basic Details") {
                CRFieldWithIcon(icon = Icons.Default.RestaurantMenu, label = "Recipe Title") {
                    CRTextField(
                        value         = title,
                        onValueChange = { title = it },
                        placeholder   = "e.g. Grandma's Apple Pie",
                        singleLine    = true,
                        leadingIcon   = { Icon(Icons.Default.RestaurantMenu, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) }
                    )
                }
                CRFieldWithIcon(icon = Icons.Default.Description, label = "Description") {
                    CRTextField(
                        value         = description,
                        onValueChange = { description = it },
                        placeholder   = "Share a little story about this recipe...",
                        minLines      = 3,
                        maxLines      = 5,
                        leadingIcon   = { Icon(Icons.Default.Description, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Servings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Surface, CircleShape)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            SmallCircleButton(icon = Icons.Default.Remove) { if (servings > 0) servings-- }
                            Text(
                                text       = if (servings > 0) "$servings" else "—",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (servings > 0) OnSurface else CKOutlineVariant
                            )
                            SmallCircleButton(icon = Icons.Default.Add) { servings++ }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CRTextField(
                                value         = hours,
                                onValueChange = { hours = it },
                                placeholder   = "0h",
                                singleLine    = true,
                                keyboardType  = KeyboardType.Number,
                                modifier      = Modifier.weight(1f)
                            )
                            CRTextField(
                                value         = minutes,
                                onValueChange = { minutes = it },
                                placeholder   = "0m",
                                singleLine    = true,
                                keyboardType  = KeyboardType.Number,
                                modifier      = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            CRCard(icon = Icons.Default.Sell, title = "Tags") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier              = Modifier.fillMaxWidth().animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag -> CRTagChip(tag = tag, onRemove = { tags.remove(tag) }) }
                    Row(
                        modifier = Modifier
                            .background(CKSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                        BasicTextField(
                            value           = tagInput,
                            onValueChange   = { tagInput = it },
                            singleLine      = true,
                            textStyle       = androidx.compose.ui.text.TextStyle(
                                fontSize   = 12.sp,
                                color      = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { if (tagInput.isNotBlank()) { tags.add(tagInput.trim()); tagInput = "" } }
                            ),
                            modifier        = Modifier.widthIn(min = 60.dp, max = 120.dp),
                            decorationBox   = { inner ->
                                Box {
                                    if (tagInput.isEmpty()) Text("Add tag...", fontSize = 12.sp, color = CKOutlineVariant, fontWeight = FontWeight.SemiBold)
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            CRCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
                ingredients.forEach { ingredient ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DragIndicator, null, tint = CKOutlineVariant, modifier = Modifier.size(20.dp))
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .background(Background, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value         = ingredient.amount,
                                onValueChange = {
                                    val idx = ingredients.indexOf(ingredient)
                                    if (idx >= 0) ingredients[idx] = ingredient.copy(amount = it)
                                },
                                singleLine    = true,
                                textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                                modifier      = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box {
                                        if (ingredient.amount.isEmpty()) Text("Amt", fontSize = 13.sp, color = CKOutlineVariant)
                                        inner()
                                    }
                                }
                            )
                        }
                        IngredientNameField(
                            name         = ingredient.name,
                            onNameChange = { name ->
                                val idx = ingredients.indexOf(ingredient)
                                if (idx >= 0) ingredients[idx] = ingredient.copy(name = name)
                            },
                            viewModel    = viewModel,
                            modifier     = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick  = { ingredients.removeAll { it.id == ingredient.id } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.RemoveCircleOutline, null, tint = CKOutlineVariant)
                        }
                    }
                }
                DashedAddButton(label = "Add Ingredient") { ingredients.add(IngredientRow(nextIngredId++)) }
            }

            CRCard(icon = Icons.Default.FormatListNumbered, title = "Instructions") {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    steps.forEachIndexed { index, step ->
                        CRStepItem(
                            number   = index + 1,
                            isFirst  = index == 0,
                            step     = step,
                            onChange = { updated ->
                                val idx = steps.indexOfFirst { it.id == step.id }
                                if (idx != -1) steps[idx] = updated
                            },
                            onRemove = { steps.removeAll { it.id == step.id } }
                        )
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${steps.size + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CKOutlineVariant)
                        }
                        OutlinedButton(
                            onClick  = { steps.add(StepRow(nextStepId++)) },
                            modifier = Modifier.weight(1f).height(64.dp),
                            shape    = RoundedCornerShape(16.dp),
                            border   = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                            colors   = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Autocomplete de ingredientes (privado — depende de RecipeViewModel) ────────
@Composable
private fun IngredientNameField(
    name: String,
    onNameChange: (String) -> Unit,
    viewModel: RecipeViewModel,
    modifier: Modifier = Modifier
) {
    var suggestions by remember { mutableStateOf<List<Ingredient>>(emptyList()) }

    LaunchedEffect(name) {
        if (name.trim().length < 2) {
            suggestions = emptyList()
        } else {
            delay(250)
            suggestions = viewModel.searchIngredients(name)
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background, CircleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value         = name,
                onValueChange = onNameChange,
                singleLine    = true,
                textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                modifier      = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) Text("Ingredient", fontSize = 13.sp, color = CKOutlineVariant)
                        inner()
                    }
                }
            )
        }
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
            ) {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNameChange(suggestion.name); suggestions = emptyList() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(suggestion.name, fontSize = 13.sp, color = OnSurface)
                    }
                    HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}