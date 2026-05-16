package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.ui.LibraryUiState
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.ui.LibraryViewModel
import software.ulpgc.cheffskiss.ui.MealPlanViewModel
import software.ulpgc.cheffskiss.ui.components.HomeBottomBar
import software.ulpgc.cheffskiss.ui.theme.*

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    mealPlanViewModel: MealPlanViewModel,
    onGoHome: () -> Unit,
    onExploreClick: () -> Unit,
    onCreateRecipe: () -> Unit,

    onRecipeClick: (Recipe) -> Unit,
    onMealPlanClick: (MealPlan) -> Unit,
    onCreateCollection: () -> Unit,
    onCollectionClick: (RecipeCollection) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            HomeBottomBar(
                currentRoute   = "library",
                onHomeClick    = onGoHome,
                onExploreClick = onExploreClick,
                onCreateClick  = onCreateRecipe,
                onSavedClick   = {}
            )
        },
        floatingActionButton = {
            val onClick = when (selectedTab) {
                0    -> onCreateRecipe
                1    -> onCreateCollection
                else -> mealPlanViewModel::showCreateDialog
            }
            val label = when (selectedTab) {
                0    -> "New Recipe"
                1    -> "New Collection"
                else -> "New Meal Plan"
            }
            FloatingActionButton(
                onClick        = onClick,
                containerColor = Primary,
                contentColor   = OnPrimary,
                shape          = CircleShape,
                modifier       = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = label, modifier = Modifier.size(28.dp))
            }
        }
        ){ padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text("Library", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Primary)
                Text("Your recipes and meal plans.", fontSize = 13.sp, color = CKOnSurfaceVariant)
            }

            // ── Tabs (pill style) ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Surface),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                LibraryPillTab(
                    label    = "My recipes",
                    icon     = Icons.Default.MenuBook,
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick  = { selectedTab = 0; selectedFilter = "All" }
                )
                LibraryPillTab(
                    label    = "Saved",
                    icon     = Icons.Default.Bookmark,
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick  = { selectedTab = 1; selectedFilter = "All" }
                )
                LibraryPillTab(
                    label    = "Meal Plans",
                    icon     = Icons.Default.CalendarMonth,
                    selected = selectedTab == 2,
                    modifier = Modifier.weight(1f),
                    onClick  = { selectedTab = 2 }
                )
            }

            // ── Filter chips (solo My recipes) ────────────────────────────────
            if (selectedTab == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    items(libraryFilterTags) { tag ->
                        val active = tag.label == selectedFilter
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) CKPrimary else Surface)
                                .clickable {
                                    selectedFilter = tag.label
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = tag.icon,
                                contentDescription = null,
                                tint = if (active) Color.White else CKOnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                tag.label,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else CKOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when (selectedTab) {
                2 -> MealPlanScreen(
                    viewModel  = mealPlanViewModel,
                    onPlanClick = onMealPlanClick
                )
                else -> when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    state.error != null -> {
                        LibraryErrorState(message = state.error!!, onRetry = viewModel::load)
                    }
                    else -> {
                        val allRecipes = if (selectedTab == 0) state.myRecipes else state.savedRecipes
                        val recipes = if (selectedFilter == "All") allRecipes
                        else allRecipes.filter { r -> r.tags.any { it.equals(selectedFilter, ignoreCase = true) } }

                        val emptyIcon = if (selectedTab == 0) Icons.Default.MenuBook else Icons.Default.Bookmark
                        val emptyTitle = if (selectedTab == 0) "No recipes yet" else "No saved recipes"
                        val emptySubtitle = if (selectedTab == 0)
                            "Publish your first recipe to see it here."
                        else
                            "Save recipes from the feed to find them here."

                        val showCollections = selectedTab == 1 && state.collections.isNotEmpty()

                        if (recipes.isEmpty() && !showCollections) {
                            // Solo muestra empty state si no hay nada que mostrar
                            LibraryEmptyState(icon = emptyIcon, title = emptyTitle, subtitle = emptySubtitle)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (showCollections) {
                                    item {
                                        Text(
                                            "My Collections",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CKOnSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    items(state.collections, key = { it.id }) { collection ->
                                        CollectionCard(
                                            collection = collection,
                                            onClick = {onCollectionClick(collection) }
                                        )
                                    }
                                    item { Spacer(Modifier.height(8.dp)) }
                                }

                                if (recipes.isNotEmpty()) {
                                    if (showCollections) {
                                        item {
                                            Text(
                                                "Saved Recipes",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = CKOnSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                    items(recipes, key = { it.id }) { recipe ->
                                        LibraryRecipeCard(
                                            recipe     = recipe,
                                            authorName = state.authorNames[recipe.id.toString()] ?: "",
                                            onClick    = { onRecipeClick(recipe) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Pill tab ──────────────────────────────────────────────────────────────────
@Composable
private fun LibraryPillTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badge: Int? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) Primary else Color.Transparent
    val fg = if (selected) OnPrimary else CKOnSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier              = Modifier.height(20.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
            Text(
                label,
                fontSize   = 12.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = fg
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .background(
                            if (selected) OnPrimary.copy(alpha = 0.25f) else Primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$badge",
                        fontSize   = 9.sp,
                        lineHeight = 9.sp,
                        color      = OnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Filter tags ───────────────────────────────────────────────────────────────
private data class LibraryFilterTag(val label: String, val icon: ImageVector)

private val libraryFilterTags = listOf(
    LibraryFilterTag("All",       Icons.Default.GridView),
    LibraryFilterTag("Vegan",     Icons.Default.Eco),
    LibraryFilterTag("Protein",   Icons.Default.FitnessCenter),
    LibraryFilterTag("Dessert",   Icons.Default.Cake),
    LibraryFilterTag("Quick",     Icons.Default.Bolt),
    LibraryFilterTag("Artisanal", Icons.Default.AutoFixHigh)
)

// ── Recipe Card ───────────────────────────────────────────────────────────────
@Composable
private fun LibraryRecipeCard(
    recipe: Recipe,
    authorName: String,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CKSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(28.dp))
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (authorName.isNotBlank()) {
                    Text(authorName, fontSize = 12.sp, color = CKOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Timer, null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("${recipe.duration.inWholeMinutes}m", fontSize = 11.sp, color = Outline)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.People, null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("${recipe.servings} servings", fontSize = 11.sp, color = Outline)
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = CKOutlineVariant)
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun LibraryEmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CKSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(40.dp))
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text(subtitle, fontSize = 13.sp, color = CKOnSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Error State ───────────────────────────────────────────────────────────────
@Composable
private fun LibraryErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.WifiOff, null, tint = CKOutlineVariant, modifier = Modifier.size(48.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Retry") }
        }
    }
}
@Composable
private fun CollectionCard(
    collection: RecipeCollection,
    onClick: () -> Unit
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
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CKSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (collection.image.isNotBlank()) {
                    AsyncImage(
                        model = collection.image,
                        contentDescription = collection.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.CollectionsBookmark,
                        null,
                        tint = Outline,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collection.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        tint = Outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "${collection.recipes.size} recipes",
                        fontSize = 11.sp,
                        color = Outline
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = CKOutlineVariant)
        }
    }
}