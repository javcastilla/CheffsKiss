package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.UpdateMealPlanCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.application.services.UserIds
import software.ulpgc.cheffskiss.application.services.MealPlanRecipeHydrator
import software.ulpgc.cheffskiss.application.services.UserRecipeCatalogService
import software.ulpgc.cheffskiss.application.control.SetActiveMealPlanCommand
import software.ulpgc.cheffskiss.domain.model.mealplan.sortedBySchedule
import software.ulpgc.cheffskiss.domain.model.mealplan.sortedSlots
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import software.ulpgc.cheffskiss.ui.screen.label
import java.util.UUID

data class SlotFormState(
    val isVisible: Boolean = false,
    val hasDraft: Boolean = false,
    val editingSlotId: UUID? = null,
    val selectedDay: WeekDay = WeekDay.MONDAY,
    val selectedMealType: MealType = MealType.BREAKFAST,
    val selectedRecipeId: UUID? = null,
    val selectedRecipeTitle: String = "",
    val isRecipePickerVisible: Boolean = false,
    val recipePickerQuery: String = "",
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
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val mealPlanHydrator: MealPlanRecipeHydrator = MealPlanRecipeHydrator(recipeReader),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanDetailUiState())
    val uiState: StateFlow<MealPlanDetailUiState> = _uiState.asStateFlow()

    private val _pendingPickRecipeId = MutableStateFlow<String?>(null)
    val pendingPickRecipeId: StateFlow<String?> = _pendingPickRecipeId.asStateFlow()

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
                    val rawPlan = plans.firstOrNull { it.id == planUuid }
                    val plan = rawPlan?.let { mealPlanHydrator.hydrate(it) }
                    _uiState.update { it.copy(plan = plan, isLoading = false) }
                    if (plan != null) {
                        viewModelScope.launch {
                            val titles = mealPlanHydrator.recipeTitles(plan)
                            _uiState.update { it.copy(recipeTitles = it.recipeTitles + titles) }
                        }
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

    fun selectDay(day: WeekDay) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun openAddSlot() {
        _uiState.update {
            it.copy(slotForm = SlotFormState(
                isVisible = true,
                hasDraft = true,
                selectedDay = it.selectedDay
            ))
        }
    }

    fun openEditSlot(slot: MealSlot) {
        val title = slot.recipe?.title
            ?: slot.resolvedRecipeId()?.let { _uiState.value.recipeTitles[it.toString()] }
            ?: ""
        _uiState.update {
            it.copy(
                slotForm = SlotFormState(
                    isVisible = true,
                    hasDraft = true,
                    editingSlotId = slot.id,
                    selectedDay = slot.day,
                    selectedMealType = slot.mealType,
                    selectedRecipeId = slot.resolvedRecipeId(),
                    selectedRecipeTitle = title,
                ),
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
            it.copy(slotForm = it.slotForm.copy(
                isVisible = false,
                isRecipePickerVisible = true,
            ))
        }
    }

    fun closeRecipePicker() {
        _uiState.update {
            val form = it.slotForm
            it.copy(slotForm = form.copy(
                isVisible = form.hasDraft,
                isRecipePickerVisible = false,
                recipePickerQuery = "",
            ))
        }
    }

    fun requestPickRecipeDetail(recipeId: String) {
        preparePickNavigation()
        _pendingPickRecipeId.value = recipeId
    }

    fun consumePendingPickRecipe() {
        _pendingPickRecipeId.value = null
    }

    fun onRecipePickerQueryChange(query: String) {
        _uiState.update { it.copy(slotForm = it.slotForm.copy(recipePickerQuery = query)) }
    }

    fun preparePickNavigation() {
        _uiState.update {
            it.copy(slotForm = it.slotForm.copy(
                isVisible = false,
                isRecipePickerVisible = false,
                recipePickerQuery = "",
            ))
        }
    }

    fun restoreSlotFormFromPickFlow() {
        _uiState.update {
            val form = it.slotForm
            if (!form.hasDraft) return@update it
            it.copy(slotForm = form.copy(
                isVisible = false,
                isRecipePickerVisible = true,
                recipePickerQuery = "",
            ))
        }
    }

    fun applyPickedRecipe(recipeId: String) {
        viewModelScope.launch {
            val recipe = resolveRecipe(recipeId) ?: return@launch
            _uiState.update {
                it.copy(slotForm = it.slotForm.copy(
                    selectedRecipeId = recipe.id,
                    selectedRecipeTitle = recipe.title,
                    isVisible = it.slotForm.hasDraft,
                    isRecipePickerVisible = false,
                    recipePickerQuery = "",
                ))
            }
        }
    }

    fun selectRecipe(recipe: Recipe?) {
        _uiState.update {
            it.copy(slotForm = it.slotForm.copy(
                selectedRecipeId = recipe?.id,
                selectedRecipeTitle = recipe?.title ?: "",
                isVisible = it.slotForm.hasDraft,
                isRecipePickerVisible = false,
                recipePickerQuery = "",
            ))
        }
    }

    private suspend fun resolveRecipe(recipeId: String): Recipe? {
        val id = runCatching { UUID.fromString(recipeId) }.getOrNull() ?: return null
        return _uiState.value.availableRecipes.firstOrNull { it.id == id }
            ?: recipeReader.getById(recipeId)
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

        viewModelScope.launch {
            val recipeId = form.selectedRecipeId ?: return@launch
            val recipe = resolveRecipe(recipeId.toString()) ?: run {
                _uiState.update { it.copy(error = "Recipe not found") }
                return@launch
            }
            saveSlotWithRecipe(plan, form, recipe)
        }
    }

    private fun saveSlotWithRecipe(plan: MealPlan, form: SlotFormState, recipe: Recipe) {
        val newSlot = MealSlot(
            id = form.editingSlotId ?: UUID.randomUUID(),
            day = form.selectedDay,
            mealType = form.selectedMealType,
            recipe = recipe,
            recipeId = recipe.id,
        )
        val updatedSlots = if (form.editingSlotId != null) {
            plan.mealSlots.map { if (it.id == form.editingSlotId) newSlot else it }
        } else {
            plan.mealSlots + newSlot
        }
        save(plan.copy(mealSlots = updatedSlots).sortedSlots())
    }

    fun deleteSlot(slot: MealSlot) {
        val plan = _uiState.value.plan ?: return
        save(plan.copy(mealSlots = plan.mealSlots.filter { it.id != slot.id }).sortedSlots())
    }

    fun setAsPrimary() {
        val plan = _uiState.value.plan ?: return
        val uid = currentUserPort.getCurrentUser() ?: return
        viewModelScope.launch {
            runCatching { SetActiveMealPlanCommand(mealPlanRepository, uid, plan.id).execute() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun save(plan: MealPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val normalized = plan.sortedSlots()
            val versioned = normalized.nextVersion()
            runCatching { UpdateMealPlanCommand(mealPlanRepository, versioned).execute() }
                .onSuccess {
                    val hydrated = mealPlanHydrator.hydrate(versioned)
                    val titles = mealPlanHydrator.recipeTitles(hydrated)
                    _uiState.update {
                        it.copy(
                            plan = hydrated,
                            isSaving = false,
                            slotForm = SlotFormState(),
                            recipeTitles = it.recipeTitles + titles,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }
}