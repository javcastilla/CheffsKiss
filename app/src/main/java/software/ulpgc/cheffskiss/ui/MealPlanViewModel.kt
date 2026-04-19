package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.CreateMealPlanCommand
import software.ulpgc.cheffskiss.application.control.DeleteMealPlanCommand
import software.ulpgc.cheffskiss.application.control.SetActiveMealPlanCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.application.services.GetMealPlansQuery
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import java.util.UUID

data class MealPlanUiState(
    val isLoading: Boolean = true,
    val plans: List<MealPlan> = emptyList(),
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createNameError: String? = null,
    val isCreating: Boolean = false
)

class MealPlanViewModel(
    private val port: MealPlanRepository = FirebaseMealPlanService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState())
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    private val userId: String? get() = currentUserPort.getCurrentUser()

    init { load() }

    fun load() {
        val uid = userId ?: run {
            _uiState.update { it.copy(isLoading = false, error = "No authenticated user") }
            return
        }
        val userUuid = UUID.nameUUIDFromBytes(uid.toByteArray())
        viewModelScope.launch {
            GetMealPlansQuery(port)(userUuid)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { plans ->
                    _uiState.update {
                        it.copy(isLoading = false, plans = plans.sortedByDescending { p -> p.isActive })
                    }
                }
        }
    }

    // ── Create dialog ─────────────────────────────────────────────────────────

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true, createName = "", createNameError = null) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }
    fun onCreateNameChange(name: String) = _uiState.update { it.copy(createName = name, createNameError = null) }

    fun createPlan() {
        val uid = userId ?: return
        val name = _uiState.value.createName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(createNameError = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            runCatching { CreateMealPlanCommand(port, uid, name).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun deletePlan(plan: MealPlan) {
        val uid = userId ?: return
        viewModelScope.launch {
            runCatching { DeleteMealPlanCommand(port, uid, plan.id).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun setActive(plan: MealPlan) {
        val uid = userId ?: return
        viewModelScope.launch {
            runCatching { SetActiveMealPlanCommand(port, uid, plan.id).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
