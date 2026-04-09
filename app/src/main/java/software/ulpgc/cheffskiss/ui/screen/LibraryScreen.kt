package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.components.HomeBottomBar
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnBackground
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Outline
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    authViewModel: AuthenticantionViewModel,
    onGoHome: () -> Unit,
    onCreateRecipe: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currentUid = authViewModel.getCurrentUid()
    val recipes = state.recipes.filter { it.author == currentUid }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            HomeBottomBar(
                currentRoute = "library",
                onHomeClick = onGoHome,
                onCreateClick = onCreateRecipe,
                onSavedClick = {}
            )
        }
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Error al cargar recetas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { }) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold
        }

        val currentUid = authViewModel.getCurrentUid()
        val recipes = state.recipes.filter { recipe ->
            recipe.author == currentUid
        }

        if (recipes.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Tu librería está vacía",
                        color = Primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aquí aparecerán tus recetas y las guardadas.",
                        color = OnBackground
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Library",
                    color = Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tus recetas creadas y guardadas.",
                    color = CKOnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(recipes) { recipe ->
                LibraryRecipeCard(
                    recipe = recipe,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryRecipeCard(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CKSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Por ${recipe.author}",
                    fontSize = 12.sp,
                    color = CKOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("${recipe.duration.inWholeMinutes}m", fontSize = 11.sp, color = Outline)
                    }

                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Outline, modifier = Modifier.size(12.dp))
                        Text("${recipe.servings} servings", fontSize = 11.sp, color = Outline)
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CKOutlineVariant
            )
        }
    }
}