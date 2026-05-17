package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.components.RecipeAsyncImage
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.ui.RecipeCollectionDetailViewModel
import software.ulpgc.cheffskiss.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCollectionDetailScreen(
    collectionId: String,
    viewModel: RecipeCollectionDetailViewModel,
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(collectionId) {
        viewModel.load(collectionId)
    }


    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.collection?.name ?: "Collection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Surface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = OnSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                actions = {
                    if (state.collection != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit collection",
                                tint = OnSurface,
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete collection",
                                tint = Color(0xFFBA1A1A),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background.copy(alpha = 0.95f),
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openRecipePicker,
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add recipe", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->

        when {
            state.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            state.error != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = CKOutlineVariant, modifier = Modifier.size(48.dp))
                        Text(state.error!!, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Button(
                            onClick = { viewModel.load(collectionId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Retry") }
                    }
                }
            }

            state.collection != null -> {
                val collection = state.collection!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (collection.image.isNotBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Surface)
                                    .border(
                                        2.dp,
                                        CKOutlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(24.dp),
                                    ),
                            ) {
                                RecipeAsyncImage(
                                    url = collection.image,
                                    contentDescription = collection.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recipes in this collection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CKOnSurfaceVariant
                            )
                            if (collection.recipes.isNotEmpty()) {
                                Text(
                                    "${collection.recipes.size} total",
                                    fontSize = 12.sp,
                                    color = CKOutlineVariant
                                )
                            }
                        }
                    }

                    if (collection.recipes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(CKSurfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Restaurant,
                                            null,
                                            tint = Primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text("No recipes yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                                    Text(
                                        "Tap + to add recipes to this collection.",
                                        fontSize = 13.sp,
                                        color = CKOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = collection.recipes,
                            key = { it.toString() }
                        ) { recipeId ->
                            val recipe = state.recipeDetails[recipeId.toString()]
                            val authorName = recipe?.creator?.id?.toString()
                                ?.let { state.authorNames[it] }
                                .orEmpty()

                            CollectionRecipeRow(
                                recipe = recipe,
                                authorName = authorName,
                                onClick = { onRecipeClick(recipeId.toString()) },
                                onRemove = { viewModel.removeRecipe(recipeId) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }

        if (state.recipePicker.isVisible) {
            RecipePickerSheet(
                query = state.recipePicker.recipePickerQuery,
                onQueryChange = viewModel::onRecipePickerQueryChange,
                availableRecipes = state.availableRecipes,
                alreadyAdded = state.collection?.recipes?.toSet() ?: emptySet(),
                onAdd = viewModel::addRecipe,
                onDismiss = viewModel::closeRecipePicker
            )
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text("Delete collection?", fontWeight = FontWeight.ExtraBold, color = OnSurface)
                },
                text = {
                    Text(
                        "This will permanently delete \"${state.collection?.name}\". This action cannot be undone.",
                        color = CKOnSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = CKOnSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun CollectionRecipeRow(
    recipe: Recipe?,
    authorName: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CKSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (recipe?.image != null) {
                        RecipeAsyncImage(
                            url = recipe.image?.toString(),
                            contentDescription = recipe.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(26.dp))
                    }
                }

                // Info
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        recipe?.title ?: "Loading...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (authorName.isNotBlank()) {
                        Text(
                            "by $authorName",
                            fontSize = 12.sp,
                            color = CKOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    // Duration + Servings (igual que Library)
                    if (recipe != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Timer, null, tint = Outline, modifier = Modifier.size(12.dp))
                                Text("${recipe.duration.inWholeMinutes}m", fontSize = 11.sp, color = Outline)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.People, null, tint = Outline, modifier = Modifier.size(12.dp))
                                Text("${recipe.servings} srv", fontSize = 11.sp, color = Outline)
                            }
                        }
                    }
                }

            IconButton(onClick = { showRemoveDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from collection",
                    tint = CKOutlineVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    null,
                    tint = Color(0xFFBA1A1A),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Remove recipe?", fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Text(
                    "Remove \"${recipe?.title ?: "this recipe"}\" from the collection?",
                    color = CKOnSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = CKOnSurfaceVariant)
                }
            }
        )
    }
}
// ── Dialog para añadir recetas ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    availableRecipes: List<Recipe>,
    alreadyAdded: Set<java.util.UUID>,
    onAdd: (Recipe) -> Unit,
    onDismiss: () -> Unit
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Select recipe",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search...", color = CKOutlineVariant) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = CKOutlineVariant)
                },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = CKSurfaceVariant,
                    cursorColor = Primary
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )

            // Opción "No recipe"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CKSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = CKOnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                Text("No recipe", fontSize = 14.sp, color = CKOnSurfaceVariant)
            }

            HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)

            val filtered = availableRecipes.filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true)
            }

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
                        val alreadyIn = recipe.id in alreadyAdded
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (alreadyIn) CKSurfaceVariant else Color.Transparent)
                                .clickable(enabled = !alreadyIn) { onAdd(recipe) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(CKSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
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