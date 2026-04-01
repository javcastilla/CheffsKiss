package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader

data class HomeUiState(
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val getAllRecipesQuery: GetAllRecipesQuery = GetAllRecipesQuery(FirebaseRecipeReader())
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val userNameReader  = FirebaseUserNameReader()
    private val _authorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val authorNames: StateFlow<Map<String, String>> = _authorNames.asStateFlow()

    init {
        observeRecipes()
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            getAllRecipesQuery()
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
                .catch { e ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        error = e.message ?: "Error al cargar recetas"
                    )
                }
                .collect { list ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        recipes = list,
                        error = null
                    )
                    resolveAllAuthors(list)

                }
        }
    }
    private fun resolveAllAuthors(recipes: List<Recipe>) {
        recipes.map { it.author }.distinct().forEach { uid ->
            if (!_authorNames.value.containsKey(uid)) {
                viewModelScope.launch {
                    val name = userNameReader.getUsernameByUid(uid)
                    if (name != null) {
                        _authorNames.update { it + (uid to name) }
                    }
                }
            }
        }
    }}
