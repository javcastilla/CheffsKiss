package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.ui.ExploreViewModel
import software.ulpgc.cheffskiss.ui.components.TabScaffold
import software.ulpgc.cheffskiss.ui.navigation.MainBottomNavigation
import software.ulpgc.cheffskiss.ui.theme.*

// ── Tag icon mapping ──────────────────────────────────────────────────────────
private fun tagIcon(tag: String): ImageVector = when (tag.lowercase()) {
    "vegan", "vegano", "vegetarian", "vegetariano" -> Icons.Default.Eco
    "protein", "proteína", "proteina"              -> Icons.Default.FitnessCenter
    "dessert", "postre", "dulce"                   -> Icons.Default.Cake
    "quick", "rápido", "rapido", "fast"            -> Icons.Default.Bolt
    "soup", "sopa", "caldo"                        -> Icons.Default.RoomService
    "breakfast", "desayuno"                        -> Icons.Default.WbSunny
    "dinner", "cena"                               -> Icons.Default.DinnerDining
    "lunch", "almuerzo", "comida"                  -> Icons.Default.LunchDining
    "spicy", "picante"                             -> Icons.Default.Whatshot
    "healthy", "saludable"                         -> Icons.Default.FavoriteBorder
    "italian", "italiana", "italiano"              -> Icons.Default.LocalPizza
    "asian", "asiático", "asiatico"                -> Icons.Default.RiceBowl
    "bbq", "grill", "parrilla"                     -> Icons.Default.OutdoorGrill
    "seafood", "mariscos", "pescado"               -> Icons.Default.SetMeal
    "pasta"                                        -> Icons.Default.DinnerDining
    else                                           -> Icons.Default.Label
}

// ── Pin color palette (solid, no gradients) ───────────────────────────────────
private val PIN_COLORS = listOf(
    Color(0xFF1B4332),
    Color(0xFF1A3A5C),
    Color(0xFF4A1942),
    Color(0xFF7B3F00),
    Color(0xFF003049),
    Color(0xFF3D0C02),
    Color(0xFF0D3B2E),
    Color(0xFF2C1654)
)

// Deterministic variable hero heights for masonry effect
private val PIN_HERO_HEIGHT = 140.dp

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onRecipeClick: (Recipe) -> Unit,
    onHomeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateRecipe: () -> Unit,
    onCreateMealPlan: () -> Unit,
    onCreateList: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyStaggeredGridState()

    TabScaffold(
        currentRoute = MainBottomNavigation.EXPLORE,
        onHomeClick = onHomeClick,
        onExploreClick = {},
        onLibraryClick = onLibraryClick,
        onProfileClick = onProfileClick,
        onCreateRecipe = onCreateRecipe,
        onCreateMealPlan = onCreateMealPlan,
        onCreateList = onCreateList,
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // ── Sticky header ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
            ) {
                ExploreHeader(
                    query         = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange
                )
                if (state.availableTags.isNotEmpty()) {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(state.availableTags, key = { it }) { tag ->
                            val selected = tag in state.selectedTags
                            ExploreTagChip(
                                tag      = tag,
                                selected = selected,
                                onClick  = { viewModel.toggleTag(tag) }
                            )
                        }
                    }
                }
            }

            // ── Scrollable grid ───────────────────────────────────────────────
            LazyVerticalStaggeredGrid(
                columns               = StaggeredGridCells.Fixed(2),
                state                 = gridState,
                modifier              = Modifier.weight(1f).fillMaxWidth(),
                contentPadding        = PaddingValues(
                    start  = 12.dp,
                    end    = 12.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing   = 10.dp
            ) {
            // ── Results bar ───────────────────────────────────────────────────
            item(span = StaggeredGridItemSpan.FullLine, key = "resultsBar") {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val label = when {
                        state.isLoading        -> "Loading…"
                        state.hasActiveFilters -> "${state.resultCount} result${if (state.resultCount != 1) "s" else ""}"
                        else                   -> "${state.resultCount} recipe${if (state.resultCount != 1) "s" else ""}"
                    }
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)

                    AnimatedVisibility(state.hasActiveFilters, enter = fadeIn(), exit = fadeOut()) {
                        TextButton(
                            onClick        = viewModel::clearFilters,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.FilterListOff, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = Primary) }
                    }
                }

                state.error != null -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ExploreErrorState(error = state.error!!, onRetry = viewModel::load)
                    }
                }

                state.filteredRecipes.isEmpty() && state.hasActiveFilters -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ExploreEmptyResult(
                            query   = state.searchQuery,
                            tags    = state.selectedTags,
                            onClear = viewModel::clearFilters
                        )
                    }
                }

                else -> {
                    items(state.filteredRecipes, key = { it.id }) { recipe ->
                        val hash     = (recipe.id.hashCode() and 0x7FFFFFFF)
                        val pinColor = PIN_COLORS[hash % PIN_COLORS.size]
                        PinCard(
                            recipe     = recipe,
                            authorName = state.authorNames[recipe.creator.id.toString()] ?: "",
                            pinColor   = pinColor,
                            heroHeight = PIN_HERO_HEIGHT,
                            onClick    = { onRecipeClick(recipe) }
                        )
                    }
                }
            }
            } // end LazyVerticalStaggeredGrid
        } // end outer Column
    }
}

// ── Header with search ────────────────────────────────────────────────────────
@Composable
private fun ExploreHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 24.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Explore,
                contentDescription = null,
                tint     = Primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Explore",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 28.sp,
                color      = Primary
            )
        }
        Text(
            "Discover recipes by name and tag",
            fontSize = 13.sp,
            color    = CKOnSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            placeholder   = { Text("Search recipes…", fontSize = 14.sp, color = CKOutlineVariant) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant) },
            trailingIcon  = {
                AnimatedVisibility(query.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape      = RoundedCornerShape(50.dp),
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Primary,
                unfocusedBorderColor    = Color.Transparent,
                focusedContainerColor   = Surface,
                unfocusedContainerColor = Surface,
                focusedTextColor        = OnSurface,
                unfocusedTextColor      = OnSurface,
                cursorColor             = Primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}

// ── Tag chip ──────────────────────────────────────────────────────────────────
@Composable
private fun ExploreTagChip(tag: String, selected: Boolean, onClick: () -> Unit) {
    val bg   = if (selected) Primary else Surface
    val fg   = if (selected) OnPrimary else CKOnSurfaceVariant
    val icon = tagIcon(tag)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            Text(
                tag,
                fontSize   = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = fg
            )
            if (selected) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.Check, null, tint = fg, modifier = Modifier.size(11.dp))
            }
        }
    }
}

// ── Pinterest pin card ────────────────────────────────────────────────────────
@Composable
private fun PinCard(
    recipe: Recipe,
    authorName: String,
    pinColor: Color,
    heroHeight: Dp,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Hero area ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(pinColor),
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
                    val heroIcon = if (recipe.tags.isNotEmpty()) tagIcon(recipe.tags.first()) else Icons.Default.Restaurant
                    Icon(
                        heroIcon,
                        contentDescription = null,
                        tint     = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(heroHeight * 0.7f)
                    )
                }

                // Duration — bottom start
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Text(
                            "${recipe.duration.inWholeMinutes}m",
                            fontSize   = 10.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Servings — bottom end
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.People, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Text(
                            "${recipe.servings}",
                            fontSize   = 10.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = OnSurface,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                if (authorName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.height(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(pinColor.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (authorName.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                fontSize   = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color      = pinColor
                            )
                        }
                        Text(
                            authorName,
                            fontSize   = 11.sp,
                            lineHeight = 11.sp,
                            color      = CKOnSurfaceVariant,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }

                if (recipe.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipe.tags.take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(pinColor.copy(alpha = 0.1f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    tag,
                                    fontSize   = 9.sp,
                                    color      = pinColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────
@Composable
private fun ExploreErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.WifiOff, null, tint = CKOutlineVariant, modifier = Modifier.size(48.dp))
            Text(error, style = MaterialTheme.typography.titleMedium, color = OnSurface, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Retry")
            }
        }
    }
}

// ── Empty result state ────────────────────────────────────────────────────────
@Composable
private fun ExploreEmptyResult(query: String, tags: Set<String>, onClear: () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .background(CKSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SearchOff, null, tint = Primary, modifier = Modifier.size(40.dp))
            }
            Text("No results", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
            val detail = buildString {
                if (query.isNotBlank()) append("\"$query\"")
                if (query.isNotBlank() && tags.isNotEmpty()) append(" · ")
                if (tags.isNotEmpty()) append(tags.joinToString(", "))
            }
            Text(
                "No recipes found for $detail.",
                fontSize  = 13.sp,
                color     = CKOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onClear,
                shape   = CircleShape,
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Text("Clear filters", fontWeight = FontWeight.Bold)
            }
        }
    }
}
