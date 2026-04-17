package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.SaveRecipeCommand
import software.ulpgc.cheffskiss.application.SaveRecipeInput
import software.ulpgc.cheffskiss.application.UnsaveRecipeCommand
import software.ulpgc.cheffskiss.application.port.output.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService

class RecipeDetailViewModel(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipePort: RecipePort = FirebaseRecipeService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val userNameReader = FirebaseUserNameReader()

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val currentUid: String? get() = currentUserPort.getCurrentUser()

    fun load(recipeId: String) {
        viewModelScope.launch {
            val r = recipeReader.getById(recipeId) ?: return@launch
            _recipe.value = r
            _isOwner.value = (currentUid == r.author)
            val name = userNameReader.getUsernameByUid(r.author)
            _authorName.value = if (name.isNullOrBlank()) "Unknown" else name
            observeIsSaved(recipeId)
        }
    }

    private fun observeIsSaved(recipeId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            GetSavedRecipesQuery(recipePort)(uid)
                .catch { }
                .collect { saved ->
                    _isSaved.value = saved.any { it.recipeId.toString() == recipeId }
                }
        }
    }

    fun toggleSave() {
        if (_isOwner.value) return
        val r = _recipe.value ?: return
        val uid = currentUid ?: return
        val currentlySaved = _isSaved.value

        _isSaved.value = !currentlySaved // optimistic

        viewModelScope.launch {
            runCatching {
                if (currentlySaved) {
                    UnsaveRecipeCommand(recipePort, uid, r.id).execute()
                } else {
                    SaveRecipeCommand(recipePort, object : SaveRecipeInput {
                        override fun recipeId() = r.id
                        override fun userId()   = uid
                    }).execute()
                }
            }.onFailure {
                _isSaved.value = currentlySaved // rollback
            }
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        val r = _recipe.value ?: return
        viewModelScope.launch {
            runCatching { recipePort.deleteRecipe(r.id.toString()) }
                .onSuccess { onDone() }
        }
    }
}
