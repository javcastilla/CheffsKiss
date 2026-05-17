package software.ulpgc.cheffskiss.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.UpdateRecipeCollectionCommand
import software.ulpgc.cheffskiss.application.control.UpdateRecipeCollectionInput
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.ImageStorage
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.application.services.GetRecipeCollectionQuery
import software.ulpgc.cheffskiss.application.services.ImagePersistence
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.ImageStorageFactory
import java.util.UUID

data class RecipePickerFormState(
    val isVisible: Boolean = false,
    val recipePickerQuery: String = "",
)

data class RecipeCollectionDetailUiState(
    val collection: RecipeCollection? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val recipePicker: RecipePickerFormState = RecipePickerFormState(),
    val recipeTitles: Map<String, String> = emptyMap(),
    val recipeDetails: Map<String, Recipe> = emptyMap(),
    val availableRecipes: List<Recipe> = emptyList(),
    val error: String? = null,
    val authorNames: Map<String, String> = emptyMap(),
    val saveCompleted: Boolean = false,
    val deleteCompleted: Boolean = false,
)

class RecipeCollectionDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val port: RecipeCollectionRepository = FirebaseRecipeCollectionService()
    private val recipeReader: RecipeReader = FirebaseRecipeReader()
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
    private val userNameReader: FirebaseUserNameReader = FirebaseUserNameReader()
    private val imageStorage: ImageStorage by lazy { ImageStorageFactory.create(getApplication()) }

    private val _uiState = MutableStateFlow(RecipeCollectionDetailUiState())
    val uiState: StateFlow<RecipeCollectionDetailUiState> = _uiState.asStateFlow()

    private val userUuid: UUID?
        get() = currentUserPort.getCurrentUser()
            ?.let { UUID.nameUUIDFromBytes(it.toByteArray()) }

    fun load(collectionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val uid = userUuid ?: run {
                _uiState.update { it.copy(isLoading = false, error = "No authenticated user") }
                return@launch
            }

            val collectionUuid = runCatching { UUID.fromString(collectionId) }.getOrElse {
                _uiState.update { it.copy(isLoading = false, error = "Invalid collection id") }
                return@launch
            }

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
        if (ids.isEmpty()) {
            _uiState.update { it.copy(recipeDetails = emptyMap(), recipeTitles = emptyMap()) }
            return
        }
        viewModelScope.launch {
            val recipes = ids.mapNotNull { id -> recipeReader.getById(id) }
            val titles = recipes.associate { it.id.toString() to it.title }
            val details = recipes.associate { it.id.toString() to it }

            _uiState.update {
                it.copy(
                    recipeTitles = titles,
                    recipeDetails = details,
                )
            }

            recipes.map { it.creator.id.toString() }.distinct().forEach { authorId ->
                if (!_uiState.value.authorNames.containsKey(authorId)) {
                    viewModelScope.launch {
                        val name = userNameReader.getUsernameByUid(authorId)
                        if (!name.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(authorNames = it.authorNames + (authorId to name))
                            }
                        }
                    }
                }
            }
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

    fun updateMetadata(newName: String, newImage: String) {
        val collection = uiState.value.collection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val imageUrl = ImagePersistence.persistIfLocal(
                    imageStorage = imageStorage,
                    source = newImage,
                    folder = collection.id.toString(),
                    fileName = "cover.jpg",
                )
                persistToFirestore(collection.copy(name = newName, image = imageUrl))
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    private fun save(updated: RecipeCollection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { persistToFirestore(updated) }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    private suspend fun persistToFirestore(updated: RecipeCollection) {
        UpdateRecipeCollectionCommand(
            port = port,
            input = object : UpdateRecipeCollectionInput {
                override fun id() = updated.id
                override fun userId() = updated.userId
                override fun name() = updated.name
                override fun image() = updated.image
                override fun createdAt() = updated.createdAt
                override fun recipes() = updated.recipes
            },
        ).execute()
        _uiState.update {
            it.copy(
                collection = updated,
                isSaving = false,
                saveCompleted = true,
                recipePicker = RecipePickerFormState(),
            )
        }
        resolveRecipeTitles(updated)
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}
