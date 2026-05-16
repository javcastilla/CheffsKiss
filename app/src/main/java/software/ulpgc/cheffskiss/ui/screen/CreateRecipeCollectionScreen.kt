package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
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
import software.ulpgc.cheffskiss.ui.RecipeCollectionViewModel
import software.ulpgc.cheffskiss.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeCollectionScreen(
    viewModel: RecipeCollectionViewModel,
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navega cuando la creación termina con éxito
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isCreating) {
        if (submitted && !uiState.isCreating && uiState.error == null) {
            onCreateSuccess()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onCreateImageChange(it.toString()) }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            CRTopBar(
                title       = "New Collection",
                onBack      = onBack,
                onSaveDraft = onBack
            )
        },
        bottomBar = {
            CCBottomBar(
                onCreate = {
                    viewModel.createCollection()   // valida internamente y setea createNameError
                    if (uiState.createName.isNotBlank()) submitted = true
                },
                isLoading      = uiState.isCreating,
                isFormComplete = uiState.createName.isNotBlank()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Cover Photo ───────────────────────────────────────────────────
            CCCoverPhotoCard(
                imageUri = uiState.createImage.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                onClick  = { galleryLauncher.launch("image/*") }
            )

            // ── Collection Details ────────────────────────────────────────────
            CRCard(icon = Icons.Default.CollectionsBookmark, title = "Collection Details") {

                CRFieldWithIcon(
                    icon  = Icons.Default.DriveFileRenameOutline,
                    label = "Collection Name"
                ) {
                    CRTextField(
                        value         = uiState.createName,
                        onValueChange = { viewModel.onCreateNameChange(it) },
                        placeholder   = "e.g. Sunday Brunch Ideas",
                        singleLine    = true,
                        // ← elimina isError, CRTextField no lo tiene
                        leadingIcon   = {
                            Icon(
                                Icons.Default.Collections,
                                contentDescription = null,
                                tint     = if (uiState.createNameError != null) Color(0xFFBA1A1A)
                                else CKOutlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                if (uiState.createNameError != null) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint     = Color(0xFFBA1A1A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text       = uiState.createNameError!!,
                            fontSize   = 11.sp,
                            color      = Color(0xFFBA1A1A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (uiState.createImage.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CKSurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint     = CKOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Cover image is optional — you can add one later.",
                        fontSize = 12.sp,
                        color    = CKOnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Cover Photo Card ──────────────────────────────────────────────────────────

@Composable
private fun CCCoverPhotoCard(imageUri: Uri?, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().height(200.dp).clickable { onClick() },
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (imageUri != null) {
                AsyncImage(
                    model              = imageUri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.85f))
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Change photo", tint = Color.White,
                        modifier = Modifier.size(18.dp))
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(56.dp).background(CKSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Primary,
                            modifier = Modifier.size(26.dp))
                    }
                    Text("Add Collection Photo", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("High quality images perform better", fontSize = 12.sp,
                        color = CKOnSurfaceVariant)
                }
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
private fun CCBottomBar(
    onCreate: () -> Unit,
    isLoading: Boolean,
    isFormComplete: Boolean
) {
    Surface(
        color = Background.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = OnPrimary
            ),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            if (!isLoading && isFormComplete)
                                listOf(Primary, Color(0xFF004D1C))
                            else
                                listOf(Primary.copy(alpha = 0.4f), Color(0xFF004D1C).copy(alpha = 0.4f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = OnPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CollectionsBookmark, null, modifier = Modifier.size(18.dp))
                        Text("Create Collection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}