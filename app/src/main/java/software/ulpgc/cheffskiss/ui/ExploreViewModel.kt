package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.services.ExploreSearchMode
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.application.services.IngredientCatalogService
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
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
    val searchMode: ExploreSearchMode = ExploreSearchMode.BY_TITLE,
    val selectedIngredientIds: Set<UUID> = emptySet(),
    val ingredientCatalog: List<Ingredient> = emptyList(),
    val ingredientCatalogLoading: Boolean = false,
    val selectedTags: Set<String> = emptySet(),
    val authorNames: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    val hasActiveFilters: Boolean
        get() = when (searchMode) {
            ExploreSearchMode.BY_TITLE -> searchQuery.isNotBlank() || selectedTags.isNotEmpty()
            ExploreSearchMode.BY_INGREDIENTS ->
                selectedIngredientIds.isNotEmpty() || selectedTags.isNotEmpty()
        }

    val resultCount get() = filteredRecipes.size
}

private data class ExploreFilterState(
    val recipes: List<Recipe>,
    val query: String,
    val searchMode: ExploreSearchMode,
    val selectedIngredientIds: Set<UUID>,
    val tags: Set<String>,
)

private data class ExploreMetaState(
    val authors: Map<String, String>,
    val loading: Boolean,
    val error: String?,
    val ingredientCatalog: List<Ingredient>,
    val ingredientCatalogLoading: Boolean,
)

class ExploreViewModel(
    private val getAllRecipesQuery: GetAllRecipesQuery = GetAllRecipesQuery(FirebaseRecipeReader()),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService(),
    private val ingredientCatalogService: IngredientCatalogService = IngredientCatalogService(FirebaseRecipeReader()),
) : ViewModel() {

    private val userDisplayService = UserDisplayService()

    private val _allRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _searchMode = MutableStateFlow(ExploreSearchMode.BY_TITLE)
    private val _selectedIngredientIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _ingredientCatalog = MutableStateFlow<List<Ingredient>>(emptyList())
    private val _ingredientCatalogLoading = MutableStateFlow(false)
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    private val _authorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExploreUiState> = combine(
        combine(
            _allRecipes,
            _searchQuery,
            _searchMode,
            _selectedIngredientIds,
            _selectedTags,
            ::ExploreFilterState,
        ),
        combine(
            _authorNames,
            _isLoading,
            _error,
            _ingredientCatalog,
            _ingredientCatalogLoading,
            ::ExploreMetaState,
        ),
    ) { filter, meta ->
        val filtered = if (!meta.loading && meta.error == null) {
            filter.recipes.filter { recipe ->
                val matchesSearch = when (filter.searchMode) {
                    ExploreSearchMode.BY_TITLE ->
                        filter.query.isBlank() || recipe.title.contains(filter.query, ignoreCase = true)
                    ExploreSearchMode.BY_INGREDIENTS ->
                        filter.selectedIngredientIds.isEmpty() ||
                            ingredientCatalogService.recipeContainsAllIngredients(
                                recipe,
                                filter.selectedIngredientIds,
                            )
                }
                val matchesTags = filter.tags.isEmpty() || filter.tags.all { tag ->
                    recipe.tags.any { it.equals(tag, ignoreCase = true) }
                }
                matchesSearch && matchesTags
            }
        } else {
            emptyList()
        }

        ExploreUiState(
            isLoading = meta.loading,
            availableTags = filter.recipes.flatMap { it.tags }.distinct().sorted(),
            filteredRecipes = filtered,
            searchQuery = filter.query,
            searchMode = filter.searchMode,
            selectedIngredientIds = filter.selectedIngredientIds,
            ingredientCatalog = meta.ingredientCatalog,
            ingredientCatalogLoading = meta.ingredientCatalogLoading,
            selectedTags = filter.tags,
            authorNames = meta.authors,
            error = meta.error,
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
                    _isLoading.value = false
                    resolveAuthors(filtered)
                }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onSearchModeChange(mode: ExploreSearchMode) {
        _searchMode.value = mode
        if (mode == ExploreSearchMode.BY_INGREDIENTS) {
            _searchQuery.value = ""
            loadIngredientCatalog()
        } else {
            _selectedIngredientIds.value = emptySet()
        }
    }

    fun onSelectedIngredientsChange(ids: Set<UUID>) {
        _selectedIngredientIds.value = ids
    }

    fun loadIngredientCatalog() {
        if (_ingredientCatalog.value.isNotEmpty() || _ingredientCatalogLoading.value) return
        viewModelScope.launch {
            _ingredientCatalogLoading.value = true
            runCatching { ingredientCatalogService.loadCatalog() }
                .onSuccess { _ingredientCatalog.value = it }
            _ingredientCatalogLoading.value = false
        }
    }

    fun toggleTag(tag: String) {
        _selectedTags.update { current ->
            if (tag in current) current - tag else current + tag
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptySet()
        _selectedIngredientIds.value = emptySet()
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
