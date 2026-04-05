package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.output.CurrentUserPort
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService

data class LibraryUiState(
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val error: String? = null
)

class LibraryViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeMyRecipes()
    }

    private fun observeMyRecipes() {
        val currentUid = currentUserPort.getCurrentUser()

        if (currentUid == null) {
            _uiState.value = LibraryUiState(
                isLoading = false,
                error = "No hay usuario autenticado"
            )
            return
        }

        viewModelScope.launch {
            recipeReader.getByAuthor(currentUid)
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                .catch { e ->
                    _uiState.value = LibraryUiState(
                        isLoading = false,
                        error = e.message ?: "Error al cargar tus recetas"
                    )
                }
                .collect { recipes ->
                    _uiState.value = LibraryUiState(
                        isLoading = false,
                        recipes = recipes,
                        error = null
                    )
                }
        }
    }
}