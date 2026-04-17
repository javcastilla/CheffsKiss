package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.SetActiveMealPlanCommand
import software.ulpgc.cheffskiss.application.UpdateMealPlanCommand
import software.ulpgc.cheffskiss.application.port.output.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.output.MealPlanPort
import software.ulpgc.cheffskiss.application.services.GetMealPlansQuery
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.MealSlot
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Weekday
import java.time.DayOfWeek
import java.time.LocalDate
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import java.util.UUID

data class SlotFormState(
    val isVisible: Boolean = false,
    val editingSlotId: UUID? = null,
    val name: String = "",
    val startTime: String = "08:00",
    val endTime: String = "09:00",
    val colorIndex: Int = 0,
    val selectedRecipeId: UUID? = null,
    val selectedRecipeTitle: String = "",
    val nameError: String? = null,
    val timeError: String? = null,
    val isRecipePickerVisible: Boolean = false,
    val recipePickerQuery: String = ""
)

private fun todayWeekday(): Weekday = when (LocalDate.now().dayOfWeek) {
    DayOfWeek.MONDAY    -> Weekday.MONDAY
    DayOfWeek.TUESDAY   -> Weekday.TUESDAY
    DayOfWeek.WEDNESDAY -> Weekday.WEDNESDAY
    DayOfWeek.THURSDAY  -> Weekday.THURSDAY
    DayOfWeek.FRIDAY    -> Weekday.FRIDAY
    DayOfWeek.SATURDAY  -> Weekday.SATURDAY
    DayOfWeek.SUNDAY    -> Weekday.SUNDAY
}

data class MealPlanDetailUiState(
    val isLoading: Boolean = true,
    val plan: MealPlan? = null,
    val selectedDay: Weekday = todayWeekday(),
    val recipeTitles: Map<String, String> = emptyMap(),
    val availableRecipes: List<Recipe> = emptyList(),
    val slotForm: SlotFormState = SlotFormState(),
    val error: String? = null,
    val isSaving: Boolean = false
)

class MealPlanDetailViewModel(
    private val port: MealPlanPort = FirebaseMealPlanService(),
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanDetailUiState())
    val uiState: StateFlow<MealPlanDetailUiState> = _uiState.asStateFlow()

    private val userId: String? get() = currentUserPort.getCurrentUser()

    // ── Load ──────────────────────────────────────────────────────────────────

    fun load(planId: String) {
        val uid = userId ?: return
        val userUuid = UUID.nameUUIDFromBytes(uid.toByteArray())

        viewModelScope.launch {
            GetMealPlansQuery(port)(userUuid)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { plans ->
                    val plan = plans.find { it.id.toString() == planId }
                    _uiState.update { it.copy(isLoading = false, plan = plan) }
                    plan?.let { resolveRecipeTitles(it) }
                }
        }

        viewModelScope.launch {
            recipeReader.getAll()
                .catch { }
                .collect { recipes ->
                    _uiState.update { it.copy(availableRecipes = recipes) }
                }
        }
    }

    private suspend fun resolveRecipeTitles(plan: MealPlan) {
        val recipeIds = plan.days.values.flatten()
            .mapNotNull { it.recipeId?.toString() }
            .distinct()
            .filter { it !in _uiState.value.recipeTitles }

        if (recipeIds.isEmpty()) return

        coroutineScope {
            recipeIds.map { id ->
                async {
                    val recipe = recipeReader.getById(id)
                    if (recipe != null) id to recipe.title else null
                }
            }.awaitAll().filterNotNull()
        }.forEach { (id, title) ->
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + (id to title)) }
        }
    }

    // ── Day selection ─────────────────────────────────────────────────────────

    fun selectDay(day: Weekday) = _uiState.update { it.copy(selectedDay = day) }

    // ── Set active ────────────────────────────────────────────────────────────

    fun toggleActive() {
        val uid = userId ?: return
        val plan = _uiState.value.plan ?: return
        if (plan.isActive) return
        viewModelScope.launch {
            runCatching { SetActiveMealPlanCommand(port, uid, plan.id).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── Slot form ─────────────────────────────────────────────────────────────

    fun openAddSlot() = _uiState.update {
        it.copy(slotForm = SlotFormState(isVisible = true))
    }

    fun openEditSlot(slot: MealSlot) = _uiState.update {
        it.copy(
            slotForm = SlotFormState(
                isVisible          = true,
                editingSlotId      = slot.id,
                name               = slot.name,
                startTime          = slot.startTime,
                endTime            = slot.endTime,
                colorIndex         = slot.colorIndex,
                selectedRecipeId   = slot.recipeId,
                selectedRecipeTitle = slot.recipeId?.let { rid ->
                    it.recipeTitles[rid.toString()] ?: ""
                } ?: ""
            )
        )
    }

    fun closeSlotForm() = _uiState.update { it.copy(slotForm = SlotFormState()) }

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

    fun openRecipePicker() = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = true, recipePickerQuery = ""))
    }

    fun closeRecipePicker() = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = false))
    }

    fun onRecipePickerQueryChange(q: String) = _uiState.update {
        it.copy(slotForm = it.slotForm.copy(recipePickerQuery = q))
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
        val day   = state.selectedDay

        // Validate name
        if (form.name.isBlank()) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(nameError = "Name is required")) }
            return
        }

        // Validate time format and logic
        val startMins = form.startTime.toMinutes()
        val endMins   = form.endTime.toMinutes()
        if (startMins == null || endMins == null) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(timeError = "Invalid time format (HH:mm)")) }
            return
        }
        if (endMins <= startMins) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(timeError = "End time must be after start time")) }
            return
        }

        val newSlot = MealSlot(
            id         = form.editingSlotId ?: UUID.randomUUID(),
            name       = form.name.trim(),
            startTime  = form.startTime,
            endTime    = form.endTime,
            recipeId   = form.selectedRecipeId,
            colorIndex = form.colorIndex
        )

        // Overlap validation
        val existingSlots = plan.days[day] ?: emptyList()
        val hasOverlap = existingSlots
            .filter { it.id != newSlot.id }
            .any { overlaps(it, newSlot) }

        if (hasOverlap) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy(timeError = "This time overlaps with an existing slot")) }
            return
        }

        val updatedSlots = if (form.editingSlotId != null) {
            existingSlots.map { if (it.id == form.editingSlotId) newSlot else it }
        } else {
            existingSlots + newSlot
        }.sortedBy { it.startTime.toMinutes() ?: 0 }

        val updatedPlan = plan.copy(days = plan.days + (day to updatedSlots))

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { UpdateMealPlanCommand(port, updatedPlan).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isSaving = false, slotForm = SlotFormState()) }
        }

        // Optimistic update + cache recipe title
        form.selectedRecipeId?.let { rid ->
            if (form.selectedRecipeTitle.isNotBlank()) {
                _uiState.update {
                    it.copy(recipeTitles = it.recipeTitles + (rid.toString() to form.selectedRecipeTitle))
                }
            }
        }
    }

    // ── Delete slot ───────────────────────────────────────────────────────────

    fun deleteSlot(slot: MealSlot) {
        val plan = _uiState.value.plan ?: return
        val day  = _uiState.value.selectedDay
        val updatedSlots = (plan.days[day] ?: emptyList()).filter { it.id != slot.id }
        val updatedPlan = plan.copy(days = plan.days + (day to updatedSlots))

        viewModelScope.launch {
            runCatching { UpdateMealPlanCommand(port, updatedPlan).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun String.toMinutes(): Int? {
        val parts = split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun overlaps(a: MealSlot, b: MealSlot): Boolean {
        val s1 = a.startTime.toMinutes() ?: return false
        val e1 = a.endTime.toMinutes()   ?: return false
        val s2 = b.startTime.toMinutes() ?: return false
        val e2 = b.endTime.toMinutes()   ?: return false
        return s2 < e1 && e2 > s1
    }
}
