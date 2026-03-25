package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.CreateRecipeCommand
import software.ulpgc.cheffskiss.application.IngredientInputRow
import software.ulpgc.cheffskiss.application.RecipeInput
import software.ulpgc.cheffskiss.application.StepInputRow
import software.ulpgc.cheffskiss.domain.model.User
import software.ulpgc.cheffskiss.domain.model.Username
import software.ulpgc.cheffskiss.domain.model.store.RecipeLineStore
import software.ulpgc.cheffskiss.domain.model.store.RecipeStore
import software.ulpgc.cheffskiss.domain.model.store.StepStore
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

sealed class RecipeUiState {
    object Idle    : RecipeUiState()
    object Loading : RecipeUiState()
    object Success : RecipeUiState()
    data class Error(val message: String) : RecipeUiState()
}

class RecipeViewModel : ViewModel() {

    private val recipeStore     = RecipeStore()
    private val stepStore       = StepStore()
    private val recipeLineStore = RecipeLineStore()

    private val recipeService = FirebaseRecipeService(recipeStore, stepStore, recipeLineStore)

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = RecipeUiState.Idle }

    fun createRecipe(
        title: String,
        hours: String,
        minutes: String,
        steps: List<StepInputRow>,
        ingredientRows: List<IngredientInputRow> = emptyList(),
        tags: List<String>,
        image: String
    ) {
        android.util.Log.d("CK_RECIPE", "▶ createRecipe() — title='$title' hours='$hours' minutes='$minutes' steps=${steps.size}")

        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading

            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser == null) {
                android.util.Log.e("CK_RECIPE", "✗ No hay usuario autenticado")
                _uiState.value = RecipeUiState.Error("No hay sesión activa")
                return@launch
            }
            android.util.Log.d("CK_RECIPE", "✓ Usuario Firebase: ${firebaseUser.uid}")

            val userId = UUID.nameUUIDFromBytes(firebaseUser.uid.toByteArray())

            val user = User(
                id          = userId,
                image       = firebaseUser.photoUrl?.toString() ?: "",
                description = null,
                username    = Username(firebaseUser.displayName ?: firebaseUser.email ?: "chef")
            )

            val totalMinutes = ((hours.toIntOrNull() ?: 0) * 60 +
                    (minutes.toIntOrNull() ?: 0)).toLong()

            android.util.Log.d("CK_RECIPE", "✓ Duración: ${totalMinutes}min — steps: ${steps.map { it.description }}")

            val input = object : RecipeInput {
                override fun user()           = user
                override fun title()          = title
                override fun duration()       = totalMinutes.minutes
                override fun stepRows()       = steps
                override fun ingredientRows() = ingredientRows
                override fun tags()           = tags
                override fun image()          = image
            }

            runCatching {
                android.util.Log.d("CK_RECIPE", "✓ Lanzando CreateRecipeCommand...")
                CreateRecipeCommand(recipeService, input).execute()
                android.util.Log.d("CK_RECIPE", "✓ Command completado")
            }.fold(
                onSuccess = {
                    android.util.Log.d("CK_RECIPE", "✓ SUCCESS — receta guardada en Firebase")
                    _uiState.value = RecipeUiState.Success
                },
                onFailure = { e ->
                    android.util.Log.e("CK_RECIPE", "✗ FAILURE: ${e.message}", e)
                    _uiState.value = RecipeUiState.Error(e.message ?: "Error al publicar la receta")
                }
            )
        }
    }
}