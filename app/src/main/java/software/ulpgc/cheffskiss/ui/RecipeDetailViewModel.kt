package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.application.services.RecipeLibraryService
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.application.services.UserDisplayService
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import software.ulpgc.cheffskiss.domain.model.RecipeLibraryDestination
import java.util.UUID

class RecipeDetailViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val collectionRepository: RecipeCollectionRepository = FirebaseRecipeCollectionService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService(),
) : ViewModel() {

    private val firebaseReader = recipeReader as FirebaseRecipeReader
    private val userDisplayService = UserDisplayService()
    private val savePickerController = SaveRecipePickerController(
        libraryService = RecipeLibraryService(recipeRepository, collectionRepository),
        currentUserPort = currentUserPort,
        scope = viewModelScope,
    )

    private val _recipe     = MutableStateFlow<Recipe?>(null)
    private val _authorName = MutableStateFlow("")
    private val _isSaved    = MutableStateFlow(false)
    private val _isOwner    = MutableStateFlow(false)
    private val _lines      = MutableStateFlow<List<RecipeLine>>(emptyList())
    private val _steps      = MutableStateFlow<List<Step>>(emptyList())

    val recipe:     StateFlow<Recipe?>          = _recipe.asStateFlow()
    val authorName: StateFlow<String>           = _authorName.asStateFlow()
    val isSaved:    StateFlow<Boolean>          = _isSaved.asStateFlow()
    val isOwner:    StateFlow<Boolean>          = _isOwner.asStateFlow()
    val lines:      StateFlow<List<RecipeLine>> = _lines.asStateFlow()
    val steps:      StateFlow<List<Step>>       = _steps.asStateFlow()
    val savePickerState = savePickerController.state

    private val currentUid: String? get() = currentUserPort.getCurrentUser()

    fun load(recipeId: String) {
        viewModelScope.launch {
            val r = recipeReader.getById(recipeId) ?: return@launch
            _recipe.value = r
            val currentCreatorId = currentUid?.let { UserIds.creatorIdFromFirebaseUid(it) }
            _isOwner.value = currentCreatorId == r.creator.id

            val name = userDisplayService.displayNameFor(r.creator.id)
            _authorName.value = name.ifBlank { "Unknown" }

            launch {
                firebaseReader.linesOf(r)
                    .catch { }
                    .collect { _lines.value = it }
            }
            launch {
                firebaseReader.stepsOf(r)
                    .catch { }
                    .collect { _steps.value = it }
            }

            observeIsSaved(recipeId)
        }
    }

    private fun observeIsSaved(recipeId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            GetSavedRecipesQuery(recipeRepository)(uid)
                .catch { }
                .collect { saved -> _isSaved.value = saved.any { it.recipeId.toString() == recipeId } }
        }
    }

    fun openSavePicker() {
        if (_isOwner.value) return
        _recipe.value?.let { savePickerController.open(it) }
    }

    fun closeSavePicker() = savePickerController.close()

    fun selectSaveDestination(destination: RecipeLibraryDestination) =
        savePickerController.selectDestination(destination)

    fun confirmSaveToList() = savePickerController.confirm()

    fun consumeSavePickerMessage() = savePickerController.consumeMessage()

    fun deleteRecipe(onDone: () -> Unit) {
        val r = _recipe.value ?: return
        viewModelScope.launch {
            runCatching { recipeRepository.deleteRecipe(r.id.toString()) }
                .onSuccess { onDone() }
        }
    }
}