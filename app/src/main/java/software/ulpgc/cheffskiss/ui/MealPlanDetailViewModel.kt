package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.MealSlot
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.vo.SlotTime
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
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
    private val recipeReader: RecipeReader = FirebaseRecipeReader()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanDetailUiState())
    val uiState: StateFlow<MealPlanDetailUiState> = _uiState.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uuid = UUID.fromString(planId)
            // Derive userId from planId is not possible directly;
            // we load all plans and find ours.
            // Instead we subscribe to the flow and pick the matching plan.
            mealPlanRepository.getMealPlans(uuid).catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { plans ->
                val plan = plans.firstOrNull { it.id == uuid }
                    ?: plans.firstOrNull() // fallback: first plan if id matches userId
                _uiState.update { it.copy(plan = plan, isLoading = false) }
                if (plan != null) resolveRecipeTitles(plan)
            }
        }
        loadAvailableRecipes()
    }

    private fun loadAvailableRecipes() {
        viewModelScope.launch {
            recipeReader.getAll().catch { }.collect { recipes ->
                _uiState.update { it.copy(availableRecipes = recipes) }
            }
        }
    }

    private fun resolveRecipeTitles(plan: MealPlan) {
        val ids = plan.days.values.flatten()
            .mapNotNull { it.recipeId?.toString() }
            .toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val titles = ids.associateWith { id ->
                recipeReader.getById(id)?.title ?: ""
            }
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
        }
    }

    // ── Day selection ─────────────────────────────────────────────────────────

    fun selectDay(day: Weekday) = _uiState.update { it.copy(selectedDay = day) }

    // ── Active toggle ─────────────────────────────────────────────────────────

    fun toggleActive() {
        val plan = _uiState.value.plan ?: return
        val updated = if (plan.isActive) plan.deactivate() else plan.activate()
        save(updated)
    }

    // ── Slot form ─────────────────────────────────────────────────────────────

    fun openAddSlot() = _uiState.update {
        it.copy(slotForm = SlotFormState(isVisible = true))
    }

    fun openEditSlot(slot: MealSlot) = _uiState.update {
        it.copy(
            slotForm = SlotFormState(
                isVisible      = true,
                editingSlotId  = slot.id,
                name           = slot.name,
                startTime      = slot.startTime.toString(),
                endTime        = slot.endTime.toString(),
                colorIndex     = slot.colorIndex,
                selectedRecipeId    = slot.recipeId,
                selectedRecipeTitle = slot.recipeId?.toString()
                    ?.let { id -> it.recipeTitles[id] } ?: ""
            )
        )
    }

    fun closeSlotForm() = _uiState.update {
        it.copy(slotForm = SlotFormState())
    }

    fun onSlotNameChange(name: String) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(name = name, nameError = null))
    }

    fun onSlotStartTimeChange(time: String) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(startTime = time, timeError = null))
    }

    fun onSlotEndTimeChange(time: String) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(endTime = time, timeError = null))
    }

    fun onSlotColorChange(index: Int) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(colorIndex = index))
    }

    // ── Recipe picker ─────────────────────────────────────────────────────────

    fun openRecipePicker() = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = true))
    }

    fun closeRecipePicker() = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = false))
    }

    fun onRecipePickerQueryChange(query: String) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(recipePickerQuery = query))
    }

    fun selectRecipe(recipe: Recipe?) = _uiState.update {
        it.copy(
            slotForm = it.slotForm.copy(
                selectedRecipeId    = recipe?.id,
                selectedRecipeTitle = recipe?.title ?: "",
                isRecipePickerVisible = false
            )
        )
    }

    // ── Save slot ─────────────────────────────────────────────────────────────

    fun saveSlot() {
        val state = _uiState.value
        val plan  = state.plan ?: return
        val form  = state.slotForm

        // Validate
        if (form.name.isBlank()) {
            _uiState.update { it.copy(slotForm = form.copy(nameError = "Name is required")) }
            return
        }
        val startTime = runCatching { SlotTime.fromHHmm(form.startTime) }.getOrNull()
        val endTime   = runCatching { SlotTime.fromHHmm(form.endTime) }.getOrNull()
        if (startTime == null || endTime == null || startTime >= endTime) {
            _uiState.update { it.copy(slotForm = form.copy(timeError = "Enter valid times (HH:MM), start before end")) }
            return
        }

        val day = state.selectedDay
        val slot = MealSlot(
            id         = form.editingSlotId ?: UUID.randomUUID(),
            name       = form.name.trim(),
            startTime  = startTime,
            endTime    = endTime,
            colorIndex = form.colorIndex,
            recipeId   = form.selectedRecipeId
        )

        _uiState.update { it.copy(isSaving = true) }
        val updated = try {
            if (form.editingSlotId != null) plan.updateSlot(day, slot)
            else                            plan.addSlot(day, slot)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(isSaving = false, slotForm = form.copy(timeError = e.message)) }
            return
        }
        save(updated)
        _uiState.update { it.copy(slotForm = SlotFormState()) }
    }

    fun deleteSlot(slot: MealSlot) {
        val plan = _uiState.value.plan ?: return
        val day  = _uiState.value.selectedDay
        save(plan.removeSlot(day, slot.id))
    }

    // ── Internal save ─────────────────────────────────────────────────────────

    private fun save(updated: MealPlan) {
        viewModelScope.launch {
            runCatching { mealPlanRepository.updateMealPlan(updated) }
                .onSuccess { _uiState.update { it.copy(plan = updated, isSaving = false) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}