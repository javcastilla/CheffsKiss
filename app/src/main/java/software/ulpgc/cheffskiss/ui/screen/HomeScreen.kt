package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.ui.ActivePlanDay
import software.ulpgc.cheffskiss.ui.HomeUiState
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.theme.*
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.ui.components.HomeBottomBar

private data class FilterTag(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val filterTags = listOf(
    FilterTag("All",       Icons.Default.GridView),
    FilterTag("Vegan",     Icons.Default.Eco),
    FilterTag("Protein",   Icons.Default.FitnessCenter),
    FilterTag("Dessert",   Icons.Default.Cake),
    FilterTag("Quick",     Icons.Default.Bolt),
    FilterTag("Artisanal", Icons.Default.AutoFixHigh)
)

// ──────────────────── Route ────────────────────
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    authViewModel: AuthenticantionViewModel = viewModel(),
    onCreateRecipe: () -> Unit,
    onLibraryClick: () -> Unit,
    onExploreClick: () -> Unit = {},
    onLogout: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onViewAll: () -> Unit = {},
    onMealPlanClick: (planId: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val authorNames by viewModel.authorNames.collectAsState()
    HomeScreen(
        state          = state,
        authorNames    = authorNames,
        onCreateRecipe = onCreateRecipe,
        onLibraryClick = onLibraryClick,
        onExploreClick = onExploreClick,
        onLogout       = { authViewModel.logout(); onLogout() },
        onRecipeClick  = onRecipeClick,
        onToggleSave   = viewModel::toggleSave,
        onRetry        = viewModel::retryLoad,
        onViewAll      = onViewAll,
        onMealPlanClick = onMealPlanClick
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    authorNames: Map<String, String>,
    onCreateRecipe: () -> Unit,
    onLibraryClick: () -> Unit,
    onExploreClick: () -> Unit = {},
    onLogout: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onToggleSave: (Recipe) -> Unit,
    onRetry: () -> Unit,
    onViewAll: () -> Unit = {},
    onMealPlanClick: (planId: String) -> Unit = {}
) {
    var selectedTag by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var prevIndex  = 0
        var prevOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                fabVisible = index < prevIndex || (index == prevIndex && offset <= prevOffset)
                prevIndex  = index
                prevOffset = offset
            }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            HomeBottomBar(
                currentRoute  = "home",
                onHomeClick   = {},
                onExploreClick = onExploreClick,
                onCreateClick = onCreateRecipe,
                onSavedClick  = onLibraryClick
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter   = scaleIn() + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick        = onCreateRecipe,
                    containerColor = Primary,
                    contentColor   = OnPrimary,
                    shape          = CircleShape,
                    modifier       = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New recipe", modifier = Modifier.size(28.dp))
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

        if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.WifiOff, null, tint = CKOutlineVariant, modifier = Modifier.size(48.dp))
                    Text(state.error, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Retry") }
                }
            }
            return@Scaffold
        }

        val filtered = state.recipes.filter { recipe ->
            (searchQuery.isBlank() || recipe.title.contains(searchQuery, ignoreCase = true)) &&
            (selectedTag == "All" || recipe.tags.any { it.equals(selectedTag, ignoreCase = true) })
        }

        LazyColumn(
            state   = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            stickyHeader(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                ) {
                    HomeHeader(onLogout)
                    HomeSearchBar(
                        query         = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier      = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    TagChipRow(
                        tags     = filterTags,
                        selected = selectedTag,
                        onSelect = { selectedTag = it },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                if (state.activePlanDay != null) {
                    ActivePlanBanner(
                        planDay  = state.activePlanDay,
                        onClick  = { onMealPlanClick(state.activePlanDay.planId) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                } else {
                    FeaturedBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fresh Recommendations", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OnBackground)
                    Text("View all", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onViewAll() })
                }
            }
            items(filtered, key = { it.id }) { recipe ->
                RecipeItemCard(
                    recipe      = recipe,
                    authorName  = authorNames[recipe.creator.id.toString()] ?: "...",
                    isSaved     = recipe.id.toString() in state.savedRecipeIds,
                    isOwn       = state.currentUserId?.let { uid ->
                        recipe.creator.id == UserIds.creatorIdFromFirebaseUid(uid)
                    } ?: false,
                    onSave      = { onToggleSave(recipe) },
                    onRecipeClick = onRecipeClick,
                    modifier    = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            item { CtaBanner(onClick = onCreateRecipe, modifier = Modifier.padding(20.dp)) }
        }
    }
}

// ──────────────────── Header ────────────────────
@Composable
private fun HomeHeader(onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CheffsKiss", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Primary, letterSpacing = (-0.5).sp)
            Text("Good evening, Chef 👋", fontSize = 13.sp, color = CKOnSurfaceVariant)
        }
        IconButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = OnBackground)
        }
    }
}

// ──────────────────── Search Bar ────────────────────
@Composable
private fun HomeSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search the archives…", fontSize = 14.sp, color = CKOutlineVariant) },
        leadingIcon  = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant) },
        trailingIcon = { Icon(Icons.Default.Tune, null, tint = Outline) },
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = Primary,
            unfocusedBorderColor  = Color.Transparent,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedTextColor      = OnSurface,
            unfocusedTextColor    = OnSurface,
            cursorColor           = Primary
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    )
}

// ──────────────────── Tag Chips ────────────────────
@Composable
private fun TagChipRow(tags: List<FilterTag>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(tags) { tag ->
            val active = tag.label == selected
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) CKPrimary else Surface)
                    .clickable { onSelect(tag.label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = tag.icon,
                    contentDescription = null,
                    tint = if (active) androidx.compose.ui.graphics.Color.White else CKOnSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    tag.label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) androidx.compose.ui.graphics.Color.White else CKOnSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────── Featured Banner ────────────────────
@Composable
private fun FeaturedBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Primary, Color(0xFF004D1F)),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Row(
                modifier = Modifier
                    .background(CKSecondary.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = CKSecondary, modifier = Modifier.size(12.dp))
                Text("FEATURED SEASONAL", fontSize = 10.sp, color = CKSecondary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Black Rice\nwith Aioli", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White, lineHeight = 26.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = OnPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Text("45m", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.8f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Restaurant, null, tint = OnPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Text("4 servings", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ──────────────────── Active Plan Banner ────────────────────
@Composable
private fun ActivePlanBanner(
    planDay: ActivePlanDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier  = modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(20.dp),
        colors    = androidx.compose.material3.CardDefaults.cardColors(containerColor = Surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    planDay.todayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(planDay.planName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
                    Icon(Icons.Default.ChevronRight, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                }
            }

            if (planDay.slots.isEmpty()) {
                Text("No slots planned for today", fontSize = 13.sp, color = CKOnSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    planDay.slots.take(4).forEach { slot ->
                        val color = mealSlotColor(slot)
                        val recipeTitle = slot.recipe?.title
                            ?: slot.recipe?.id?.let { planDay.recipeTitles[it.toString()] }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Background),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(44.dp)
                                    .background(color, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                slot.mealType.label(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.width(56.dp)
                            )
                            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Text(
                                    recipeTitle ?: "No recipe assigned",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    slot.mealType.label(),
                                    fontSize = 11.sp,
                                    color = CKOnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    if (planDay.slots.size > 4) {
                        Text(
                            "+${planDay.slots.size - 4} more",
                            fontSize = 11.sp,
                            color = CKOnSurfaceVariant,
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────── Recipe Card ────────────────────
@Composable
private fun RecipeItemCard(
    recipe: Recipe,
    authorName: String,
    isSaved: Boolean,
    isOwn: Boolean,
    onSave: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        onClick   = { onRecipeClick(recipe) },
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
                if (recipe.image != null) {
                    AsyncImage(
                        model = recipe.image.toString(),
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(28.dp))
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(authorName, fontSize = 12.sp, color = CKOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            // Bookmark button (hidden for own recipes)
            if (!isOwn) {
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save recipe",
                        tint = if (isSaved) Primary else CKOutlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ──────────────────── CTA Banner ────────────────────
@Composable
private fun CtaBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Primary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What did you\ncook today?", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Log your latest culinary creation\ninto the digital ledger.", fontSize = 13.sp, color = OnPrimary.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CKSecondary, contentColor = CKOnSecondary),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start Entry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
