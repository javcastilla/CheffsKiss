package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.ui.theme.*

// â”€â”€ Data models â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class IngredientRow(
    val id: Int,
    val ingredientId: java.util.UUID? = null,
    val name: String = "",
    val amount: String = "",
    val unit: String = "UNIT",
)

data class StepRow(
    val id: Int,
    val description: String = "",
    val duration: String = "",
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null
)

val unitOptions = listOf("UNIT", "GRAM", "KG", "ML", "LITRE", "CUP", "TBSP", "TSP", "SLICE", "PINCH")

// â”€â”€ Top Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CRTopBar(
    title: String = "New Recipe",
    onBack: () -> Unit,
    onSaveDraft: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier.size(36.dp).background(Surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        },
        actions = { IconButton(onClick = onSaveDraft) {} },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background.copy(alpha = 0.95f))
    )
}

// â”€â”€ Bottom Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRBottomBar(
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    isLoading: Boolean,
    isPublishFormComplete: Boolean,
    publishLabel: String = "Publish Recipe"
) {
    Surface(
        color = Background.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier.height(52.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.4f)),
                colors = outlinedButtonColors(contentColor = CKOnSurfaceVariant, containerColor = Color.Transparent)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Draft", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Button(
                onClick = onPublish,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = CircleShape,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPublishFormComplete) Primary else Primary.copy(alpha = 0.4f),
                    contentColor = OnPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Publish, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(publishLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// â”€â”€ Cover Photo Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CoverPhotoCard(
    imageUri: Uri?,
    existingUrl: String? = null,
    onClick: () -> Unit
) {
    val showExisting = imageUri == null && !existingUrl.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(CKSurfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri != null -> AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            showExisting -> AsyncImage(
                model = existingUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(CKSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Primary, modifier = Modifier.size(26.dp))
                }
                Text("Add Recipe Photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("High quality images perform better", fontSize = 12.sp, color = CKOnSurfaceVariant)
            }
        }
        if (imageUri != null || showExisting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Background.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = OnSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// â”€â”€ Section Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            content()
        }
    }
}

// â”€â”€ Field with Icon â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRFieldWithIcon(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CKOnSurfaceVariant)
        content()
    }
}

// â”€â”€ Text Field â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    fillWidth: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = CKOutlineVariant) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        shape = if (singleLine) CircleShape else RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Background,
            unfocusedContainerColor = Background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface
        )
    )
}

// â”€â”€ Small Circle Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun SmallCircleButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).background(Background, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = OnSurface, modifier = Modifier.size(16.dp))
    }
}

// â”€â”€ Tag Chip â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(Primary, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
        Icon(
            Icons.Default.Close,
            null,
            tint = OnPrimary.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove)
        )
    }
}

// â”€â”€ Dashed Add Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun DashedAddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(2.dp, CKOutlineVariant.copy(alpha = 0.4f)),
        colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
    ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// â”€â”€ Step Item â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun CRStepItem(
    number: Int,
    isFirst: Boolean,
    step: StepRow,
    onChange: (StepRow) -> Unit,
    onRemove: () -> Unit
) {
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onChange(step.copy(imageUri = it)) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (isFirst) Primary else Background, CircleShape)
                .border(if (isFirst) 0.dp else 2.dp, CKOutlineVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFirst) OnPrimary else CKOutlineVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f).background(Background, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = step.description,
                onValueChange = { onChange(step.copy(description = it)) },
                placeholder = { Text("Describe this step...", fontSize = 13.sp, color = CKOutlineVariant) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface
                )
            )

            val displayImage: Any? = step.imageUri ?: step.existingImageUrl?.takeIf { it.isNotBlank() }
            if (displayImage != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(Background.copy(alpha = 0.8f), CircleShape)
                            .clickable { onChange(step.copy(imageUri = null, existingImageUrl = null)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CKOutlineVariant, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CKOutlineVariant.copy(alpha = 0.3f)),
                    colors = outlinedButtonColors(contentColor = CKOutlineVariant, containerColor = Color.Transparent)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add photo", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Timer, null, tint = CKOutlineVariant, modifier = Modifier.size(14.dp))
                    BasicTextField(
                        value = step.duration,
                        onValueChange = { onChange(step.copy(duration = it)) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = OnSurface),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(48.dp),
                        decorationBox = { inner ->
                            Box {
                                if (step.duration.isEmpty()) Text("0", fontSize = 12.sp, color = CKOutlineVariant)
                                inner()
                            }
                        }
                    )
                    Text("min", fontSize = 12.sp, color = CKOutlineVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.RemoveCircleOutline, null, tint = CKOutlineVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
// â”€â”€ EditRecipeScreen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// AÃ±adir al final de EditRecipeScreen.kt

@OptIn(ExperimentalMaterial3Api::class)

// ── EditRecipeScreen ──────────────────────────────────────────────────────────

@Composable
fun EditRecipeScreen(
    recipe: software.ulpgc.cheffskiss.domain.model.recipe.Recipe,
    initialLines: List<software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine>,
    initialSteps: List<Step>,
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit,
) {
    RecipeFormScreen(
        mode = RecipeFormMode.Edit(recipe, initialLines, initialSteps),
        onBack = onBack,
        onSuccess = onUpdateSuccess,
        onSaveDraft = onBack,
    )
}
