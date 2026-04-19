package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.ui.IngredientViewModel
import software.ulpgc.cheffskiss.ui.RecipeUiState
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.components.DurationPickerDialog
import software.ulpgc.cheffskiss.ui.components.IngredientSearchField
import software.ulpgc.cheffskiss.ui.theme.*
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    onBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    onSaveDraft: () -> Unit,
    viewModel: RecipeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val authorId = currentUser?.uid ?: ""

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(2) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var tagInput by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    val tags = remember { mutableStateListOf<String>() }
    val ingredients = remember { mutableStateListOf<IngredientRow>().also { it.addIngredientRow(0) } }
    val steps = remember { mutableStateListOf<StepRow>().also { it.addStepRow(0) } }
    var nextIngredId by remember { mutableIntStateOf(1) }
    var nextStepId by remember { mutableIntStateOf(1) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverImageUri = it }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is RecipeUiState.Success -> {
                viewModel.resetState()
                onPublishSuccess()
            }
            is RecipeUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as RecipeUiState.Error).message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    val durationLabel = buildString {
        val h = hours.toIntOrNull() ?: 0
        val m = minutes.toIntOrNull() ?: 0
        if (h > 0) append("${h}h ")
        if (m > 0) append("${m}m")
        if (h == 0 && m == 0) append("Set duration")
    }.trim()

    if (showTimePicker) {
        DurationPickerDialog(
            hours = hours.toIntOrNull() ?: 0,
            minutes = minutes.toIntOrNull() ?: 0,
            onConfirm = { h, m ->
                hours = if (h > 0) h.toString() else ""
                minutes = if (m > 0) m.toString() else ""
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar = {
            CRTopBar(title = "New Recipe", onBack = onBack)
        },
        bottomBar = {
            CRBottomBar(
                onSaveDraft = onSaveDraft,
                onPublish = {
                    val mappedIngredients = ingredients
                        .filter { it.name.isNotBlank() }
                        .map {
                            val ingredientId = it.ingredientId
                                ?: throw IllegalArgumentException("Select a valid ingredient from the list")
                            "${it.amount};${it.unit};$ingredientId"
                        }

                    val mappedSteps = steps.filter { it.description.isNotBlank() }
                    val stepImageUris = mappedSteps.map { it.imageUri }

                    viewModel.createRecipe(
                        authorId = authorId,
                        title = title,
                        description = description,
                        servings = servings,
                        hours = hours,
                        minutes = minutes,
                        ingredients = mappedIngredients,
                        steps = mappedSteps.mapIndexed { index, stepRow ->
                            Step(
                                id = UUID.randomUUID(),
                                description = stepRow.description,
                                duration = (stepRow.duration.toLongOrNull() ?: 0L).minutes,
                                cardinal = index + 1,
                                image = stepRow.existingImageUrl ?: ""
                            )
                        },
                        stepImageUris = stepImageUris,
                        tags = tags.toList(),
                        imageUri = coverImageUri
                    )
                },
                isLoading = uiState is RecipeUiState.Loading
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
            // Cover Photo
            CoverPhotoCard(
                imageUri = coverImageUri,
                existingUrl = null,
                onClick = { galleryLauncher.launch("image/*") }
            )

            // Basic Details
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
                        minLines = 3,
                        maxLines = 5,
                        leadingIcon = {
                            Icon(Icons.Default.Description, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Servings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .background(Background, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallCircleButton(icon = Icons.Default.Remove) { if (servings > 1) servings-- }
                            Text(servings.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                            SmallCircleButton(icon = Icons.Default.Add) { servings++ }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Background, CircleShape)
                                .clip(CircleShape)
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                            Text(durationLabel, fontSize = 13.sp, color = if (durationLabel == "Set duration") CKOutlineVariant else OnSurface)
                        }
                    }
                }
            }

            // Tags
            CRCard(icon = Icons.Default.Label, title = "Tags") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags) { tag ->
                        CRTagChip(tag = tag, onRemove = { tags.remove(tag) })
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
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
                                if (tagInput.isEmpty()) Text("Add tag...", fontSize = 12.sp, color = CKOutlineVariant, fontWeight = FontWeight.SemiBold)
                                inner()
                            }
                        }
                    )
                }
            }

            // Ingredients
            CRCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
                ingredients.forEachIndexed { index, ingredient ->
                    val ingredientViewModel: IngredientViewModel = viewModel(key = "ingr_vm_$index")
                    var fieldValue by remember(ingredient.id) { mutableStateOf(ingredient.name) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.DragIndicator, null, tint = CKOutlineVariant, modifier = Modifier.size(20.dp))

                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .background(Background, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = ingredient.amount,
                                onValueChange = { ingredients[index] = ingredient.copy(amount = it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                                decorationBox = { inner ->
                                    if (ingredient.amount.isEmpty()) Text("Amt", fontSize = 13.sp, color = CKOutlineVariant) else inner()
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            IngredientSearchField(
                                value = fieldValue,
                                onValueChange = { v ->
                                    fieldValue = v
                                    ingredients[index] = ingredient.copy(name = v, ingredientId = null)
                                },
                                onIngredientSelected = { selected ->
                                    fieldValue = selected.name
                                    ingredients[index] = ingredient.copy(
                                        name = selected.name,
                                        ingredientId = selected.id.toString()
                                    )
                                },
                                placeholder = "Ingredient",
                                modifier = Modifier.fillMaxWidth(),
                                ingredientViewModel = ingredientViewModel
                            )
                        }

                        IconButton(
                            onClick = { ingredients.removeAll { it.id == ingredient.id } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, null, tint = CKOutlineVariant)
                        }
                    }
                }
                DashedAddButton(label = "Add Ingredient") {
                    ingredients.addIngredientRow(nextIngredId++)
                }
            }

            // Instructions
            CRCard(icon = Icons.Default.FormatListNumbered, title = "Instructions") {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    steps.forEachIndexed { index, step ->
                        CreateStepItem(
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
                            modifier = Modifier
                                .size(28.dp)
                                .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((steps.size + 1).toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CKOutlineVariant)
                        }
                        OutlinedButton(
                            onClick = { steps.addStepRow(nextStepId++) },
                            modifier = Modifier.weight(1f).height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Step item ────────────────────────────────────────────────────────────────

@Composable
private fun CreateStepItem(
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
            Text(number.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isFirst) OnPrimary else CKOutlineVariant)
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
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(model = displayImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { onChange(step.copy(imageUri = null, existingImageUrl = null)) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Box(modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp).align(Alignment.Center))
                        }
                    }
                }
            } else {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    Text("Step time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CKOutlineVariant)
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
                        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, textAlign = TextAlign.Center),
                        modifier = Modifier.width(36.dp),
                        decorationBox = { inner ->
                            if (step.duration.isEmpty()) Text("0", fontSize = 12.sp, color = CKOutlineVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) else inner()
                        }
                    )
                    Text("min", fontSize = 11.sp, color = CKOutlineVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Shared UI components ─────────────────────────────────────────────────────

@Composable
fun CoverPhotoCard(imageUri: Uri?, existingUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val model: Any? = imageUri ?: existingUrl?.takeIf { it.isNotBlank() }
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AddAPhoto, null, tint = if (model != null) Color.White else CKOutlineVariant, modifier = Modifier.size(32.dp))
            Text(
                if (model != null) "Change cover photo" else "Add cover photo",
                fontSize = 13.sp,
                color = if (model != null) Color.White else CKOutlineVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CRCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
            HorizontalDivider(color = CKOutlineVariant.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
fun CRFieldWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CRTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Background,
            unfocusedContainerColor = Background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            cursorColor = Primary
        )
    )
}

@Composable
fun SmallCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).background(Background, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun CRTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(Primary.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(tag, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.Close, null, tint = Primary, modifier = Modifier.size(12.dp).clickable(onClick = onRemove))
    }
}

@Composable
fun DashedAddButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CKOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Add, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, color = CKOutlineVariant, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CRTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackground) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = OnBackground)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@Composable
fun CRBottomBar(
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    isLoading: Boolean,
    publishLabel: String = "Publish Recipe"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSaveDraft,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CKOnSurfaceVariant)
        ) {
            Text("Save Draft", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onPublish,
            enabled = !isLoading,
            modifier = Modifier.weight(2f).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(publishLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
