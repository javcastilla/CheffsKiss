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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    lines: List<RecipeLine>,
    steps: List<Step>,
    authorName: String,
    isSaved: Boolean,
    isOwner: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val checkedLines = remember { mutableStateListOf<Int>() }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Recipe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background.copy(alpha = 0.95f))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero Image ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface)
                    .border(2.dp, CKOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                if (recipe.image != null) {
                    AsyncImage(
                        model = recipe.image.toString(),
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).background(CKSurfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Restaurant, null, tint = Primary, modifier = Modifier.size(26.dp))
                            }
                            Text("No photo available", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                    }
                }
            }

            // ── Title, Author, Tags, Actions ──────────────────────────────────
            DetailCard(icon = Icons.Default.RestaurantMenu, title = recipe.title) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    Text("by ", fontSize = 13.sp, color = CKOnSurfaceVariant)
                    Text(authorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                }
                if (recipe.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipe.tags.forEach { tag ->
                            Box(
                                modifier = Modifier.background(Primary, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(tag, fontSize = 12.sp, color = OnPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                HorizontalDivider(color = CKOutlineVariant.copy(alpha = 0.3f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isOwner) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                null, modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSaved) "Saved" else "Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    if (isOwner) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                            colors = outlinedButtonColors(contentColor = Primary, containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                if (isOwner) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.4f)),
                        colors = outlinedButtonColors(contentColor = Color(0xFFBA1A1A), containerColor = Color.Transparent)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Recipe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ── Metadata ──────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetaStatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        label = "SERVINGS",
                        value = "${recipe.servings}"
                    )
                    VerticalDivider(
                        modifier = Modifier.height(60.dp).align(Alignment.CenterVertically),
                        color = CKOutlineVariant.copy(alpha = 0.3f)
                    )
                    MetaStatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        label = "TOTAL TIME",
                        value = "${recipe.duration.inWholeMinutes} min"
                    )
                }
            }

            // ── Ingredients ───────────────────────────────────────────────────
            DetailCard(icon = Icons.Default.ShoppingBasket, title = "Ingredients") {
                if (lines.isEmpty()) {
                    Text("No ingredients added", fontSize = 13.sp, color = CKOnSurfaceVariant)
                } else {
                    lines.forEachIndexed { index, line ->
                        val checked = checkedLines.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (checked) CKSurfaceVariant else Background)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) checkedLines.add(index) else checkedLines.remove(index)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Primary,
                                    uncheckedColor = CKOutlineVariant,
                                    checkmarkColor = OnPrimary
                                )
                            )
                            Text(
                                "${line.amount} ${line.measurement?.name?.lowercase() ?: ""}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (checked) CKOnSurfaceVariant else OnSurface
                            )
                        }
                    }
                }
            }

            // ── Steps ─────────────────────────────────────────────────────────
            DetailCard(icon = Icons.Default.FormatListNumbered, title = "Preparation") {
                val sortedSteps = steps.sortedBy { it.cardinal }
                if (sortedSteps.isEmpty()) {
                    Text("No steps added", fontSize = 13.sp, color = CKOnSurfaceVariant)
                } else {
                    sortedSteps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (index < sortedSteps.lastIndex) 12.dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (index == 0) Primary else Background, CircleShape)
                                    .border(
                                        if (index == 0) 0.dp else 2.dp,
                                        CKOutlineVariant.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${step.cardinal}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) OnPrimary else CKOutlineVariant
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Background, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(step.description, fontSize = 14.sp, color = OnSurface, lineHeight = 22.sp)
                                if ((step.duration?.inWholeMinutes ?: 0) > 0) {
                                    HorizontalDivider(color = CKOutlineVariant.copy(alpha = 0.2f))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(12.dp))
                                        Text("${step.duration?.inWholeMinutes} min", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CKOutlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailCard(
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
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            content()
        }
    }
}

@Composable
private fun MetaStatItem(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(26.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant, letterSpacing = 0.8.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
    }
}