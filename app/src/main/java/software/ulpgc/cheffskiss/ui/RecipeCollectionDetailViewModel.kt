package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.UpdateRecipeCollectionCommand
import software.ulpgc.cheffskiss.application.control.UpdateRecipeCollectionInput
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.application.services.GetRecipeCollectionQuery
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import java.util.UUID

data class RecipePickerFormState(
    val isVisible: Boolean = false,
    val recipePickerQuery: String = ""
)

data class RecipeCollectionDetailUiState(
    val collection: RecipeCollection? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val recipePicker: RecipePickerFormState = RecipePickerFormState(),
    val recipeTitles: Map<String, String> = emptyMap(),   // recipeId → title (igual que MealPlan)
    val availableRecipes: List<Recipe> = emptyList(),
    val error: String? = null
)

class RecipeCollectionDetailViewModel(
    private val port: RecipeCollectionRepository = FirebaseRecipeCollectionService(),
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeCollectionDetailUiState())
    val uiState: StateFlow<RecipeCollectionDetailUiState> = _uiState.asStateFlow()

    private val userUuid: UUID?
        get() = currentUserPort.getCurrentUser()
            ?.let { UUID.nameUUIDFromBytes(it.toByteArray()) }


    fun load(collectionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val uid = userUuid ?: run {
                _uiState.update { it.copy(isLoading = false, error = "No authenticated user") }
                return@launch
            }

            val collectionUuid = UUID.fromString(collectionId)

            GetRecipeCollectionQuery(port)(uid)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { collections ->
                    val collection = collections.firstOrNull { it.id == collectionUuid }
                    _uiState.update { it.copy(collection = collection, isLoading = false) }
                    if (collection != null) resolveRecipeTitles(collection)
                }
        }
        loadAvailableRecipes()
    }

    private fun loadAvailableRecipes() {
        viewModelScope.launch {
            GetAllRecipesQuery(recipeReader)()
                .catch { }
                .collect { recipes ->
                    _uiState.update { it.copy(availableRecipes = recipes) }
                }
        }
    }

    private fun resolveRecipeTitles(collection: RecipeCollection) {
        val ids = collection.recipes.map { it.toString() }.toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val titles = ids.associateWith { id ->
                recipeReader.getById(id)?.title ?: ""
            }
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
        }
    }


    fun openRecipePicker() =
        _uiState.update { it.copy(recipePicker = it.recipePicker.copy(isVisible = true)) }

    fun closeRecipePicker() =
        _uiState.update { it.copy(recipePicker = RecipePickerFormState()) }

    fun onRecipePickerQueryChange(query: String) =
        _uiState.update { it.copy(recipePicker = it.recipePicker.copy(recipePickerQuery = query)) }


    fun addRecipe(recipe: Recipe) {
        val collection = _uiState.value.collection ?: return
        if (recipe.id in collection.recipes) return
        val updated = collection.copy(recipes = collection.recipes + recipe.id)
        save(updated)
        closeRecipePicker()
    }

    fun removeRecipe(recipeId: UUID) {
        val collection = _uiState.value.collection ?: return
        val updated = collection.copy(recipes = collection.recipes - recipeId)
        save(updated)
    }


    private fun save(updated: RecipeCollection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            UpdateRecipeCollectionCommand(
                port  = port,
                input = object : UpdateRecipeCollectionInput {
                    override fun id()        = updated.id
                    override fun userId()    = updated.userId
                    override fun name()      = updated.name
                    override fun image()     = updated.image
                    override fun createdAt() = updated.createdAt
                    override fun recipes()   = updated.recipes
                }
            ).execute()
            _uiState.update { it.copy(collection = updated, isSaving = false, recipePicker = RecipePickerFormState()) }
        }
    }
}