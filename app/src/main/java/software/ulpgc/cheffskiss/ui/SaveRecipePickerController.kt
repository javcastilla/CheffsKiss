package software.ulpgc.cheffskiss.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.services.RecipeLibraryService
import software.ulpgc.cheffskiss.domain.model.RecipeLibraryDestination
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import java.util.UUID

data class SaveRecipePickerUiState(
    val visible: Boolean = false,
    val recipeTitle: String = "",
    val recipeId: UUID? = null,
    val collections: List<software.ulpgc.cheffskiss.domain.model.RecipeCollection> = emptyList(),
    val selected: RecipeLibraryDestination? = null,
    val isInSaved: Boolean = false,
    val collectionIdsContainingRecipe: Set<UUID> = emptySet(),
    val isWorking: Boolean = false,
    val resultMessage: String? = null,
)

class SaveRecipePickerController(
    private val libraryService: RecipeLibraryService,
    private val currentUserPort: CurrentUserPort,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(SaveRecipePickerUiState())
    val state: StateFlow<SaveRecipePickerUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun open(recipe: Recipe) {
        val uid = currentUserPort.getCurrentUser() ?: return
        observeJob?.cancel()
        _state.value = SaveRecipePickerUiState(
            visible = true,
            recipeTitle = recipe.title,
            recipeId = recipe.id,
            selected = RecipeLibraryDestination.Saved,
        )
        observeJob = scope.launch {
            libraryService.observePickerContext(recipe.id, uid).collect { context ->
                _state.update { current ->
                    current.copy(
                        collections = context.collections,
                        isInSaved = context.isInSaved,
                        collectionIdsContainingRecipe = context.collectionIdsContainingRecipe,
                        selected = current.selected ?: RecipeLibraryDestination.Saved,
                    )
                }
            }
        }
    }

    fun close() {
        observeJob?.cancel()
        _state.value = SaveRecipePickerUiState()
    }

    fun selectDestination(destination: RecipeLibraryDestination) {
        _state.update { it.copy(selected = destination) }
    }

    fun confirm() {
        val snapshot = _state.value
        val recipeId = snapshot.recipeId ?: return
        val destination = snapshot.selected ?: return
        val uid = currentUserPort.getCurrentUser() ?: return
        val alreadyPresent = isRecipeInDestination(snapshot, destination)

        scope.launch {
            _state.update { it.copy(isWorking = true) }
            val result = runCatching {
                if (alreadyPresent) {
                    libraryService.removeRecipeFrom(destination, recipeId, uid).message
                } else {
                    libraryService.addRecipeTo(destination, recipeId, uid).message
                }
            }
            result.fold(
                onSuccess = { message ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            visible = false,
                            resultMessage = message,
                        )
                    }
                    observeJob?.cancel()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            resultMessage = error.message ?: "Could not update list",
                        )
                    }
                },
            )
        }
    }

    private fun isRecipeInDestination(
        state: SaveRecipePickerUiState,
        destination: RecipeLibraryDestination,
    ): Boolean = when (destination) {
        RecipeLibraryDestination.Saved -> state.isInSaved
        is RecipeLibraryDestination.Collection ->
            destination.collectionId in state.collectionIdsContainingRecipe
    }

    fun consumeMessage() {
        _state.update { it.copy(resultMessage = null) }
    }
}
