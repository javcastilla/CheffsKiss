package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.UpdateMealPlanCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import java.util.UUID

data class SlotFormState(
    val isVisible: Boolean = false,
    val editingSlotId: UUID? = null,
    val selectedDay: WeekDay = WeekDay.MONDAY,
    val selectedMealType: MealType = MealType.BREAKFAST,
    val selectedRecipeId: UUID? = null,
    val selectedRecipeTitle: String = "",
    val isRecipePickerVisible: Boolean = false,
    val recipePickerQuery: String = ""
)

data class MealPlanDetailUiState(
    val plan: MealPlan? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val selectedDay: WeekDay = WeekDay.MONDAY,
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
        val ids = plan.mealSlots
            .mapNotNull { it.recipe?.id?.toString() }
            .toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val titles = ids.associateWith { id -> recipeReader.getById(id)?.title ?: "" }
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
        }
    }

    fun selectDay(day: WeekDay) {
        _uiState.update { it.copy(selectedDay = day) }
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
                    selectedDay = slot.day,
                    selectedMealType = slot.mealType,
                    selectedRecipeId = slot.recipe?.id,
                    selectedRecipeTitle = slot.recipe?.title ?: ""
                )
            )
        }
    }

    fun closeSlotForm() {
        _uiState.update { it.copy(slotForm = SlotFormState()) }
    }

    fun onSlotMealTypeChange(mealType: MealType) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(selectedMealType = mealType)) }
    }

    fun onSlotDayChange(day: WeekDay) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(selectedDay = day)) }
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

        if (form.selectedRecipeId == null) {
            _uiState.update { it.copy(slotForm = it.slotForm.copy()) }
            return
        }

        val newSlot = MealSlot(
            id = form.editingSlotId ?: UUID.randomUUID(),
            day = form.selectedDay,
            mealType = form.selectedMealType,
            recipe = _uiState.value.availableRecipes.firstOrNull { it.id == form.selectedRecipeId }
        )

        val updatedSlots = if (form.editingSlotId != null) {
            plan.mealSlots.map { if (it.id == form.editingSlotId) newSlot else it }
        } else {
            plan.mealSlots + newSlot
        }

        save(plan.copy(mealSlots = updatedSlots))
    }

    fun deleteSlot(slot: MealSlot) {
        val plan = _uiState.value.plan ?: return
        save(plan.copy(mealSlots = plan.mealSlots.filter { it.id != slot.id }))
    }

    private fun save(plan: MealPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val versioned = plan.nextVersion()
            runCatching { UpdateMealPlanCommand(mealPlanRepository, versioned).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(plan = versioned, isSaving = false, slotForm = SlotFormState()) }
        }
    }
}