package software.ulpgc.cheffskiss.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.Measurement
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

sealed class RecipeUiState {
    data object Idle : RecipeUiState()
    data object Loading : RecipeUiState()
    data object Success : RecipeUiState()
    data class Error(val message: String) : RecipeUiState()
}

class RecipeViewModel(
    private val recipeRepository: RecipeRepository = FirebaseRecipeService()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = RecipeUiState.Idle
    }

    fun createRecipe(
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredients: List<String>,
        steps: List<Step>,
        stepImageUris: List<Uri?>,
        tags: List<String>,
        imageUri: Uri?
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.value = RecipeUiState.Error("Recipe title is required")
            return
        }
        if (authorId.isBlank()) {
            _uiState.value = RecipeUiState.Error("You must be logged in to create a recipe")
            return
        }

        val validIngredients = ingredients.filter { line ->
            val parts = line.split("|")
            parts.size >= 3 && parts.last().isNotBlank()
        }

        val validSteps = steps.filter { it.description.isNotBlank() }

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            try {
                val h = hours.toLongOrNull() ?: 0L
                val m = minutes.toLongOrNull() ?: 0L
                val duration = (h * 60 + m).minutes

                val lines = validIngredients.map { line -> parseIngredientLine(line) }

                val recipe = Recipe(
                    id = UUID.randomUUID(),
                    author = authorId,
                    title = trimmedTitle,
                    description = description.trim(),
                    duration = duration,
                    tags = tags.filter { it.isNotBlank() },
                    image = "",
                    servings = servings
                )

                val uploadedSteps = validSteps.mapIndexed { index, step ->
                    val uri = stepImageUris.getOrNull(index)
                    step.copy(image = uri?.toString() ?: step.image)
                }

                recipeRepository.createRecipe(recipe, lines, uploadedSteps)
                _uiState.value = RecipeUiState.Success
            } catch (e: Exception) {
                _uiState.value = RecipeUiState.Error(e.message ?: "Unknown error creating recipe")
            }
        }
    }

    fun updateRecipe(
        recipeId: String,
        authorId: String,
        title: String,
        description: String,
        servings: Int,
        hours: String,
        minutes: String,
        ingredients: List<String>,
        steps: List<Step>,
        stepImageUris: List<Uri?>,
        tags: List<String>,
        imageUri: Uri?,
        existingImageUrl: String,
        createdAt: Instant
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.value = RecipeUiState.Error("Recipe title is required")
            return
        }

        val validIngredients = ingredients.filter { line ->
            val parts = line.split("|")
            parts.size >= 3 && parts.last().isNotBlank()
        }

        val validSteps = steps.filter { it.description.isNotBlank() }

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            try {
                val h = hours.toLongOrNull() ?: 0L
                val m = minutes.toLongOrNull() ?: 0L
                val duration = (h * 60 + m).minutes

                val lines = validIngredients.map { line -> parseIngredientLine(line) }

                val recipe = Recipe(
                    id = UUID.fromString(recipeId),
                    author = authorId,
                    title = trimmedTitle,
                    description = description.trim(),
                    duration = duration,
                    tags = tags.filter { it.isNotBlank() },
                    image = existingImageUrl,
                    servings = servings,
                    createdAt = createdAt
                )

                val uploadedSteps = validSteps.mapIndexed { index, step ->
                    val uri = stepImageUris.getOrNull(index)
                    step.copy(image = uri?.toString() ?: step.image)
                }

                recipeRepository.updateRecipe(recipe, lines, uploadedSteps)
                _uiState.value = RecipeUiState.Success
            } catch (e: Exception) {
                _uiState.value = RecipeUiState.Error(e.message ?: "Unknown error updating recipe")
            }
        }
    }

    private fun parseIngredientLine(line: String): RecipeLine {
        val parts = line.split(";", limit = 4)

        val amount = parts.getOrNull(0)?.toDoubleOrNull()?.toInt() ?: 1
        val measurement = parts.getOrNull(1)
            ?.uppercase()
            ?.let { runCatching { Measurement.valueOf(it) }.getOrNull() }
            ?: Measurement.UNIT

        val ingredientIdRaw = parts.getOrNull(2)?.trim()
            ?: throw IllegalArgumentException("Ingredient ID is missing")

        val ingredientId = runCatching { UUID.fromString(ingredientIdRaw) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid ingredient ID: $ingredientIdRaw")

        return RecipeLine(
            ingredientId = ingredientId,
            amount = amount,
            measurement = measurement
        )
    }
}
