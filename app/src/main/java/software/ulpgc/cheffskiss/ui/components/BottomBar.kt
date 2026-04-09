package software.ulpgc.cheffskiss.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@Composable
fun HomeBottomBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSavedClick: () -> Unit
) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                indicatorColor = Primary.copy(alpha = 0.1f),
                unselectedIconColor = CKOutlineVariant,
                unselectedTextColor = CKOutlineVariant
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("Explore", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = CKOutlineVariant,
                unselectedTextColor = CKOutlineVariant
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = onCreateClick,
            icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
            label = { Text("Create", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = CKOutlineVariant,
                unselectedTextColor = CKOutlineVariant
            )
        )

        NavigationBarItem(
            selected = currentRoute == "library",
            onClick = onSavedClick,
            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Saved") },
            label = { Text("Saved", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                indicatorColor = Primary.copy(alpha = 0.1f),
                unselectedIconColor = CKOutlineVariant,
                unselectedTextColor = CKOutlineVariant
            )
        )
    }
}