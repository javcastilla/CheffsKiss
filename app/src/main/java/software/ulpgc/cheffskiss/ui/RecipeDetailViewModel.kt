package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.SaveRecipeCommand
import software.ulpgc.cheffskiss.application.control.SaveRecipeInput
import software.ulpgc.cheffskiss.application.control.UnsaveRecipeCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService

class RecipeDetailViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val firebaseReader = recipeReader as FirebaseRecipeReader
    private val userNameReader = FirebaseUserNameReader()

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

    private val currentUid: String? get() = currentUserPort.getCurrentUser()

    fun load(recipeId: String) {
        viewModelScope.launch {
            val r = recipeReader.getById(recipeId) ?: return@launch
            _recipe.value = r
            _isOwner.value = currentUid == r.author

            val name = userNameReader.getUsernameByUid(r.author)
            _authorName.value = if (name.isNullOrBlank()) "Unknown" else name

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

    fun toggleSave() {
        if (_isOwner.value) return
        val r = _recipe.value ?: return
        val uid = currentUid ?: return
        val currentlySaved = _isSaved.value
        _isSaved.value = !currentlySaved
        viewModelScope.launch {
            runCatching {
                if (currentlySaved) {
                    UnsaveRecipeCommand(recipeRepository, uid, r.id).execute()
                } else {
                    SaveRecipeCommand(recipeRepository, object : SaveRecipeInput {
                        override fun recipeId() = r.id
                        override fun userId() = uid
                    }).execute()
                }
            }.onFailure { _isSaved.value = currentlySaved }
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        val r = _recipe.value ?: return
        viewModelScope.launch {
            runCatching { recipeRepository.deleteRecipe(r.id.toString()) }
                .onSuccess { onDone() }
        }
    }
}