package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.ui.theme.*

private val allRecipesTags = listOf("All", "Vegan", "Protein", "Dessert", "Quick", "Artisanal")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecipesScreen(
    recipes: List<Recipe>,
    savedRecipeIds: Set<String>,
    authorNames: Map<String, String>,
    currentUserId: String? = null,
    onBack: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onToggleSave: (Recipe) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All") }

    val filtered = remember(recipes, searchQuery, selectedTag) {
        recipes.filter { recipe ->
            (searchQuery.isBlank() || recipe.title.contains(searchQuery, ignoreCase = true)) &&
            (selectedTag == "All" || recipe.tags.any { it.equals(selectedTag, ignoreCase = true) })
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(modifier = Modifier.background(Background)) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Fresh Picks",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = OnBackground,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "${filtered.size} recipes",
                            fontSize = 12.sp,
                            color = CKOutlineVariant
                        )
                    }
                    // Sort icon placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Surface)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CKOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recipes…", fontSize = 14.sp, color = CKOutlineVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CKOutlineVariant, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                        }}
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CKPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = CKPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Tag chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allRecipesTags) { tag ->
                        val active = tag == selectedTag
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) CKPrimary else Surface)
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                tag,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else CKOnSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SearchOff, null, tint = CKOutlineVariant, modifier = Modifier.size(52.dp))
                    Text("No recipes found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnBackground)
                    Text("Try a different search or filter", fontSize = 13.sp, color = CKOutlineVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filtered, key = { _, r -> r.id }) { index, recipe ->
                    AnimatedRecipeMenuCard(
                        recipe = recipe,
                        authorName = authorNames[recipe.creator.id.toString()] ?: "…",
                        isSaved = recipe.id.toString() in savedRecipeIds,
                        isOwn = currentUserId?.let { uid ->
                            recipe.creator.id == UserIds.creatorIdFromFirebaseUid(uid)
                        } ?: false,
                        index = index,
                        onSave = { onToggleSave(recipe) },
                        onRecipeClick = { onRecipeClick(recipe) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRecipeMenuCard(
    recipe: Recipe,
    authorName: String,
    isSaved: Boolean,
    isOwn: Boolean,
    index: Int,
    onSave: () -> Unit,
    onRecipeClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.coerceAtMost(8) * 40L)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "card_alpha_$index"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_y_$index"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = translateY.dp),
        onClick = onRecipeClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(0.dp)) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(88.dp)
                    .background(
                        if (isSaved) CKSecondary else CKPrimary,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CKSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = CKOutlineVariant, modifier = Modifier.size(26.dp))
                }

                // Info
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        recipe.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "by $authorName",
                        fontSize = 12.sp,
                        color = CKOutlineVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetaChip(icon = Icons.Default.Timer, label = "${recipe.duration.inWholeMinutes}m")
                        MetaChip(icon = Icons.Default.People, label = "${recipe.servings} srv")
                    }
                }

                // Bookmark (hidden for own recipes)
                if (!isOwn) {
                    IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isSaved) CKSecondary else CKOutlineVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = CKOutlineVariant, modifier = Modifier.size(11.dp))
        Text(label, fontSize = 11.sp, color = CKOutlineVariant)
    }
}
