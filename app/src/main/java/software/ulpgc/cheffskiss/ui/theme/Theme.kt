// ✅ Theme.kt correcto
package software.ulpgc.cheffskiss.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CheffsColorScheme = lightColorScheme(
    primary          = CKPrimary,
    onPrimary        = CKOnPrimary,
    secondary        = CKSecondary,
    onSecondary      = CKOnSecondary,
    background       = CKBackground,
    onBackground     = CKOnBackground,
    surface          = CKSurface,
    onSurface        = CKOnSurface,
    surfaceVariant   = CKSurfaceVariant,
    onSurfaceVariant = CKOnSurfaceVariant,
    outline          = CKOutline,
    outlineVariant   = CKOutlineVariant

)

@Composable
fun CheffsKissTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CheffsColorScheme,
        typography = Typography,
        content = content
    )
}
