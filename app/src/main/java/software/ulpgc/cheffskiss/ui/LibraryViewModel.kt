package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.application.services.GetRecipeCollectionQuery
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.application.services.UserDisplayService
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import java.util.UUID

data class LibraryUiState(
    val isLoading: Boolean = true,
    val myRecipes: List<Recipe> = emptyList(),
    val savedRecipes: List<Recipe> = emptyList(),
    val collections: List<RecipeCollection> = emptyList(),
    val authorNames: Map<String, String> = emptyMap(),
    val error: String? = null
)

class LibraryViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService(),
    private val collectionPort: RecipeCollectionRepository = FirebaseRecipeCollectionService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val userDisplayService = UserDisplayService()

    init { load() }

    fun load() {
        val uid = currentUserPort.getCurrentUser()
        if (uid == null) {
            _uiState.value = LibraryUiState(isLoading = false, error = "No authenticated user")
            return
        }
        observeMyRecipes(uid)
        observeSavedRecipes(uid)
        loadCollections()
    }

    private fun observeMyRecipes(uid: String) {
        val authorUuid = UserIds.creatorIdStringFromFirebaseUid(uid)
        viewModelScope.launch {
            recipeReader.getByAuthor(authorUuid)
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
                .catch {  }
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
        recipes
            .mapNotNull { it.creator?.id?.toString() }
            .distinct()
            .forEach { authorId ->
                if (!_uiState.value.authorNames.containsKey(authorId)) {
                    viewModelScope.launch {
                        val name = userDisplayService.displayNameFor(UUID.fromString(authorId))
                        if (!name.isNullOrBlank()) {
                            _uiState.update { it.copy(authorNames = it.authorNames + (authorId to name)) }
                        }
                    }
                }
            }
    }
    private fun loadCollections() {
        val uid = currentUserPort.getCurrentUser()
            ?.let { UUID.nameUUIDFromBytes(it.toByteArray()) } ?: return
        viewModelScope.launch {
            GetRecipeCollectionQuery(collectionPort)(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { cols ->
                    _uiState.update { it.copy(collections = cols) }
                }
        }
    }
}
