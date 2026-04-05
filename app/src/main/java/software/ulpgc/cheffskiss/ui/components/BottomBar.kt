package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.OnPrimary
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@Composable
fun CheffsBottomBar(
    currentRoute: String,
    onGoHome: () -> Unit,
    onGoLibrary: () -> Unit,
    onCreateRecipe: () -> Unit,
    onGoSaved: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onGoHome,
            icon = { androidx.compose.material3.Icon(Icons.Default.Home, null) },
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
            selected = currentRoute == "library",
            onClick = onGoLibrary,
            icon = { androidx.compose.material3.Icon(Icons.Default.MenuBook, null) },
            label = { Text("Library", fontSize = 11.sp) },
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
            onClick = onCreateRecipe,
            icon = { androidx.compose.material3.Icon(Icons.Default.AddCircle, null) },
            label = { Text("Create", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                indicatorColor = Primary.copy(alpha = 0.1f),
                unselectedIconColor = CKOutlineVariant,
                unselectedTextColor = CKOutlineVariant
            )
        )

        NavigationBarItem(
            selected = currentRoute == "saved",
            onClick = onGoSaved,
            icon = { androidx.compose.material3.Icon(Icons.Default.Bookmark, null) },
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