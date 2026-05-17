package software.ulpgc.cheffskiss.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.control.CreateRecipeCommand
import software.ulpgc.cheffskiss.application.control.RecipeInput
import software.ulpgc.cheffskiss.application.control.UpdateRecipeCommand
import software.ulpgc.cheffskiss.application.port.ImageStorage
import software.ulpgc.cheffskiss.application.services.IngredientCatalogService
import software.ulpgc.cheffskiss.application.services.IngredientDraft
import software.ulpgc.cheffskiss.application.services.RecipeIngredientService
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.user.User
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
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

    private val recipeService = FirebaseRecipeService()
    private val recipeReader  = FirebaseRecipeReader()
    private val imageStorage: ImageStorage = LocalImageStorage(application)
    private val ingredientCatalog = IngredientCatalogService(recipeReader)
    private val ingredientService = RecipeIngredientService(recipeReader, ingredientCatalog)

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _ingredientCatalog = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredientCatalogState = _ingredientCatalog.asStateFlow()

    private val _ingredientCatalogLoading = MutableStateFlow(false)
    val ingredientCatalogLoading = _ingredientCatalogLoading.asStateFlow()

    fun resetState() { _uiState.value = RecipeUiState.Idle }

    fun loadIngredientCatalog() {
        if (_ingredientCatalog.value.isNotEmpty() || _ingredientCatalogLoading.value) return
        viewModelScope.launch {
            _ingredientCatalogLoading.value = true
            runCatching { ingredientCatalog.loadCatalog() }
                .onSuccess { _ingredientCatalog.value = it }
                .onFailure { e ->
                    _uiState.value = RecipeUiState.Error(
                        e.message ?: "Could not load ingredients catalog"
                    )
                }
            _ingredientCatalogLoading.value = false
        }
    }

    fun filterIngredients(query: String): List<Ingredient> =
        ingredientCatalog.filterCatalog(_ingredientCatalog.value, query)

    fun createRecipe(
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredientDrafts: List<IngredientDraft>,
        steps: List<Step>,
        stepImageUris: List<Uri?> = emptyList(),
        tags: List<String>,
        imageUri: Uri?,
    ) {
        when {
            title.isBlank() -> {
                _uiState.value = RecipeUiState.Error("The title must be filled.")
                return
            }
            servings < 1 -> {
                _uiState.value = RecipeUiState.Error("Indicate the number of servings.")
                return
            }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") -> {
                _uiState.value = RecipeUiState.Error("Indicate the recipe duration.")
                return
            }
            ingredientDrafts.none { it.ingredientId != null } -> {
                _uiState.value = RecipeUiState.Error("Select at least one ingredient from the catalog.")
                return
            }
            steps.isEmpty() -> {
                _uiState.value = RecipeUiState.Error("There must be at least one step.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            runCatching {
                val recipeId = UUID.randomUUID()
                val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)).toLong()
                val coverUrl = imageUri?.let { imageStorage.save(it, recipeId.toString(), "cover.jpg") } ?: ""
                val lines = ingredientService.resolveLines(ingredientDrafts)
                val stepsWithMedia = attachStepImages(recipeId.toString(), steps, stepImageUris)

                val input = object : RecipeInput {
                    override fun id() = recipeId
                    override fun title() = title
                    override fun description() = description.trim()
                    override fun servings() = servings
                    override fun duration() = totalMinutes.minutes
                    override fun lines() = lines
                    override fun steps() = stepsWithMedia
                    override fun tags() = tags
                    override fun image() = coverUrl
                    override fun creator() = User(UserIds.creatorIdFromFirebaseUid(authorId))
                }
                CreateRecipeCommand(recipeService, input).execute()
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e -> _uiState.value = RecipeUiState.Error(e.message ?: "Error publishing the recipe") },
            )
        }
    }

    fun updateRecipe(
        recipeId: UUID,
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredientDrafts: List<IngredientDraft>,
        steps: List<Step>,
        stepImageUris: List<Uri?> = emptyList(),
        tags: List<String>,
        imageUri: Uri?,
        existingImageUrl: String = "",
        createdAt: Instant,
        currentVersion: Int,
    ) {
        when {
            title.isBlank() -> { _uiState.value = RecipeUiState.Error("The title have to be filled"); return }
            servings < 1 -> { _uiState.value = RecipeUiState.Error("Indicate the number of servings."); return }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") ->
                { _uiState.value = RecipeUiState.Error("Indicate the recipe duration"); return }
            ingredientDrafts.none { it.ingredientId != null } ->
                { _uiState.value = RecipeUiState.Error("Select at least one ingredient from the catalog"); return }
            steps.isEmpty() -> { _uiState.value = RecipeUiState.Error("Add at least one step"); return }
        }

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            runCatching {
                val existing = recipeReader.getById(recipeId.toString())
                    ?: error("Recipe not found")
                val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)).toLong()
                val coverUrl = if (imageUri != null) {
                    imageStorage.save(imageUri, recipeId.toString(), "cover.jpg")
                } else existingImageUrl
                val lines = ingredientService.resolveLines(ingredientDrafts)
                val stepsWithMedia = attachStepImages(recipeId.toString(), steps, stepImageUris)

                val input = object : RecipeInput {
                    override fun id() = recipeId
                    override fun title() = title
                    override fun description() = description.trim()
                    override fun servings() = servings
                    override fun duration() = totalMinutes.minutes
                    override fun lines() = lines
                    override fun steps() = stepsWithMedia
                    override fun tags() = tags
                    override fun image() = coverUrl
                    override fun creator() = existing.creator
                }

                UpdateRecipeCommand(
                    recipeRepository = recipeService,
                    current = existing.copy(version = currentVersion),
                    recipeInput = input,
                ).execute()
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e -> _uiState.value = RecipeUiState.Error(e.message ?: "Error updating recipe") },
            )
        }
    }

    private suspend fun attachStepImages(
        recipeFolder: String,
        steps: List<Step>,
        imageUris: List<Uri?>,
    ): List<Step> = steps.mapIndexed { index, step ->
        val uri = imageUris.getOrNull(index)
        val imageUrl = when {
            uri != null -> imageStorage.save(uri, recipeFolder, "step_${step.id}.jpg")
            else -> step.imageUrl
        }
        step.withImageUrl(imageUrl?.takeIf { it.isNotBlank() })
    }
}
