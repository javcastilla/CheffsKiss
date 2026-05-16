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
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.ui.RecipeCollectionDetailViewModel
import software.ulpgc.cheffskiss.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCollectionDetailScreen(
    collectionId: String,
    viewModel: RecipeCollectionDetailViewModel,
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit   // recipeId
) {
    val state by viewModel.uiState.collectAsState()

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
                        color = OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = OnSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Botón para añadir recetas
                    IconButton(onClick = viewModel::openRecipePicker) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add recipe",
                                tint = OnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background.copy(alpha = 0.95f)
                )
            )
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

                    // ── Hero de la colección ───────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Surface)
                                .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) {
                            if (collection.image.isNotBlank()) {
                                AsyncImage(
                                    model = collection.image,
                                    contentDescription = collection.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(CKSurfaceVariant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CollectionsBookmark,
                                                null,
                                                tint = Primary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Text("No cover photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                    }
                                }
                            }
                        }
                    }

                    // ── Info: nombre + contador ───────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CollectionStatItem(
                                    icon = Icons.Default.MenuBook,
                                    label = "RECIPES",
                                    value = "${collection.recipes.size}"
                                )

                            }
                        }
                    }
                    // ── Lista de recetas ──────────────────────────────────────
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
                            val title = state.recipeTitles[recipeId.toString()] ?: "Loading..."
                            CollectionRecipeRow(
                                recipeId = recipeId.toString(),
                                title = title,
                                onClick = { onRecipeClick(recipeId.toString()) },
                                onRemove = { viewModel.removeRecipe(recipeId) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }

        // ── Recipe Picker Dialog ─────────────────────────────────────────────
        if (state.recipePicker.isVisible) {
            RecipePickerDialog(
                query = state.recipePicker.recipePickerQuery,
                onQueryChange = viewModel::onRecipePickerQueryChange,
                availableRecipes = state.availableRecipes.filter { r ->
                    state.recipePicker.recipePickerQuery.isBlank() ||
                            r.title.contains(state.recipePicker.recipePickerQuery, ignoreCase = true)
                },
                alreadyAdded = state.collection?.recipes?.toSet() ?: emptySet(),
                onAdd = viewModel::addRecipe,
                onDismiss = viewModel::closeRecipePicker
            )
        }
    }
}

// ── Fila de receta en la colección ───────────────────────────────────────────
@Composable
private fun CollectionRecipeRow(
    recipeId: String,
    title: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thumbnail placeholder (igual que LibraryRecipeCard)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CKSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(28.dp))
            }

            // Título
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text("Tap to view details", fontSize = 11.sp, color = CKOutlineVariant)
            }

            // Quitar de la colección
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from collection",
                    tint = CKOutlineVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Stat item (igual que RecipeDetailScreen) ─────────────────────────────────
@Composable
private fun CollectionStatItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(22.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CKOnSurfaceVariant, letterSpacing = 0.8.sp)
    }
}

// ── Dialog para añadir recetas ────────────────────────────────────────────────
@Composable
private fun RecipePickerDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    availableRecipes: List<Recipe>,
    alreadyAdded: Set<java.util.UUID>,
    onAdd: (Recipe) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Add a recipe", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OnSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search recipes...", fontSize = 14.sp, color = CKOutlineVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CKOutlineVariant.copy(alpha = 0.4f),
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                if (availableRecipes.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recipes found", fontSize = 13.sp, color = CKOutlineVariant)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableRecipes.take(6).forEach { recipe ->
                            val alreadyIn = recipe.id in alreadyAdded
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (alreadyIn) CKSurfaceVariant else Background)
                                    .clickable(enabled = !alreadyIn) { onAdd(recipe) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CKSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    recipe.title,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (alreadyIn) CKOutlineVariant else OnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (alreadyIn) {
                                    Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CKOnSurfaceVariant)
            }
        }
    )
}