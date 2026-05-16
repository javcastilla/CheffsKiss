package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import software.ulpgc.cheffskiss.application.services.IngredientDraft
import software.ulpgc.cheffskiss.ui.RecipeUiState
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.theme.*
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import androidx.compose.ui.graphics.Brush

// ── Models ────────────────────────────────────────────────────────────────────
data class IngredientRow(val id: Int, val name: String = "", val amount: String = "", val unit: String = "UNIT")
data class StepRow(val id: Int, val description: String = "", val duration: String = "", val imageUri: Uri? = null, val existingImageUrl: String? = null)
val unitOptions = listOf("UNIT","GRAM","KG","ML","LITRE","CUP","TBSP","TSP","SLICE","PINCH")

// ── Screen ────────────────────────────────────────────────────────────────────
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

    val hasValidDuration = !((hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0"))
    val hasIngredients = ingredients.any { it.name.isNotBlank() }
    val hasSteps = steps.any { it.description.isNotBlank() }

    val isRecipeFormComplete =
        title.isNotBlank() &&
                servings >= 1 &&
                hasValidDuration &&
                hasIngredients &&
                hasSteps

    LaunchedEffect(uiState) {
        if (uiState is RecipeUiState.Success) {
            viewModel.resetState()
            onPublishSuccess()
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
                        id = UUID.randomUUID(),
                        description = stepRow.description.trim(),
                        duration = (stepRow.duration.toLongOrNull() ?: 0L).minutes,
                        cardinal = index + 1
                    )
                }

            val stepImageUris = steps.map { it.imageUri }
            viewModel.createRecipe(
                authorId      = authorId,
                title         = title,
                description   = description.trim(),
                servings      = servings,
                hours         = hours,
                minutes       = minutes,
                ingredientDrafts = ingredientDrafts,
                steps         = mappedSteps,
                stepImageUris = stepImageUris,
                tags          = tags.toList(),
                imageUri      = coverImageUri
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is RecipeUiState.Success -> {
                viewModel.resetState()
                onPublishSuccess()
            }
            is RecipeUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar = { CRTopBar(onBack = onBack, onSaveDraft = onSaveDraft) },

        bottomBar = {CRBottomBar(
            onSaveDraft = onSaveDraft,
            onPublish = handlePublish,
            isLoading = uiState is RecipeUiState.Loading,
            isPublishFormComplete = isRecipeFormComplete
        )}
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
            CoverPhotoCard(imageUri = coverImageUri, onClick = { galleryLauncher.launch("image/*") })

            // ── Basic Details ────────────────────────────────────────────────
            CRCard(icon = Icons.Default.Info, title = "Basic Details") {
                CRFieldWithIcon(icon = Icons.Default.RestaurantMenu, label = "Recipe Title") {
                    CRTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g. Grandma's Apple Pie",
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.RestaurantMenu, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                CRFieldWithIcon(icon = Icons.Default.Description, label = "Description") {
                    CRTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Share a little story about this recipe...",
                        minLines = 3, maxLines = 5,
                        leadingIcon = {
                            Icon(Icons.Default.Description, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Servings
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Servings",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Surface, CircleShape)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallCircleButton(icon = Icons.Default.Remove) { if (servings > 0) servings-- }
                            Text(
                                if (servings > 0) "$servings" else "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (servings > 0) OnSurface else CKOutlineVariant
                            )
                            SmallCircleButton(icon = Icons.Default.Add) { servings++ }
                        }
                    }

                    // Duration
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Duration",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CRTextField(
                                value = hours, onValueChange = { hours = it },
                                placeholder = "0h", singleLine = true,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            CRTextField(
                                value = minutes, onValueChange = { minutes = it },
                                placeholder = "0m", singleLine = true,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Tags ─────────────────────────────────────────────────────────
            CRCard(icon = Icons.Default.Sell, title = "Tags") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        CRTagChip(tag = tag, onRemove = { tags.remove(tag) })
                    }
                    Row(
                        modifier = Modifier
                            .background(CKSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                        BasicTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    if (tagInput.isNotBlank()) {
                                        tags.add(tagInput.trim())
                                        tagInput = ""
                                    }
                                }
                            ),
                            modifier = Modifier.widthIn(min = 60.dp, max = 120.dp),
                            decorationBox = { inner ->
                                Box {
                                    inner()
                                    // Placeholder encima, solo visible si está vacío
                                    if (tagInput.isEmpty()) {
                                        Text(
                                            "Add tag...",
                                            fontSize = 12.sp,
                                            color = CKOutlineVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
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

                        // Amount — ancho fijo pequeño
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .background(Background, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = ingredient.amount,
                                onValueChange = { ingredients[ingredients.indexOf(ingredient)] = ingredient.copy(amount = it) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (ingredient.amount.isEmpty())
                                        Text("Amt", fontSize = 13.sp, color = CKOutlineVariant)
                                    else inner()
                                }
                            )
                        }

                        IngredientNameField(
                            name = ingredient.name,
                            onNameChange = { name ->
                                val idx = ingredients.indexOf(ingredient)
                                if (idx >= 0) ingredients[idx] = ingredient.copy(name = name)
                            },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f),
                        )

                        IconButton(
                            onClick = { ingredients.removeAll { it.id == ingredient.id } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.RemoveCircleOutline, null, tint = CKOutlineVariant)
                        }
                    }
                }
                DashedAddButton(label = "Add Ingredient") {
                    ingredients.add(IngredientRow(nextIngredId++))
                }
            }
            // ── Instructions ─────────────────────────────────────────────────
            CRCard(icon = Icons.Default.FormatListNumbered, title = "Instructions") {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    steps.forEachIndexed { index, step ->
                        CRStepItem(
                            number = index + 1,
                            isFirst = index == 0,
                            step = step,
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
                            modifier = Modifier.size(28.dp)
                                .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${steps.size + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CKOutlineVariant)
                        }
                        OutlinedButton(
                            onClick = { steps.add(StepRow(nextStepId++)) },
                            modifier = Modifier.weight(1f).height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                            colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
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

// ── Top Bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CRTopBar(title: String = "New Recipe", onBack: () -> Unit, onSaveDraft: () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier.size(36.dp).background(Surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, null, tint = OnSurface, modifier = Modifier.size(20.dp)) }
            }
        },
        actions = {
            IconButton(onClick = {}) {}
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background.copy(alpha = 0.95f))
    )
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────
@Composable
internal fun CRBottomBar(
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    isLoading: Boolean,
    isPublishFormComplete: Boolean,
    publishLabel: String = "Publish Recipe"
) {
    Surface(
        color = Background.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = CircleShape,
                colors = outlinedButtonColors(containerColor = Surface, contentColor = Primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.5f))
            ) {
                Text("Save Draft", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(
                onClick = onPublish,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = OnPrimary
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                if (!isLoading && isPublishFormComplete) {
                                    listOf(Primary, Color(0xFF004D1C))
                                } else {
                                    listOf(
                                        Primary.copy(alpha = 0.4f),
                                        Color(0xFF004D1C).copy(alpha = 0.4f)
                                    )
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = OnPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Publish, null, modifier = Modifier.size(18.dp))
                            Text(
                                text = publishLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Cover Photo ───────────────────────────────────────────────────────────────
@Composable
internal fun CoverPhotoCard(imageUri: Uri?, existingUrl: String? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp))
            .background(Surface)
            .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri != null -> AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            !existingUrl.isNullOrBlank() -> AsyncImage(model = existingUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).background(CKSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.AddAPhoto, null, tint = Primary, modifier = Modifier.size(26.dp)) }
                Text("Add Recipe Photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("High quality images perform better", fontSize = 12.sp, color = CKOnSurfaceVariant)
            }
        }
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────
@Composable
internal fun CRCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            content()
        }
    }
}

// ── Field with Icon ───────────────────────────────────────────────────────────
@Composable
internal fun CRFieldWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
        content()
    }
}
@Composable
internal fun CRTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    fillWidth: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        shape = if (singleLine) CircleShape else RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Background, unfocusedContainerColor = Background,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface
        )
    )
}
// ── Small Circle Button ───────────────────────────────────────────────────────
@Composable
internal fun SmallCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).background(Background, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = OnSurface, modifier = Modifier.size(16.dp)) }
}

// ── Tag Chip ──────────────────────────────────────────────────────────────────
@Composable
internal fun CRTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.background(Primary, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
        Icon(Icons.Default.Close, null, tint = OnPrimary.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove))
    }
}

// ── Dashed Add Button ─────────────────────────────────────────────────────────
@Composable
internal fun DashedAddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
        colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
    ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// ── Step Item ─────────────────────────────────────────────────────────────────
@Composable
internal fun CRStepItem(number: Int, isFirst: Boolean, step: StepRow, onChange: (StepRow) -> Unit, onRemove: () -> Unit) {
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onChange(step.copy(imageUri = it)) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Number badge
        Box(
            modifier = Modifier.size(28.dp)
                .background(if (isFirst) Primary else Background, CircleShape)
                .border(if (isFirst) 0.dp else 2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (isFirst) OnPrimary else CKOutlineVariant)
        }

        // Step content card
        Column(
            modifier = Modifier.weight(1f).background(Background, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Description field
            TextField(
                value = step.description,
                onValueChange = { onChange(step.copy(description = it)) },
                placeholder = { Text("Describe this step...", fontSize = 13.sp, color = CKOutlineVariant) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent, unfocusedContainerColor   = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent, unfocusedIndicatorColor   = Color.Transparent,
                    focusedTextColor        = OnSurface,         unfocusedTextColor         = OnSurface
                )
            )

            // Step image — preview or picker button
            val displayImage: Any? = step.imageUri ?: step.existingImageUrl?.takeIf { it.isNotBlank() }
            if (displayImage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Remove button
                    IconButton(
                        onClick = { onChange(step.copy(imageUri = null, existingImageUrl = null)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                // Add photo button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface)
                        .clickable { imageLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                    Text("Add photo (optional)", fontSize = 12.sp, color = CKOutlineVariant, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = CKOutlineVariant.copy(alpha = 0.2f))

            // Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    Text("Step Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CKOutlineVariant)
                }
                Row(
                    modifier = Modifier
                        .background(Surface, CircleShape)
                        .border(1.dp, CKOutlineVariant.copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = step.duration,
                        onValueChange = { onChange(step.copy(duration = it)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = OnSurface, textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.width(36.dp),
                        decorationBox = { inner ->
                            if (step.duration.isEmpty())
                                Text("0", fontSize = 12.sp, color = CKOutlineVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            else inner()
                        }
                    )
                    Text("min", fontSize = 11.sp, color = CKOutlineVariant)
                }
            }
        }

        // Delete button
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp).padding(top = 4.dp)) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IngredientNameField(
    name: String,
    onNameChange: (String) -> Unit,
    viewModel: RecipeViewModel,
    modifier: Modifier = Modifier,
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
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    color = OnSurface,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text("Ingredient", fontSize = 13.sp, color = CKOutlineVariant)
                    } else {
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
                            .clickable {
                                onNameChange(suggestion.name)
                                suggestions = emptyList()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = CKOutlineVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(suggestion.name, fontSize = 13.sp, color = OnSurface)
                    }
                    HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}