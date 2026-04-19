package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.domain.port.input.IngredientSearchPort
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseIngredientRepository

@OptIn(FlowPreview::class)
class IngredientSearchViewModel(
    private val repository: IngredientSearchPort = FirebaseIngredientRepository()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _suggestions = MutableStateFlow<List<Ingredient>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val suggestions: StateFlow<List<Ingredient>> = _suggestions.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(250)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        _isLoading.value = true
                        _suggestions.value = repository.searchByName(query)
                        _isLoading.value = false
                    } else {
                        _suggestions.value = emptyList()
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
        _query.value = ""
    }
}
