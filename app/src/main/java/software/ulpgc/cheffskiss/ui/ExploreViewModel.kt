package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.application.services.UserDisplayService
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import java.util.UUID

data class ExploreUiState(
    val isLoading: Boolean = true,
    val availableTags: List<String> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val authorNames: Map<String, String> = emptyMap(),
    val error: String? = null
) {
    val hasActiveFilters get() = searchQuery.isNotBlank() || selectedTags.isNotEmpty()
    val resultCount get() = filteredRecipes.size
}

private data class ExploreFilterState(val recipes: List<Recipe>, val query: String, val tags: Set<String>)
private data class ExploreMetaState(val authors: Map<String, String>, val loading: Boolean, val error: String?)

class ExploreViewModel(
    private val getAllRecipesQuery: GetAllRecipesQuery = GetAllRecipesQuery(FirebaseRecipeReader()),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val userDisplayService = UserDisplayService()

    private val _allRecipes   = MutableStateFlow<List<Recipe>>(emptyList())
    private val _searchQuery  = MutableStateFlow("")
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    private val _authorNames  = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _isLoading    = MutableStateFlow(true)
    private val _error        = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExploreUiState> = combine(
        combine(_allRecipes, _searchQuery, _selectedTags, ::ExploreFilterState),
        combine(_authorNames, _isLoading, _error, ::ExploreMetaState)
    ) { filter, meta ->
        val filtered = if (!meta.loading && meta.error == null) {
            filter.recipes.filter { recipe ->
                val matchesName = filter.query.isBlank() || recipe.title.contains(filter.query, ignoreCase = true)
                val matchesTags = filter.tags.isEmpty() || filter.tags.all { tag ->
                    recipe.tags.any { it.equals(tag, ignoreCase = true) }
                }
                matchesName && matchesTags
            }
        } else emptyList()

        ExploreUiState(
            isLoading       = meta.loading,
            availableTags   = filter.recipes.flatMap { it.tags }.distinct().sorted(),
            filteredRecipes = filtered,
            searchQuery     = filter.query,
            selectedTags    = filter.tags,
            authorNames     = meta.authors,
            error           = meta.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    init { load() }

    fun load() {
        viewModelScope.launch {
            getAllRecipesQuery()
                .onStart { _isLoading.value = true; _error.value = null }
                .catch { e -> _error.value = e.message ?: "Error loading recipes"; _isLoading.value = false }
                .collect { recipes ->
                    val currentUid = currentUserPort.getCurrentUser()
                    val currentCreatorId = currentUid?.let { UserIds.creatorIdFromFirebaseUid(it) }
                    val filtered = recipes.filter { it.creator.id != currentCreatorId }
                    _allRecipes.value = filtered
                    _isLoading.value  = false
                    resolveAuthors(filtered)
                }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun toggleTag(tag: String) {
        _selectedTags.update { current ->
            if (tag in current) current - tag else current + tag
        }
    }

    fun clearFilters() {
        _searchQuery.value  = ""
        _selectedTags.value = emptySet()
    }

    private fun resolveAuthors(recipes: List<Recipe>) {
        recipes.map { it.creator.id.toString() }.distinct().forEach { uid ->
            if (!_authorNames.value.containsKey(uid)) {
                viewModelScope.launch {
                    val name = userDisplayService.displayNameFor(UUID.fromString(uid))
                    if (name.isNotBlank()) _authorNames.update { it + (uid to name) }
                }
            }
        }
    }
}
