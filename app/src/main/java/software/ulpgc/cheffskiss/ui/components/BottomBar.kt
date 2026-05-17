package software.ulpgc.cheffskiss.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.navigation.MainBottomNavigation
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.CKSecondary
import software.ulpgc.cheffskiss.ui.theme.OnPrimary
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface
import software.ulpgc.cheffskiss.ui.theme.SurfaceVariant

@Composable
fun AppBottomBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
    createExpanded: Boolean,
    onCreateToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavTab(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = currentRoute == MainBottomNavigation.HOME,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f),
                )
                BottomNavTab(
                    icon = Icons.Default.Explore,
                    label = "Explore",
                    selected = currentRoute == MainBottomNavigation.EXPLORE,
                    onClick = onExploreClick,
                    modifier = Modifier.weight(1f),
                )
                BottomNavTab(
                    icon = Icons.Default.MenuBook,
                    label = "Library",
                    selected = currentRoute == MainBottomNavigation.LIBRARY,
                    onClick = onLibraryClick,
                    modifier = Modifier.weight(1f),
                )
                BottomNavTab(
                    icon = Icons.Default.Groups,
                    label = "Profile",
                    selected = currentRoute == MainBottomNavigation.PROFILE,
                    onClick = onProfileClick,
                    modifier = Modifier.weight(1f),
                )
                CreateNavButton(
                    expanded = createExpanded,
                    onClick = onCreateToggle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
}

/** @deprecated Use [AppBottomBar] */
@Composable
fun HomeBottomBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSavedClick: () -> Unit,
) {
    AppBottomBar(
        currentRoute = currentRoute,
        onHomeClick = onHomeClick,
        onExploreClick = onExploreClick,
        onLibraryClick = onSavedClick,
        onProfileClick = {},
        createExpanded = false,
        onCreateToggle = onCreateClick,
    )
}

@Composable
private fun BottomNavTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) Primary else CKOutlineVariant,
        animationSpec = tween(180),
        label = "nav_tint",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(if (selected) 26.dp else 24.dp),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
        )
    }
}

@Composable
private fun CreateNavButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(if (expanded) 220 else 160),
        label = "create_rotate",
    )
    val bgColor by animateColorAsState(
        targetValue = if (expanded) Primary else SurfaceVariant,
        animationSpec = tween(if (expanded) 220 else 160),
        label = "create_bg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (expanded) OnPrimary else Primary,
        animationSpec = tween(if (expanded) 220 else 160),
        label = "create_icon",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Create",
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
            )
        }
        Text(
            text = "Create",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (expanded) CKSecondary else Primary,
        )
    }
}
