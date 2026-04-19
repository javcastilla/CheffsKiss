package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import software.ulpgc.cheffskiss.application.port.IngredientRepository
import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseIngredientRepository

@OptIn(FlowPreview::class)
class IngredientViewModel(
    private val repository: IngredientRepository = FirebaseIngredientRepository()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _suggestions = MutableStateFlow<List<Ingredient>>(emptyList())

    val suggestions: StateFlow<List<Ingredient>> = _suggestions.asStateFlow()

    init {
        _query
            .debounce(250)
            .distinctUntilChanged()
            .onEach { q ->
                _suggestions.value = if (q.isBlank()) emptyList() else repository.search(q)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
        _query.value = ""
    }
}
