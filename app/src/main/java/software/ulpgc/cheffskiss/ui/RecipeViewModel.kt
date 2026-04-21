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
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.Measurement
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
import software.ulpgc.cheffskiss.application.port.ImageStorage
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

    private val recipeService: FirebaseRecipeService = FirebaseRecipeService()
    private val imageStorage: ImageStorage       = LocalImageStorage(application)

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = RecipeUiState.Idle }

    // ── Text → RecipeLine ─────────────────────────────────────────────────────
    // "200 KILO harina" → RecipeLine(ingredientId = nameUUID("harina"), amount=200, KILO)
    // Si no hay cantidad/unidad reconocibles, amount=1, UNIT

    private fun parseLines(ingredients: List<String>): List<RecipeLine> =
        ingredients.map { raw ->
            val parts = raw.trim().split("\\s+".toRegex())
            val amount = parts.getOrNull(0)?.toIntOrNull()
            val measurement = parts.getOrNull(1)
                ?.uppercase()
                ?.let { runCatching { Measurement.valueOf(it) }.getOrNull() }
            val nameTokens = when {
                amount != null && measurement != null -> parts.drop(2)
                amount != null                        -> parts.drop(1)
                else                                  -> parts
            }
            val name = nameTokens.joinToString(" ").ifBlank { raw.trim() }
            RecipeLine(
                ingredientId = UUID.nameUUIDFromBytes(name.lowercase().toByteArray()),
                amount       = amount ?: 1,
                measurement  = measurement ?: Measurement.UNIT
            )
        }

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
            title.isBlank() -> {
                _uiState.value = RecipeUiState.Error("The title must be filled.")
                return
            }
            description.isBlank() -> {
                _uiState.value = RecipeUiState.Error("The description must be filled.")
                return
            }
            (hours.isBlank() || hours == "0") && (minutes.isBlank() || minutes == "0") -> {
                _uiState.value = RecipeUiState.Error("Indicate the recipe duration.")
                return
            }
            ingredients.isEmpty() -> {
                _uiState.value = RecipeUiState.Error("There must be at least one ingredient.")
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
                val recipeId     = UUID.randomUUID()
                val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)).toLong()

                val coverUrl = imageUri?.let {
                    imageStorage.save(it, recipeId.toString(), "cover.jpg")
                } ?: ""

                val stepsWithImages = steps.mapIndexed { idx, step ->
                    val uri = stepImageUris.getOrNull(idx)
                    val url = if (uri != null)
                        imageStorage.save(uri, recipeId.toString(), "step_$idx.jpg")
                    else step.image
                    step.copy(image = url)
                }

                val lines = parseLines(ingredients)

                val input = object : RecipeInput {
                    override fun id()          = recipeId
                    override fun author()      = authorId
                    override fun title()       = title
                    override fun description() = description
                    override fun servings()    = servings
                    override fun duration()    = totalMinutes.minutes
                    override fun lines()       = lines
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

                val coverUrl = if (imageUri != null)
                    imageStorage.save(imageUri, recipeId.toString(), "cover.jpg")
                else existingImageUrl

                val stepsWithImages = steps.mapIndexed { idx, step ->
                    val uri = stepImageUris.getOrNull(idx)
                    val url = if (uri != null)
                        imageStorage.save(uri, recipeId.toString(), "step_$idx.jpg")
                    else step.image
                    step.copy(image = url)
                }

                val lines = parseLines(ingredients)

                val recipe = Recipe(
                    id          = recipeId,
                    author      = authorId,
                    title       = title,
                    description = description,
                    servings    = servings,
                    duration    = totalMinutes.minutes,
                    tags        = tags,
                    image       = coverUrl,
                    createdAt   = createdAt
                )
                recipeService.updateRecipe(recipe, lines, stepsWithImages)
            }.fold(
                onSuccess = { _uiState.value = RecipeUiState.Success },
                onFailure = { e -> _uiState.value = RecipeUiState.Error(e.message ?: "Error updating recipe") }
            )
        }
    }
}