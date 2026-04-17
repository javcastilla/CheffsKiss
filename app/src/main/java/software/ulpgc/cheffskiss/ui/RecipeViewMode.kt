package software.ulpgc.cheffskiss.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.CreateRecipeCommand
import software.ulpgc.cheffskiss.application.RecipeInput
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.application.port.output.ImageStoragePort
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.LocalImageStorage
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

sealed class RecipeUiState {
    object Idle    : RecipeUiState()
    object Loading : RecipeUiState()
    object Success : RecipeUiState()
    data class Error(val message: String) : RecipeUiState()
}

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeService : FirebaseRecipeService = FirebaseRecipeService()
    private val imageStorage  : ImageStoragePort      = LocalImageStorage(application)

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = RecipeUiState.Idle }

    // ── Create ────────────────────────────────────────────────────────────────

    fun createRecipe(
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredients: List<String>,
        steps: List<Step>,
        stepImageUris: List<Uri?> = emptyList(),
        tags: List<String>,
        imageUri: Uri?
    ) {
        when {
            title.isBlank()       -> { _uiState.value = RecipeUiState.Error("The title have to be filled"); return }
            description.isBlank() -> { _uiState.value = RecipeUiState.Error("The description have to be filled"); return }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") ->
                { _uiState.value = RecipeUiState.Error("Indicate the recipe duration"); return }
            ingredients.isEmpty() -> { _uiState.value = RecipeUiState.Error("The ingredients have to be filled with at least one ingredient"); return }
            steps.isEmpty()       -> { _uiState.value = RecipeUiState.Error("The steps have to be filled with at least one step"); return }
        }

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            runCatching {
                val recipeId     = UUID.randomUUID()
                val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)).toLong()

                // Upload cover image
                val coverUrl = imageUri?.let {
                    imageStorage.save(it, recipeId.toString(), "cover.jpg")
                } ?: ""

                // Upload step images
                val stepsWithImages = steps.mapIndexed { idx, step ->
                    val uri = stepImageUris.getOrNull(idx)
                    val url = if (uri != null) imageStorage.save(uri, recipeId.toString(), "step_$idx.jpg")
                              else step.image
                    step.copy(image = url)
                }

                val input = object : RecipeInput {
                    override fun id()          = recipeId
                    override fun author()      = authorId
                    override fun title()       = title
                    override fun description() = description
                    override fun servings()    = servings
                    override fun duration()    = totalMinutes.minutes
                    override fun ingredients() = ingredients
                    override fun steps()       = stepsWithImages
                    override fun tags()        = tags
                    override fun image()       = coverUrl
                }
                CreateRecipeCommand(recipeService, input).execute()
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e -> _uiState.value = RecipeUiState.Error(e.message ?: "Error publishing the recipe") }
            )
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    fun updateRecipe(
        recipeId: UUID,
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredients: List<String>,
        steps: List<Step>,
        stepImageUris: List<Uri?> = emptyList(),
        tags: List<String>,
        imageUri: Uri?,
        existingImageUrl: String = "",
        createdAt: Instant
    ) {
        when {
            title.isBlank()       -> { _uiState.value = RecipeUiState.Error("The title have to be filled"); return }
            description.isBlank() -> { _uiState.value = RecipeUiState.Error("The description have to be filled"); return }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") ->
                { _uiState.value = RecipeUiState.Error("Indicate the recipe duration"); return }
            ingredients.isEmpty() -> { _uiState.value = RecipeUiState.Error("Add at least one ingredient"); return }
            steps.isEmpty()       -> { _uiState.value = RecipeUiState.Error("Add at least one step"); return }
        }
        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            runCatching {
                val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)).toLong()

                // Upload new cover if selected, otherwise keep existing
                val coverUrl = if (imageUri != null) {
                    imageStorage.save(imageUri, recipeId.toString(), "cover.jpg")
                } else existingImageUrl

                // Upload new step images, keep existing URLs for unchanged steps
                val stepsWithImages = steps.mapIndexed { idx, step ->
                    val uri = stepImageUris.getOrNull(idx)
                    val url = if (uri != null) imageStorage.save(uri, recipeId.toString(), "step_$idx.jpg")
                              else step.image
                    step.copy(image = url)
                }

                val updated = Recipe(
                    id = recipeId, author = authorId, title = title, description = description,
                    servings = servings, duration = totalMinutes.minutes, ingredients = ingredients,
                    steps = stepsWithImages, tags = tags, image = coverUrl, createdAt = createdAt
                )
                recipeService.updateRecipe(updated)
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e -> _uiState.value = RecipeUiState.Error(e.message ?: "Error updating recipe") }
            )
        }
    }
}
