package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.components.CheffsBottomBar
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.OnBackground
import software.ulpgc.cheffskiss.ui.theme.Primary

@Composable
fun LibraryScreen(
    onGoHome: () -> Unit,
    onCreateRecipe: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        bottomBar = {
            CheffsBottomBar(
                currentRoute = "library",
                onGoHome = onGoHome,
                onGoLibrary = {},
                onCreateRecipe = onCreateRecipe
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Library",
                    color = Primary,
                    fontSize = 24.sp
                )
                Text(
                    text = "Pantalla provisional para probar navegación y barra inferior.",
                    color = OnBackground
                )
            }
        }
    }
}