package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.SaveRecipeCommand
import software.ulpgc.cheffskiss.application.SaveRecipeInput
import software.ulpgc.cheffskiss.application.UnsaveRecipeCommand
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.application.services.GetMealPlansQuery
import software.ulpgc.cheffskiss.application.services.GetSavedRecipesQuery
import software.ulpgc.cheffskiss.domain.model.MealSlot
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseMealPlanService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class ActivePlanDay(
    val planId: String,
    val planName: String,
    val todayName: String,
    val slots: List<MealSlot>,
    val recipeTitles: Map<String, String> = emptyMap()
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val savedRecipeIds: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val activePlanDay: ActivePlanDay? = null,
    val error: String? = null
)

class HomeViewModel(
    private val getAllRecipesQuery: GetAllRecipesQuery = GetAllRecipesQuery(FirebaseRecipeReader()),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val mealPlanRepository: MealPlanRepository = FirebaseMealPlanService(),
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _authorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val authorNames: StateFlow<Map<String, String>> = _authorNames.asStateFlow()

    private val userNameReader = FirebaseUserNameReader()

    init {
        _uiState.update { it.copy(currentUserId = currentUserPort.getCurrentUser()) }
        observeRecipes()
        observeSavedRecipes()
        observeActivePlan()
    }

    fun retryLoad() {
        observeRecipes()
        observeSavedRecipes()
        observeActivePlan()
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            getAllRecipesQuery()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error loading recipes") } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, recipes = list, error = null) }
                    resolveAllAuthors(list)
                }
        }
    }

    private fun observeSavedRecipes() {
        val uid = currentUserPort.getCurrentUser() ?: return
        viewModelScope.launch {
            GetSavedRecipesQuery(recipeRepository)(uid)
                .catch { }
                .collect { savedList ->
                    _uiState.update { it.copy(savedRecipeIds = savedList.map { s -> s.recipeId.toString() }.toSet()) }
                }
        }
    }

    private fun observeActivePlan() {
        val uid = currentUserPort.getCurrentUser() ?: return
        val userUuid = UUID.nameUUIDFromBytes(uid.toByteArray())
        val todayWeekday = LocalDate.now().dayOfWeek.toWeekday()

        viewModelScope.launch {
            GetMealPlansQuery(mealPlanRepository)(userUuid)
                .catch { }
                .collect { plans ->
                    val active = plans.firstOrNull { it.isActive }
                    if (active == null) {
                        _uiState.update { it.copy(activePlanDay = null) }
                        return@collect
                    }

                    val slots = active.days[todayWeekday] ?: emptyList()
                    val planDay = ActivePlanDay(
                        planId    = active.id.toString(),
                        planName  = active.name,
                        todayName = todayWeekday.fullName,
                        slots     = slots
                    )
                    _uiState.update { it.copy(activePlanDay = planDay) }

                    // Resolve recipe titles for today's slots
                    val recipeIds = slots.mapNotNull { it.recipeId?.toString() }.distinct()
                    if (recipeIds.isNotEmpty()) {
                        val titles = coroutineScope {
                            recipeIds.map { id -> async { id to recipeReader.getById(id)?.title } }
                                .awaitAll()
                                .mapNotNull { (id, title) -> title?.let { id to it } }
                                .toMap()
                        }
                        _uiState.update { s ->
                            s.copy(activePlanDay = s.activePlanDay?.copy(recipeTitles = titles))
                        }
                    }
                }
        }
    }

    fun toggleSave(recipe: Recipe) {
        val uid = currentUserPort.getCurrentUser() ?: return
        if (recipe.author == uid) return
        val recipeIdStr = recipe.id.toString()
        val currentlySaved = _uiState.value.savedRecipeIds.contains(recipeIdStr)

        _uiState.update {
            it.copy(savedRecipeIds = if (currentlySaved) it.savedRecipeIds - recipeIdStr else it.savedRecipeIds + recipeIdStr)
        }

        viewModelScope.launch {
            runCatching {
                if (currentlySaved) {
                    UnsaveRecipeCommand(recipeRepository, uid, recipe.id).execute()
                } else {
                    SaveRecipeCommand(recipeRepository, object : SaveRecipeInput {
                        override fun recipeId() = recipe.id
                        override fun userId()   = uid
                    }).execute()
                }
            }.onFailure {
                _uiState.update { s ->
                    s.copy(savedRecipeIds = if (currentlySaved) s.savedRecipeIds + recipeIdStr else s.savedRecipeIds - recipeIdStr)
                }
            }
        }
    }

    private fun resolveAllAuthors(recipes: List<Recipe>) {
        recipes.map { it.author }.distinct().forEach { uid ->
            if (!_authorNames.value.containsKey(uid)) {
                viewModelScope.launch {
                    val name = userNameReader.getUsernameByUid(uid)
                    if (name != null) _authorNames.update { it + (uid to name) }
                }
            }
        }
    }

    private fun DayOfWeek.toWeekday() = when (this) {
        DayOfWeek.MONDAY    -> Weekday.MONDAY
        DayOfWeek.TUESDAY   -> Weekday.TUESDAY
        DayOfWeek.WEDNESDAY -> Weekday.WEDNESDAY
        DayOfWeek.THURSDAY  -> Weekday.THURSDAY
        DayOfWeek.FRIDAY    -> Weekday.FRIDAY
        DayOfWeek.SATURDAY  -> Weekday.SATURDAY
        DayOfWeek.SUNDAY    -> Weekday.SUNDAY
    }
}
