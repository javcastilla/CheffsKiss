package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.UpdateMealPlanCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.application.services.UserRecipeCatalogService
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import software.ulpgc.cheffskiss.ui.screen.label
import java.util.UUID

data class SlotFormState(
    val isVisible: Boolean = false,
    val editingSlotId: UUID? = null,
    val selectedDay: WeekDay = WeekDay.MONDAY,
    val selectedMealType: MealType = MealType.BREAKFAST,
    val selectedRecipeId: UUID? = null,
    val selectedRecipeTitle: String = "",
    val isRecipePickerVisible: Boolean = false,
    val recipePickerQuery: String = "",
    val previewRecipe: Recipe? = null,
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
    private val recipeCatalog: UserRecipeCatalogService = UserRecipeCatalogService(),
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
            val userUuid = UserIds.creatorIdFromFirebaseUid(uid)
            mealPlanRepository.getMealPlans(userUuid)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { plans ->
                    val plan = plans.firstOrNull { it.id == planUuid }
                    _uiState.update { it.copy(plan = plan, isLoading = false) }
                    if (plan != null) {
                        cacheRecipeTitles(plan)
                        loadAvailableRecipes()
                    }
                }
        }
    }

    private fun loadAvailableRecipes() {
        viewModelScope.launch {
            runCatching { recipeCatalog.loadOwnedAndSaved() }
                .onSuccess { recipes ->
                    _uiState.update { it.copy(availableRecipes = recipes) }
                }
        }
    }

    private fun cacheRecipeTitles(plan: MealPlan) {
        val titles = plan.mealSlots
            .mapNotNull { slot -> slot.recipe?.let { it.id.toString() to it.title } }
            .toMap()
        if (titles.isNotEmpty()) {
            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
        }
    }

    fun selectDay(day: WeekDay) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun openAddSlot() {
        _uiState.update {
            it.copy(
                slotForm = SlotFormState(
                    isVisible = true,
                    selectedDay = it.selectedDay,
                )
            )
        }
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
                    selectedRecipeTitle = slot.recipe?.title ?: "",
                )
            )
        }
    }

    fun closeSlotForm() {
        _uiState.update { it.copy(slotForm = SlotFormState(), error = null) }
    }

    fun onSlotMealTypeChange(mealType: MealType) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(selectedMealType = mealType)) }
    }

    fun openRecipePicker() {
        _uiState.update {
            it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = true, previewRecipe = null))
        }
    }

    fun closeRecipePicker() {
        _uiState.update {
            it.copy(slotForm = it.slotForm.copy(isRecipePickerVisible = false, previewRecipe = null))
        }
    }

    fun onRecipePickerQueryChange(query: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(recipePickerQuery = query)) }
    }

    fun openRecipePreview(recipe: Recipe) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(previewRecipe = recipe)) }
    }

    fun closeRecipePreview() {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(previewRecipe = null)) }
    }

    fun selectRecipe(recipe: Recipe?) {
        _uiState.update {
            it.copy(
                slotForm = it.slotForm.copy(
                    selectedRecipeId = recipe?.id,
                    selectedRecipeTitle = recipe?.title ?: "",
                    isRecipePickerVisible = false,
                    recipePickerQuery = "",
                    previewRecipe = null,
                )
            )
        }
    }

    fun saveSlot() {
        val form = _uiState.value.slotForm
        val plan = _uiState.value.plan ?: return

        if (form.selectedRecipeId == null) {
            _uiState.update { it.copy(error = "Select a recipe for this slot") }
            return
        }

        val duplicateMealType = plan.mealSlots.any { slot ->
            slot.id != form.editingSlotId &&
                slot.day == form.selectedDay &&
                slot.mealType == form.selectedMealType
        }
        if (duplicateMealType) {
            _uiState.update {
                it.copy(error = "This day already has a ${form.selectedMealType.label().lowercase()} slot")
            }
            return
        }

        val recipe = _uiState.value.availableRecipes.firstOrNull { it.id == form.selectedRecipeId }
            ?: form.previewRecipe

        val newSlot = MealSlot(
            id = form.editingSlotId ?: UUID.randomUUID(),
            day = form.selectedDay,
            mealType = form.selectedMealType,
            recipe = recipe,
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
            _uiState.update { it.copy(isSaving = true, error = null) }
            val versioned = plan.nextVersion()
            runCatching { UpdateMealPlanCommand(mealPlanRepository, versioned).execute() }
                .onSuccess {
                    cacheRecipeTitles(versioned)
                    _uiState.update {
                        it.copy(plan = versioned, isSaving = false, slotForm = SlotFormState())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }
}
