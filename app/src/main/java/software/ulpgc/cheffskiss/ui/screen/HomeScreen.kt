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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.HomeUiState
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.theme.*
import software.ulpgc.cheffskiss.domain.model.Recipe
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator

private val tags = listOf("All", "Vegan", "Protein", "Dessert", "Quick", "Artisanal")

// ──────────────────── Screen principal ────────────────────
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onCreateRecipe: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val authorNames by viewModel.authorNames.collectAsState()
    HomeScreen(
        state = state,
        authorNames = authorNames,
        onCreateRecipe = onCreateRecipe,
        onLogout = onLogout
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    authorNames: Map<String, String>,
    onCreateRecipe: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTag by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        // ── Bottom Navigation Bar ──
        bottomBar = { HomeBottomBar(onCreateRecipe) },
        // ── FAB Create ──
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRecipe,
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create recipe", modifier = Modifier.size(28.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.error, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { /* TODO: retry button */ }) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold
        }

        val recipes = state.recipes

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Header ──
            item { HomeHeader(onLogout) }

            // ── Search bar ──
            item {
                HomeSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ── Tag chips ──
            item {
                TagChipRow(
                    tags = tags,
                    selected = selectedTag,
                    onSelect = { selectedTag = it },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ── Featured banner ──
            item { FeaturedBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }

            // ── Section title ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Fresh Recommendations",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = OnBackground
                    )
                    Text(
                        "View all",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { }
                    )
                }
            }

            // ── Recipe cards ──
            val filtered = recipes.filter { recipe ->
                // Filtro por nombre (search)
                val matchesSearch = searchQuery.isBlank() ||
                        recipe.title.contains(searchQuery, ignoreCase = true)

                // Filtro por tag (simplificado, ya que tu Recipe no tiene 'tag')
                val matchesTag = selectedTag == "All" ||
                        // TODO: si más adelante añades campo 'tag' a Recipe, aquí lo usas
                        true // por ahora muestra todas

                matchesSearch && matchesTag
            }

            items(filtered) { recipe ->
                RecipeItemCard(
                    recipe = recipe,
                    authorName = authorNames[recipe.author] ?: "...",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // ── My Recipes ledger ──
            item { MyRecipesSection(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }

            // ── CTA Banner ──
            item { CtaBanner(onClick = onCreateRecipe, modifier = Modifier.padding(20.dp)) }
        }
    }
}

// ──────────────────── Header ────────────────────
@Composable
private fun HomeHeader(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CheffsKiss", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Primary, letterSpacing = (-0.5).sp)
            Text("Good evening, Chef 👋", fontSize = 13.sp, color = CKOnSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = null, tint = OnBackground)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = OnBackground)
            }
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
        leadingIcon = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant) },
        trailingIcon = { Icon(Icons.Default.Tune, null, tint = Outline) },
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    )
}

// ──────────────────── Tag Chips ────────────────────
@Composable
private fun TagChipRow(tags: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(tags) { tag ->
            val active = tag == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) Primary else Surface)
                    .clickable { onSelect(tag) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    tag,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) OnPrimary else CKOnSurfaceVariant
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
        // Decoración circular
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
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
            Text("Arroz Negro\ncon Alioli", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White, lineHeight = 26.sp)
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


@Composable
private fun RecipeItemCard(
    recipe: Recipe,
    authorName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Placeholder imagen
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CKSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, null, tint = Outline, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Por $authorName", fontSize = 12.sp, color = CKOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Timer, null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("${recipe.duration.inWholeMinutes}m", fontSize = 11.sp, color = Outline)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.People, null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("4 servings", fontSize = 11.sp, color = Outline)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = CKOutlineVariant)
        }
    }
}

// ──────────────────── My Recipes Section ────────────────────
@Composable
private fun MyRecipesSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kitchen Ledger", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Primary)
                Icon(Icons.Default.MenuBook, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            listOf("01" to "Smoked Paprika Chicken", "02" to "Miso Ginger Broth", "03" to "Lemon Risotto").forEachIndexed { i, (num, title) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(num, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = CKOutlineVariant)
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSurface)
                            Text("MODIFIED OCT ${10 + i}", fontSize = 10.sp, color = CKOutlineVariant, letterSpacing = 0.5.sp)
                        }
                    }
                    Icon(Icons.Default.Edit, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                }
                if (i < 2) HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
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

// ──────────────────── Bottom Bar ────────────────────
@Composable
private fun HomeBottomBar(onCreateRecipe: () -> Unit) {
    NavigationBar(containerColor = Surface, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                indicatorColor = Primary.copy(alpha = 0.1f),
                unselectedIconColor = CKOutlineVariant
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Explore, null) },
            label = { Text("Explore", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = CKOutlineVariant, unselectedTextColor = CKOutlineVariant)
        )
        NavigationBarItem(
            selected = false,
            onClick = onCreateRecipe,
            icon = { Icon(Icons.Default.AddCircle, null) },
            label = { Text("Create", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = CKOutlineVariant, unselectedTextColor = CKOutlineVariant)
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Bookmark, null) },
            label = { Text("Saved", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = CKOutlineVariant, unselectedTextColor = CKOutlineVariant)
        )
    }
}
