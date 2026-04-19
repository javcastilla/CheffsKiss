package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService

data class LibraryUiState(
    val isLoading: Boolean = true,
    val myRecipes: List<Recipe> = emptyList(),
    val savedRecipes: List<Recipe> = emptyList(),
    val authorNames: Map<String, String> = emptyMap(),
    val error: String? = null
)

class LibraryViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val userNameReader = FirebaseUserNameReader()

    init { load() }

    fun load() {
        val uid = currentUserPort.getCurrentUser()
        if (uid == null) {
            _uiState.value = LibraryUiState(isLoading = false, error = "No authenticated user")
            return
        }
        observeMyRecipes(uid)
        observeSavedRecipes(uid)
    }

    private fun observeMyRecipes(uid: String) {
        viewModelScope.launch {
            recipeReader.getByAuthor(uid)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error loading your recipes") }
                }
                .collect { recipes ->
                    _uiState.update { it.copy(myRecipes = recipes, isLoading = false) }
                    resolveAuthors(recipes)
                }
        }
    }

    private fun observeSavedRecipes(uid: String) {
        viewModelScope.launch {
            GetSavedRecipesQuery(recipeRepository)(uid)
                .catch { /* no bloquear si falla saved */ }
                .collect { savedList ->
                    val recipes = coroutineScope {
                        savedList.map { saved ->
                            async { recipeReader.getById(saved.recipeId.toString()) }
                        }.awaitAll().filterNotNull()
                    }
                    _uiState.update { it.copy(savedRecipes = recipes) }
                    resolveAuthors(recipes)
                }
        }
    }

    private fun resolveAuthors(recipes: List<Recipe>) {
        recipes.map { it.author }.distinct().forEach { authorId ->
            if (!_uiState.value.authorNames.containsKey(authorId)) {
                viewModelScope.launch {
                    val name = userNameReader.getUsernameByUid(authorId)
                    if (!name.isNullOrBlank()) {
                        _uiState.update { it.copy(authorNames = it.authorNames + (authorId to name)) }
                    }
                }
            }
        }
    }
}
