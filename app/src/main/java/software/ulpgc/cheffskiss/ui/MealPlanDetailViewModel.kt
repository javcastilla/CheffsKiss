package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.MealSlot
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.vo.SlotTime
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import java.util.UUID

data class SlotFormState(
    val isVisible: Boolean = false,
    val editingSlotId: UUID? = null,
    val name: String = "",
    val nameError: String? = null,
    val startTime: String = "",
    val endTime: String = "",
    val timeError: String? = null,
    val colorIndex: Int = 0,
    val selectedRecipeId: UUID? = null,
    val selectedRecipeTitle: String = "",
    val isRecipePickerVisible: Boolean = false,
    val recipePickerQuery: String = ""
)

data class MealPlanDetailUiState(
    val plan: MealPlan? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val selectedDay: Weekday = Weekday.MONDAY,
    val slotForm: SlotFormState = SlotFormState(),
    val recipeTitles: Map<String, String> = emptyMap(),
    val availableRecipes: List<Recipe> = emptyList(),
    val error: String? = null
)

class MealPlanDetailViewModel(
    private val mealPlanRepository: MealPlanRepository = FirebaseMealPlanService(),
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanDetailUiState())
    val uiState: StateFlow<MealPlanDetailUiState> = _uiState.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val planUuid = UUID.fromString(planId)
            val uid = currentUserPort.getCurrentUser() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "No authenticated user") }
                return@launch
            }
            val userUuid = UUID.nameUUIDFromBytes(uid.toByteArray())
            mealPlanRepository.getMealPlans(userUuid)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { plans ->
                    val plan = plans.firstOrNull { it.id == planUuid }
                    _uiState.update { it.copy(plan = plan, isLoading = false) }
                    if (plan != null) {
                        resolveRecipeTitles(plan)
                        loadAvailableRecipes()
                    }
                }
        }
    }

    private fun loadAvailableRecipes() {
        viewModelScope.launch {
            recipeReader.getAll()
                .catch { }
                .collect { recipes -> _uiState.update { it.copy(availableRecipes = recipes) } }
        }
    }

    private fun resolveRecipeTitles(plan: MealPlan) {
        val ids = plan.days.values.flatten()
            .mapNotNull { it.recipeId?.toString() }
            .toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val titles = ids.associateWith { id -> recipeReader.getById(id)?.title ?: "" }
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
        }
    }

    fun selectDay(day: Weekday) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun toggleActive() {
        val plan = _uiState.value.plan ?: return
        val updated = if (plan.isActive) plan.deactivate() else plan.activate()
        save(updated)
    }

    fun openAddSlot() {
        _uiState.update { it.copy(slotForm = SlotFormState(isVisible = true)) }
    }

    fun openEditSlot(slot: MealSlot) {
        _uiState.update {
            it.copy(
                slotForm = SlotFormState(
                    isVisible = true,
                    editingSlotId = slot.id,
                    name = slot.name,
                    startTime = slot.startTime.toString(),
                    endTime = slot.endTime.toString(),
                    colorIndex = slot.colorIndex,
                    selectedRecipeId = slot.recipeId,
                    selectedRecipeTitle = slot.recipeId?.toString()
                        ?.let { id -> it.recipeTitles[id] } ?: ""
                )
            )
        }
    }

    fun closeSlotForm() {
        _uiState.update { it.copy(slotForm = SlotFormState()) }
    }

    fun onSlotNameChange(name: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(name = name, nameError = null)) }
    }

    fun onSlotStartTimeChange(time: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(startTime = time, timeError = null)) }
    }

    fun onSlotEndTimeChange(time: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(endTime = time, timeError = null)) }
    }

    fun onSlotColorChange(index: Int) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(colorIndex = index)) }
    }

    fun openRecipePicker() {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = true)) }
    }

    fun closeRecipePicker() {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = false)) }
    }

    fun onRecipePickerQueryChange(query: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(recipePickerQuery = query)) }
    }

    fun selectRecipe(recipe: Recipe?) {
        _uiState.update {
            it.copy(
                slotForm = it.slotForm.copy(
                    selectedRecipeId = recipe?.id,
                    selectedRecipeTitle = recipe?.title ?: "",
                    isRecipePickerVisible = false,
                    recipePickerQuery = ""
                )
            )
        }
    }

    fun saveSlot() {
        val form = _uiState.value.slotForm
        val plan = _uiState.value.plan ?: return

        if (form.name.isBlank()) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(nameError = "Name is required")) }
            return
        }

        val start = SlotTime.fromHHmm(form.startTime)
        val end = SlotTime.fromHHmm(form.endTime)

        if (start >= end) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(timeError = "Invalid time range")) }
            return
        }

        val day = _uiState.value.selectedDay
        val currentSlots = plan.days[day] ?: emptyList()

        val newSlot = MealSlot(
            id = form.editingSlotId ?: UUID.randomUUID(),
            name = form.name,
            startTime = start,
            endTime = end,
            colorIndex = form.colorIndex,
            recipeId = form.selectedRecipeId
        )

        val updatedSlots = if (form.editingSlotId != null) {
            currentSlots.map { if (it.id == form.editingSlotId) newSlot else it }
        } else {
            currentSlots + newSlot
        }

        val updatedPlan = plan.copy(days = plan.days + (day to updatedSlots))
        save(updatedPlan)
    }

    fun deleteSlot(slot: MealSlot) {
        val plan = _uiState.value.plan ?: return
        val day = _uiState.value.selectedDay
        val updatedSlots = (plan.days[day] ?: emptyList()).filter { it.id != slot.id }
        val updatedPlan = plan.copy(days = plan.days + (day to updatedSlots))
        save(updatedPlan)
    }

    private fun save(plan: MealPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            mealPlanRepository.updateMealPlan(plan)
            _uiState.update { it.copy(plan = plan, isSaving = false, slotForm = SlotFormState()) }
        }
    }
}