package software.ulpgc.cheffskiss.ui.screen


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    authorName: String,
    isSaved: Boolean,
    isOwner: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()
    val checkedIngredients = remember { mutableStateListOf<Int>() }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recipe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Primary
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
                            Icon(Icons.Default.ArrowBack, null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // ── Hero Image ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                if (recipe.image.isNotBlank()) {
                    AsyncImage(
                        model = recipe.image,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CKSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            null,
                            tint = Outline,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // ── Title, Author, Tags ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    recipe.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                    lineHeight = 32.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("by ", fontSize = 14.sp, color = CKOnSurfaceVariant)
                    Text(
                        authorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
                if (recipe.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        recipe.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Surface, CircleShape)
                                    .border(1.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(tag, fontSize = 12.sp, color = CKOnSurfaceVariant, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── Save / Delete buttons ─────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onSave,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSaved) "Saved" else "Save", fontWeight = FontWeight.SemiBold)
                    }
                    if (isOwner) {
                        OutlinedButton(
                            onClick = onDelete,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFBA1A1A).copy(alpha = 0.5f)
                            ),
                            colors = outlinedButtonColors(
                                contentColor = Color(0xFFBA1A1A),
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Metadata Bento Grid ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Servings
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Group, null, tint = Primary, modifier = Modifier.size(28.dp))
                        Text(
                            "SERVINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CKOnSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            "${recipe.servings}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
                // Duration
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = Primary, modifier = Modifier.size(28.dp))
                        Text(
                            "TOTAL TIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CKOnSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            "${recipe.duration.inWholeMinutes} min",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Ingredients ───────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Ingredients",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Spacer(Modifier.height(12.dp))
                recipe.ingredients.forEachIndexed { index, ingredient ->
                    val checked = checkedIngredients.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (checked) CKSurfaceVariant else Surface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (it) checkedIngredients.add(index)
                                else checkedIngredients.remove(index)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Primary,
                                uncheckedColor = CKOutlineVariant,
                                checkmarkColor = OnPrimary
                            )
                        )
                        Text(
                            ingredient,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (checked) CKOnSurfaceVariant else OnSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Steps ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Preparation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                Spacer(Modifier.height(16.dp))

                val sortedSteps = recipe.steps.sortedBy { it.cardinal }
                sortedSteps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Number badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (index == 0) Primary else Surface,
                                    CircleShape
                                )
                                .border(
                                    if (index == 0) 0.dp else 2.dp,
                                    CKOutlineVariant.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${step.cardinal}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 0) OnPrimary else CKOutlineVariant
                            )
                        }

                        // Step card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        step.description,
                                        fontSize = 14.sp,
                                        color = OnSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                                // Duration badge top-right
                                if (step.duration.inWholeMinutes > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(
                                                CKSurfaceVariant,
                                                RoundedCornerShape(bottomStart = 12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Timer,
                                                null,
                                                tint = Primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                "${step.duration.inWholeMinutes} min",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Primary
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
}