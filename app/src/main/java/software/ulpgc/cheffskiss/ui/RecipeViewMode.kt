package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.CreateRecipeCommand
import software.ulpgc.cheffskiss.application.RecipeInput
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

sealed class RecipeUiState {
    object Idle : RecipeUiState()
    object Loading : RecipeUiState()
    object Success : RecipeUiState()
    data class Error(val message: String) : RecipeUiState()
}

class RecipeViewModel : ViewModel() {

    private val recipeService = FirebaseRecipeService()

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = RecipeUiState.Idle }

    fun createRecipe(
        authorId: UUID,
        title: String,
        description: String,
        hours: String,
        minutes: String,
        ingredients: List<String>,
        steps: List<Step>,
        tags: List<String>,
        image: String
    ) {
        when{
            title.isBlank() -> {
                _uiState.value = RecipeUiState.Error("The title have to be filled")
                return
            }
            description.isBlank() -> {
                _uiState.value = RecipeUiState.Error("The description have to be filled")
                return
            }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") -> {
                _uiState.value = RecipeUiState.Error("Indicate the recipe duration")
                return
            }            ingredients.isEmpty() -> {
                _uiState.value = RecipeUiState.Error("The ingredients have to be filled with at least one ingredient")
                return
            }
            steps.isEmpty() -> {
                _uiState.value = RecipeUiState.Error("The steps have to be filled with at least one step")
                return
            }
            hours.isBlank() && minutes.isBlank() -> {
                _uiState.value = RecipeUiState.Error("Indica la duración de la receta")
                return
            }


        }
        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading

            val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 +
                    (minutes.toIntOrNull() ?: 0)).toLong()

            val input = object : RecipeInput {
                override fun id() = UUID.randomUUID()
                override fun author() = authorId
                override fun title() = title
                override fun description() = description
                override fun duration() = totalMinutes.minutes
                override fun ingredients() = ingredients
                override fun steps() = steps
                override fun tags() = tags
                override fun image() = image
            }

            runCatching {
                CreateRecipeCommand(recipeService, input).execute()
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e ->
                    _uiState.value = RecipeUiState.Error(
                        e.message ?: "Error al publicar la receta"
                    )
                }
            )
        }
    }
}
