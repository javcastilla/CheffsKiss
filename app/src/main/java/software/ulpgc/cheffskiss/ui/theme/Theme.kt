// ✅ Theme.kt correcto
package software.ulpgc.cheffskiss.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CheffsColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    secondary        = Secondary,
    onSecondary      = OnSecondary,
    background       = Background,
    onBackground     = OnBackground,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline          = Outline,
    outlineVariant   = OutlineVariant
)

@Composable
fun CheffsKissTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CheffsColorScheme,
        typography = Typography,
        content = content
    )
}
