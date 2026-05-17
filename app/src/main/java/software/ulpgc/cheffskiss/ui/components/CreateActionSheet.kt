package software.ulpgc.cheffskiss.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@Composable
fun CreateActionSheetOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreateRecipe: () -> Unit,
    onCreateMealPlan: () -> Unit,
    onCreateList: () -> Unit,
) {
    val openMs = 220
    val closeMs = 160

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(openMs)),
            exit = fadeOut(tween(closeMs)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Primary.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(openMs)) + slideInVertically(tween(openMs)) { it / 4 } +
                    scaleIn(tween(openMs), initialScale = 0.96f),
                exit = fadeOut(tween(closeMs)) + slideOutVertically(tween(closeMs)) { it / 6 } +
                    scaleOut(tween(closeMs), targetScale = 0.98f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Crear",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Primary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                    )
                    CreateActionRow(
                        icon = Icons.Default.Restaurant,
                        title = "Recipe",
                        subtitle = "Publish a new recipe",
                        onClick = onCreateRecipe,
                    )
                    HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
                    CreateActionRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "Meal Plan",
                        subtitle = "Plan your week",
                        onClick = onCreateMealPlan,
                    )
                    HorizontalDivider(color = CKSurfaceVariant, thickness = 0.5.dp)
                    CreateActionRow(
                        icon = Icons.Default.MenuBook,
                        title = "List",
                        subtitle = "Organize recipes in a collection",
                        onClick = onCreateList,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CKSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnSurface)
            Text(subtitle, fontSize = 12.sp, color = CKOnSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = CKOutlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
