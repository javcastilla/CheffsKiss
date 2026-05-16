package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import software.ulpgc.cheffskiss.ui.RecipeCollectionDetailViewModel
import software.ulpgc.cheffskiss.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeCollectionScreen(
    collectionId: String,
    viewModel: RecipeCollectionDetailViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember(state.collection) {
        mutableStateOf(state.collection?.name ?: "")
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(collectionId) {
        viewModel.load(collectionId)
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.resetSaveState()
            onSaveSuccess()  // aquí sí existe como parámetro
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { imageUri = it } }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Collection", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(Modifier.size(36.dp).background(Surface, CircleShape), Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background.copy(alpha = 0.95f))
            )
        },
        bottomBar = {
            Surface(color = Background.copy(alpha = 0.95f), tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (name.isBlank()) { nameError = "Collection name cannot be empty"; return@Button }
                        val imageString = imageUri?.toString() ?: state.collection?.image ?: ""
                        viewModel.updateMetadata(name.trim(), imageString)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = OnPrimary),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            brush = Brush.linearGradient(
                                if (!state.isSaving) listOf(Primary, Color(0xFF004D1C))
                                else listOf(Primary.copy(0.4f), Color(0xFF004D1C).copy(0.4f))
                            ), shape = CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isSaving) CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading || state.collection == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverPhotoCard(
                imageUri = imageUri,
                existingUrl = state.collection!!.image.takeIf { it.isNotBlank() },
                onClick = { galleryLauncher.launch("image/*") }
            )

            CRCard(icon = Icons.Default.CollectionsBookmark, title = "Collection Details") {
                CRFieldWithIcon(icon = Icons.Default.DriveFileRenameOutline, label = "Collection Name") {
                    CRTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        placeholder = "e.g. Sunday Brunch Ideas",
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Collections, null,
                                tint = if (nameError != null) Color(0xFFBA1A1A) else CKOutlineVariant,
                                modifier = Modifier.size(18.dp))
                        }
                    )
                    if (nameError != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 4.dp)) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(14.dp))
                            Text(nameError!!, fontSize = 11.sp, color = Color(0xFFBA1A1A), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}