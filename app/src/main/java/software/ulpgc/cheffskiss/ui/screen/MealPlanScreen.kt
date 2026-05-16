package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import software.ulpgc.cheffskiss.ui.MealPlanUiState
import software.ulpgc.cheffskiss.ui.MealPlanViewModel
import software.ulpgc.cheffskiss.ui.theme.*

// ── Slot color palette ────────────────────────────────────────────────────────
val SLOT_COLORS = listOf(
    Color(0xFF2D7D46),
    Color(0xFFD97706),
    Color(0xFFDC2626),
    Color(0xFF7C3AED),
    Color(0xFF0284C7),
    Color(0xFF0D9488),
    Color(0xFFDB2777),
    Color(0xFF92400E)
)

@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onPlanClick: (MealPlan) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            state.error != null -> MealPlanErrorState(state.error!!, onRetry = viewModel::load)
            else -> MealPlanContent(state = state, onPlanClick = onPlanClick, viewModel = viewModel)
        }


    }

    if (state.showCreateDialog) {
        CreatePlanDialog(
            name         = state.createName,
            nameError    = state.createNameError,
            isCreating   = state.isCreating,
            onNameChange = viewModel::onCreateNameChange,
            onConfirm    = viewModel::createPlan,
            onDismiss    = viewModel::hideCreateDialog
        )
    }
}

// ── Content ───────────────────────────────────────────────────────────────────
@Composable
private fun MealPlanContent(
    state: MealPlanUiState,
    onPlanClick: (MealPlan) -> Unit,
    viewModel: MealPlanViewModel
) {
    if (state.plans.isEmpty()) {
        MealPlanEmptyState()
        return
    }

    val activePlan = state.plans.firstOrNull { it.isActive }
    val otherPlans = state.plans.filter { !it.isActive }
    var planToDelete by remember { mutableStateOf<MealPlan?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (activePlan != null) {
            item { SectionLabel("Active plan") }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                ActivePlanCard(
                    plan     = activePlan,
                    onClick  = { onPlanClick(activePlan) },
                    onDelete = { planToDelete = activePlan }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        if (otherPlans.isNotEmpty()) {
            item { SectionLabel("Other plans") }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                // List container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        otherPlans.forEachIndexed { index, plan ->
                            PlanListRow(
                                plan        = plan,
                                onClick     = { onPlanClick(plan) },
                                onSetActive = { viewModel.setActive(plan) },
                                onDelete    = { planToDelete = plan }
                            )
                            if (index < otherPlans.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = CKSurfaceVariant,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(88.dp)) }
    }
    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    null,
                    tint = Color(0xFFBA1A1A),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Delete meal plan?", fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${plan.name}\"? This action cannot be undone.",
                    color = CKOnSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlan(plan)
                        planToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel", color = CKOnSurfaceVariant)
                }
            }
        )
    }
}

// ── Active plan card ──────────────────────────────────────────────────────────
@Composable
private fun ActivePlanCard(plan: MealPlan, onClick: () -> Unit, onDelete: () -> Unit) {
    val totalSlots    = plan.days.values.sumOf { it.size }
    val daysWithSlots = plan.days.values.count { it.isNotEmpty() }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Primary, Color(0xFF004D1F))),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = CKSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CKSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(plan.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, null, tint = OnPrimary.copy(alpha = 0.5f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Week mini strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Weekday.entries.forEach { day ->
                        val hasSlots = plan.days[day]?.isNotEmpty() == true
                        WeekdayPill(day = day.shortName, active = hasSlots)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StatChip(Icons.Default.Restaurant, "$totalSlots slots")
                    StatChip(Icons.Default.CalendarToday, "$daysWithSlots days planned")
                }
            }
        }
    }
}

@Composable
private fun WeekdayPill(day: String, active: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(day, fontSize = 10.sp, color = OnPrimary.copy(alpha = if (active) 1f else 0.45f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (active) CKSecondary else OnPrimary.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = OnPrimary.copy(alpha = 0.65f), modifier = Modifier.size(13.dp))
        Text(label, fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.75f))
    }
}

// ── Inactive plan list row ────────────────────────────────────────────────────
@Composable
private fun PlanListRow(
    plan: MealPlan,
    onClick: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    val totalSlots = plan.days.values.sumOf { it.size }
    val daysWithSlots = plan.days.values.count { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Leading icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CKSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Primary, modifier = Modifier.size(18.dp))
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(plan.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSurface)
            Text(
                "$totalSlots slots · $daysWithSlots days",
                fontSize = 12.sp,
                color = CKOnSurfaceVariant
            )
        }

        // Activate icon
        IconButton(onClick = onSetActive, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.StarBorder,
                contentDescription = "Activate plan",
                tint = CKOutlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Delete icon
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = CKOutlineVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = CKOnSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// ── Create plan dialog ────────────────────────────────────────────────────────
@Composable
private fun CreatePlanDialog(
    name: String,
    nameError: String?,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("New weekly plan", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OnSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Give your meal plan a name.", fontSize = 13.sp, color = CKOnSurfaceVariant)
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Plan name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor  = Primary,
                        cursorColor        = Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating,
                colors  = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary, strokeWidth = 2.dp)
                else Text("Create", color = OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CKOnSurfaceVariant) }
        }
    )
}

// ── Empty / Error states ──────────────────────────────────────────────────────
@Composable
private fun MealPlanEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(CKSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = Primary, modifier = Modifier.size(36.dp))
            }
            Text("No meal plans yet", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text("Create your first weekly plan\nby tapping the + button", fontSize = 13.sp, color = CKOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MealPlanErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.WifiOff, null, tint = CKOutlineVariant, modifier = Modifier.size(48.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Retry")
            }
        }
    }
}
