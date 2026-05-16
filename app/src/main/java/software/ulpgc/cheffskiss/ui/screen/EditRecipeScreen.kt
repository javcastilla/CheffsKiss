package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.application.services.IngredientDraft
import software.ulpgc.cheffskiss.ui.components.IngredientPickerDropdown
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.ui.theme.*
import kotlin.time.toDuration

// ── Data models ───────────────────────────────────────────────────────────────

data class IngredientRow(
    val id: Int,
    val ingredientId: java.util.UUID? = null,
    val name: String = "",
    val amount: String = "",
    val unit: String = "UNIT",
)

data class StepRow(
    val id: Int,
    val description: String = "",
    val duration: String = "",
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null
)

val unitOptions = listOf("UNIT", "GRAM", "KG", "ML", "LITRE", "CUP", "TBSP", "TSP", "SLICE", "PINCH")

// ── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CRTopBar(
    title: String = "New Recipe",
    onBack: () -> Unit,
    onSaveDraft: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier.size(36.dp).background(Surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        },
        actions = { IconButton(onClick = onSaveDraft) {} },
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier.height(52.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                colors = outlinedButtonColors(contentColor = CKOnSurfaceVariant, containerColor = Color.Transparent)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Draft", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Button(
                onClick = onPublish,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = CircleShape,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPublishFormComplete) Primary else Primary.copy(alpha = 0.4f),
                    contentColor = OnPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Publish, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(publishLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Cover Photo Card ──────────────────────────────────────────────────────────

@Composable
internal fun CoverPhotoCard(
    imageUri: Uri?,
    existingUrl: String? = null,
    onClick: () -> Unit
) {
    val showExisting = imageUri == null && !existingUrl.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(CKSurfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri != null -> AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            showExisting -> AsyncImage(
                model = existingUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(CKSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Primary, modifier = Modifier.size(26.dp))
                }
                Text("Add Recipe Photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("High quality images perform better", fontSize = 12.sp, color = CKOnSurfaceVariant)
            }
        }
        if (imageUri != null || showExisting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Background.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = OnSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────

@Composable
internal fun CRCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
        content()
    }
}

// ── Text Field ────────────────────────────────────────────────────────────────

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
    leadingIcon: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        shape = if (singleLine) CircleShape else RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Background,
            unfocusedContainerColor = Background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface
        )
    )
}

// ── Small Circle Button ───────────────────────────────────────────────────────

@Composable
internal fun SmallCircleButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).background(Background, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = OnSurface, modifier = Modifier.size(16.dp))
    }
}

// ── Tag Chip ──────────────────────────────────────────────────────────────────

@Composable
internal fun CRTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(Primary, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
        Icon(
            Icons.Default.Close,
            null,
            tint = OnPrimary.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove)
        )
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
internal fun CRStepItem(
    number: Int,
    isFirst: Boolean,
    step: StepRow,
    onChange: (StepRow) -> Unit,
    onRemove: () -> Unit
) {
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onChange(step.copy(imageUri = it)) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (isFirst) Primary else Background, CircleShape)
                .border(if (isFirst) 0.dp else 2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFirst) OnPrimary else CKOutlineVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f).background(Background, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = step.description,
                onValueChange = { onChange(step.copy(description = it)) },
                placeholder = { Text("Describe this step...", fontSize = 13.sp, color = CKOutlineVariant) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface
                )
            )

            val displayImage: Any? = step.imageUri ?: step.existingImageUrl?.takeIf { it.isNotBlank() }
            if (displayImage != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(Background.copy(alpha = 0.8f), CircleShape)
                            .clickable { onChange(step.copy(imageUri = null, existingImageUrl = null)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.3f)),
                    colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add photo", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    BasicTextField(
                        value = step.duration,
                        onValueChange = { onChange(step.copy(duration = it)) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = OnSurface),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(48.dp),
                        decorationBox = { inner ->
                            Box {
                                if (step.duration.isEmpty()) Text("0", fontSize = 12.sp, color = CKOutlineVariant)
                                inner()
                            }
                        }
                    )
                    Text("min", fontSize = 12.sp, color = CKOutlineVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.RemoveCircleOutline, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
// ── EditRecipeScreen ──────────────────────────────────────────────────────────
// Añadir al final de EditRecipeScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    recipe: software.ulpgc.cheffskiss.domain.model.recipe.Recipe,
    initialLines: List<software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine>,
    initialSteps: List<Step>,
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    val viewModel: software.ulpgc.cheffskiss.ui.RecipeViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val ingredientCatalog by viewModel.ingredientCatalogState.collectAsStateWithLifecycle()
    val ingredientCatalogLoading by viewModel.ingredientCatalogLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadIngredientCatalog() }

    // ── Estado local pre-poblado con los datos existentes ─────────────────
    var coverImageUri   by remember { mutableStateOf<Uri?>(null) }
    var title           by remember { mutableStateOf(recipe.title) }
    var description     by remember { mutableStateOf(recipe.description) }
    var servings        by remember { mutableIntStateOf(recipe.servings) }
    var durationHours   by remember { mutableStateOf((recipe.duration.inWholeMinutes / 60).toString()) }
    var durationMinutes by remember { mutableStateOf((recipe.duration.inWholeMinutes % 60).toString()) }
    val tags            = remember { mutableStateListOf<String>().also { it.addAll(recipe.tags) } }
    var tagInput        by remember { mutableStateOf("") }

    val ingredients = remember {
        mutableStateListOf<IngredientRow>().also { list ->
            initialLines.forEachIndexed { i, line ->
                list.add(
                    IngredientRow(
                        id           = i,
                        ingredientId = line.ingredient?.id,
                        name         = line.ingredient?.name ?: "",
                        amount       = line.amount.toString(),
                        unit         = line.measurement?.name ?: "UNIT",
                    )
                )
            }
        }
    }

    val steps = remember {
        mutableStateListOf<StepRow>().also { list ->
            initialSteps.sortedBy { it.cardinal }.forEachIndexed { i, step ->
                list.add(
                    StepRow(
                        id          = i,
                        description = step.description,
                        duration    = step.duration?.inWholeMinutes?.toString() ?: "0"
                    )
                )
            }
        }
    }

    var nextIngredId by remember { mutableIntStateOf(initialLines.size) }
    var nextStepId   by remember { mutableIntStateOf(initialSteps.size) }

    // Escuchar resultado del ViewModel
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is software.ulpgc.cheffskiss.ui.RecipeUiState.Success -> {
                viewModel.resetState()
                onUpdateSuccess()
            }
            is software.ulpgc.cheffskiss.ui.RecipeUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    val isFormComplete = title.isNotBlank() &&
            servings >= 1 &&
            (durationHours.isNotBlank() || durationMinutes.isNotBlank()) &&
            ingredients.any { it.ingredientId != null } &&
            steps.any { it.description.isNotBlank() }

    val handleUpdate: () -> Unit = {
        val mappedSteps = steps
            .filter { it.description.isNotBlank() }
            .mapIndexed { index, row ->
                val stepDurationMinutes = row.duration.toLongOrNull() ?: 0L
                Step(
                    id          = java.util.UUID.randomUUID(),
                    description = row.description.trim(),
                    duration    = stepDurationMinutes.toDuration(kotlin.time.DurationUnit.MINUTES),
                    cardinal    = index + 1
                )
            }
        val drafts = ingredients
            .filter { it.ingredientId != null }
            .map {
                IngredientDraft(
                    ingredientId = it.ingredientId,
                    name = it.name,
                    amount = it.amount,
                    unit = it.unit,
                )
            }

        viewModel.updateRecipe(
            recipeId        = recipe.id,
            authorId        = recipe.creator.id.toString(),
            title           = title,
            description     = description,
            servings        = servings,
            hours           = durationHours,
            minutes         = durationMinutes ,
            ingredientDrafts= drafts,
            steps           = mappedSteps,
            stepImageUris   = steps.map { it.imageUri },
            tags            = tags.toList(),
            imageUri        = coverImageUri,
            existingImageUrl= recipe.image?.toString() ?: "",
            createdAt       = recipe.timestamp ?: kotlinx.datetime.Clock.System.now(),
            currentVersion  = recipe.version
        )
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { coverImageUri = it } }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = software.ulpgc.cheffskiss.ui.theme.Background,
        topBar = {
            CRTopBar(
                title       = "Edit Recipe",
                onBack      = onBack,
                onSaveDraft = onBack
            )
        },
        bottomBar = {
            CRBottomBar(
                onSaveDraft          = onBack,
                onPublish            = handleUpdate,
                isLoading            = uiState is software.ulpgc.cheffskiss.ui.RecipeUiState.Loading,
                isPublishFormComplete= isFormComplete,
                publishLabel         = "Save Changes"
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cover photo ───────────────────────────────────────────────
            item {
                CoverPhotoCard(
                    imageUri    = coverImageUri,
                    existingUrl = recipe.image?.toString(),
                    onClick     = { imageLauncher.launch("image/*") }
                )
            }

            // ── Basic details ─────────────────────────────────────────────
            item {
                CRCard(icon = Icons.Default.Info, title = "Basic Details") {
                    CRFieldWithIcon(icon = Icons.Default.RestaurantMenu, label = "Recipe Title") {
                        CRTextField(value = title, onValueChange = { title = it },
                            placeholder = "e.g. Grandma's Apple Pie", singleLine = true)
                    }
                    CRFieldWithIcon(icon = Icons.Default.Description, label = "Description") {
                        CRTextField(value = description, onValueChange = { description = it },
                            placeholder = "Share a little story...", minLines = 3, maxLines = 5)
                    }
                    // Servings + Duration
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CRFieldWithIcon(
                            icon = Icons.Default.Group, label = "Servings",
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SmallCircleButton(Icons.Default.Remove) {
                                    if (servings > 1) servings--
                                }
                                Text(
                                    "$servings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = software.ulpgc.cheffskiss.ui.theme.OnSurface
                                )
                                SmallCircleButton(Icons.Default.Add) { servings++ }
                            }
                        }
                        CRFieldWithIcon(
                            icon = Icons.Default.Schedule, label = "Duration",
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CRTextField(
                                    value = durationHours   , onValueChange = { durationHours    = it },
                                    placeholder = "0h", singleLine = true,
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    fillWidth = false, modifier = Modifier.width(64.dp)
                                )
                                CRTextField(
                                    value = durationMinutes , onValueChange = { durationMinutes  = it },
                                    placeholder = "0m", singleLine = true,
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    fillWidth = false, modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                    }
                    // Tags
                    CRFieldWithIcon(icon = Icons.Default.Tag, label = "Tags") {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                CRTagChip(tag = tag, onRemove = { tags.remove(tag) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CRTextField(
                                value = tagInput, onValueChange = { tagInput = it },
                                placeholder = "Add tag...", singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            SmallCircleButton(Icons.Default.Add) {
                                val t = tagInput.trim()
                                if (t.isNotBlank() && !tags.contains(t)) tags.add(t)
                                tagInput = ""
                            }
                        }
                    }
                }
            }

            // ── Ingredients ───────────────────────────────────────────────
            item {
                CRCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
                    ingredients.toList().forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IngredientPickerDropdown(
                                selected = ingredientCatalog.firstOrNull { it.id == row.ingredientId },
                                options = ingredientCatalog,
                                isLoading = ingredientCatalogLoading,
                                onSelected = { selected ->
                                    val i = ingredients.indexOfFirst { it.id == row.id }
                                    if (i != -1) {
                                        ingredients[i] = row.copy(
                                            ingredientId = selected.id,
                                            name = selected.name,
                                        )
                                    }
                                },
                                modifier = Modifier.weight(2f),
                            )
                            CRTextField(
                                value = row.amount,
                                onValueChange = { v ->
                                    val i = ingredients.indexOfFirst { it.id == row.id }
                                    if (i != -1) ingredients[i] = row.copy(amount = v)
                                },
                                placeholder = "Qty",
                                singleLine = true,
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                fillWidth = false,
                                modifier = Modifier.width(64.dp)
                            )
                            SmallCircleButton(Icons.Default.Remove) {
                                ingredients.removeAll { it.id == row.id }
                            }
                        }
                    }
                    DashedAddButton(label = "Add Ingredient") {
                        ingredients.add(IngredientRow(id = nextIngredId++))
                    }
                }
            }

            // ── Steps ─────────────────────────────────────────────────────
            item {
                CRCard(icon = Icons.Default.FormatListNumbered, title = "Instructions") {
                    steps.toList().forEachIndexed { index, step ->
                        CRStepItem(
                            number   = index + 1,
                            isFirst  = index == 0,
                            step     = step,
                            onChange = { updated ->
                                val i = steps.indexOfFirst { it.id == step.id }
                                if (i != -1) steps[i] = updated
                            },
                            onRemove = { steps.removeAll { it.id == step.id } }
                        )
                    }
                    DashedAddButton(label = "Add Step") {
                        steps.add(StepRow(nextStepId++, "", "0"))
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// Helper para CRFieldWithIcon con modifier extra (sobrecarga)
@Composable
private fun CRFieldWithIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant)
        content()
    }
}