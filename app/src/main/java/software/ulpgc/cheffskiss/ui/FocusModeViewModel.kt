package software.ulpgc.cheffskiss.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.port.FocusSessionRepository
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.focus.FocusCapabilities
import software.ulpgc.cheffskiss.domain.model.focus.FocusPhase
import software.ulpgc.cheffskiss.domain.model.focus.FocusSession
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeLineStore
import software.ulpgc.cheffskiss.domain.port.input.StepStore
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.LocalFocusSessionRepository
import java.util.UUID

data class FocusTimerState(
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
)

data class FocusModeUiState(
    val isLoading: Boolean = true,
    val recipe: Recipe? = null,
    val lines: List<software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine> = emptyList(),
    val steps: List<Step> = emptyList(),
    val capabilities: FocusCapabilities? = null,
    val phase: FocusPhase = FocusPhase.INTRO,
    val currentStepIndex: Int = 0,
    val completedStepIds: Set<UUID> = emptySet(),
    val keepScreenOn: Boolean = true,
    val timer: FocusTimerState = FocusTimerState(),
    val showExitDialog: Boolean = false,
    val elapsedMs: Long = 0L,
    val error: String? = null,
    val resumedSession: Boolean = false,
    val usingCachedData: Boolean = false,
)

class FocusModeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val recipeStore = FirebaseRecipeReader()
    private val lineStore: RecipeLineStore = recipeStore
    private val stepStore: StepStore = recipeStore
    private val focusSessionRepository: FocusSessionRepository = LocalFocusSessionRepository(application)

    private var recipeId: UUID? = null

    private val _uiState = MutableStateFlow(FocusModeUiState())
    val uiState: StateFlow<FocusModeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartedAt: Long = System.currentTimeMillis()
    private var bootstrapped = false
    private var networkLoadStarted = false

    /**
     * Uses recipe/lines/steps already loaded on the detail screen (works offline).
     */
    fun bootstrap(
        recipe: Recipe,
        lines: List<software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine>,
        steps: List<Step>,
    ) {
        if (bootstrapped) return
        bootstrapped = true
        viewModelScope.launch {
            runCatching {
                recipeId = recipe.id
                val saved = focusSessionRepository.load(recipe.id)
                sessionStartedAt = saved?.startedAtEpochMs ?: System.currentTimeMillis()
                applyRecipeData(
                    recipe = recipe,
                    lines = lines,
                    steps = steps.sortedBy { it.cardinal },
                    saved = saved,
                    usingCachedData = true,
                )
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Could not start focus mode")
                }
            }
        }
    }

    /** Fallback when opening focus without detail cache (requires network). */
    fun ensureNetworkLoad() {
        if (bootstrapped || networkLoadStarted) return
        networkLoadStarted = true
        viewModelScope.launch {
            runCatching {
                val recipeIdString = savedStateHandle.get<String>(ARG_RECIPE_ID)
                if (recipeIdString.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false, error = "Missing recipe id") }
                    return@launch
                }
                val parsedId = runCatching { UUID.fromString(recipeIdString) }.getOrNull()
                if (parsedId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid recipe id") }
                    return@launch
                }
                recipeId = parsedId

                val recipe = recipeStore.getById(parsedId.toString())
                if (recipe == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Recipe not found. Check your connection and try again.",
                        )
                    }
                    return@launch
                }

                val saved = focusSessionRepository.load(parsedId)
                sessionStartedAt = saved?.startedAtEpochMs ?: System.currentTimeMillis()
                bootstrapped = true

                _uiState.update {
                    it.copy(
                        recipe = recipe,
                        isLoading = false,
                        currentStepIndex = saved?.currentStepIndex ?: 0,
                        completedStepIds = saved?.completedStepIds ?: emptySet(),
                        keepScreenOn = saved?.keepScreenOn ?: true,
                        elapsedMs = saved?.elapsedMs ?: 0L,
                        resumedSession = saved != null,
                        usingCachedData = false,
                        phase = restoredPhase(saved),
                    )
                }

                observeLines(recipe)
                observeSteps(recipe)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Could not load recipe. Check your connection.",
                    )
                }
            }
        }
    }

    private suspend fun applyRecipeData(
        recipe: Recipe,
        lines: List<software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine>,
        steps: List<Step>,
        saved: FocusSession?,
        usingCachedData: Boolean,
    ) {
        val caps = FocusCapabilities.from(
            stepCount = steps.size,
            timedStepCount = steps.count { it.hasTimer() },
            mediaStepCount = steps.count { it.hasMedia() },
            totalDurationMinutes = recipe.duration.inWholeMinutes,
        )
        _uiState.update {
            it.copy(
                recipe = recipe,
                lines = lines,
                steps = steps,
                capabilities = caps,
                isLoading = false,
                error = if (steps.isEmpty()) "This recipe has no steps yet." else null,
                currentStepIndex = saved?.currentStepIndex ?: 0,
                completedStepIds = saved?.completedStepIds ?: emptySet(),
                keepScreenOn = saved?.keepScreenOn ?: true,
                elapsedMs = saved?.elapsedMs ?: 0L,
                resumedSession = saved != null,
                usingCachedData = usingCachedData,
                phase = restoredPhase(saved),
            )
        }
    }

    private fun restoredPhase(saved: FocusSession?): FocusPhase =
        if (saved != null && saved.currentStepIndex > 0) FocusPhase.STEP else FocusPhase.INTRO

    private fun observeLines(recipe: Recipe) {
        viewModelScope.launch {
            runCatching {
                lineStore.linesOf(recipe)
                    .catch { }
                    .collect { lines -> _uiState.update { s -> s.copy(lines = lines) } }
            }
        }
    }

    private fun observeSteps(recipe: Recipe) {
        viewModelScope.launch {
            runCatching {
                stepStore.stepsOf(recipe)
                    .catch { }
                    .collect { rawSteps ->
                        val steps = rawSteps.sortedBy { it.cardinal }
                        val caps = FocusCapabilities.from(
                            stepCount = steps.size,
                            timedStepCount = steps.count { it.hasTimer() },
                            mediaStepCount = steps.count { it.hasMedia() },
                            totalDurationMinutes = recipe.duration.inWholeMinutes,
                        )
                        _uiState.update { s -> s.copy(steps = steps, capabilities = caps) }
                    }
            }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun startCooking() {
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return
        _uiState.update {
            it.copy(
                phase = FocusPhase.STEP,
                currentStepIndex = it.currentStepIndex.coerceIn(0, steps.lastIndex),
            )
        }
        resetTimerForCurrentStep()
        persistSession()
    }

    fun goToIntro() {
        cancelTimer()
        _uiState.update { it.copy(phase = FocusPhase.INTRO, timer = FocusTimerState()) }
    }

    fun previousStep() {
        val index = _uiState.value.currentStepIndex
        if (index <= 0) return
        cancelTimer()
        _uiState.update { it.copy(currentStepIndex = index - 1, timer = FocusTimerState()) }
        resetTimerForCurrentStep()
        persistSession()
    }

    fun nextStep() {
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return
        val index = _uiState.value.currentStepIndex
        val step = steps.getOrNull(index) ?: return
        markStepCompleted(step.id)
        if (index >= steps.lastIndex) {
            finishRecipe()
        } else {
            cancelTimer()
            _uiState.update { it.copy(currentStepIndex = index + 1, timer = FocusTimerState()) }
            resetTimerForCurrentStep()
            persistSession()
        }
    }

    fun markCurrentStepCompleted() {
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return
        val index = _uiState.value.currentStepIndex
        val step = steps.getOrNull(index) ?: return
        markStepCompleted(step.id)
        if (index >= steps.lastIndex) {
            finishRecipe()
        } else {
            cancelTimer()
            _uiState.update { it.copy(currentStepIndex = index + 1, timer = FocusTimerState()) }
            resetTimerForCurrentStep()
            persistSession()
        }
    }

    private fun markStepCompleted(stepId: UUID) {
        _uiState.update { it.copy(completedStepIds = it.completedStepIds + stepId) }
        persistSession()
    }

    private fun finishRecipe() {
        cancelTimer()
        val elapsed = _uiState.value.elapsedMs + (System.currentTimeMillis() - sessionStartedAt)
        _uiState.update {
            it.copy(
                phase = FocusPhase.COMPLETE,
                elapsedMs = elapsed,
                timer = FocusTimerState(),
            )
        }
        val id = recipeId ?: return
        viewModelScope.launch {
            runCatching { focusSessionRepository.clear(id) }
        }
    }

    fun requestExit() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun continueCooking() {
        dismissExitDialog()
    }

    fun exitAndSaveProgress() {
        val elapsed = _uiState.value.elapsedMs + (System.currentTimeMillis() - sessionStartedAt)
        _uiState.update { it.copy(elapsedMs = elapsed, showExitDialog = false) }
        cancelTimer()
        persistSession()
    }

    fun exitWithoutSaving(onDone: () -> Unit) {
        cancelTimer()
        val id = recipeId
        viewModelScope.launch {
            if (id != null) {
                runCatching { focusSessionRepository.clear(id) }
            }
            onDone()
        }
    }

    fun resetTimerForCurrentStep() {
        val step = currentStep() ?: return
        val seconds = step.timerSeconds()
        _uiState.update {
            it.copy(
                timer = FocusTimerState(
                    totalSeconds = seconds,
                    remainingSeconds = seconds,
                ),
            )
        }
    }

    fun startTimer() {
        val state = _uiState.value.timer
        if (state.totalSeconds <= 0) return
        if (state.isRunning) return
        val remaining = if (state.remainingSeconds > 0) state.remainingSeconds else state.totalSeconds
        _uiState.update {
            it.copy(
                timer = state.copy(
                    isRunning = true,
                    isPaused = false,
                    remainingSeconds = remaining,
                    isFinished = false,
                ),
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var left = remaining
            while (isActive && left > 0) {
                delay(1_000)
                left -= 1
                _uiState.update { s ->
                    s.copy(timer = s.timer.copy(remainingSeconds = left.coerceAtLeast(0)))
                }
            }
            if (left <= 0) {
                _uiState.update { s ->
                    s.copy(timer = s.timer.copy(isRunning = false, isFinished = true, remainingSeconds = 0))
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timer = it.timer.copy(isRunning = false, isPaused = true)) }
    }

    fun resumeTimer() = startTimer()

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timer = FocusTimerState()) }
    }

    fun cancelActiveTimer() {
        resetTimerForCurrentStep()
    }

    fun currentStep(): Step? {
        val steps = _uiState.value.steps
        val index = _uiState.value.currentStepIndex
        return steps.getOrNull(index)
    }

    private fun persistSession() {
        val id = recipeId ?: return
        val state = _uiState.value
        if (state.phase == FocusPhase.COMPLETE) return
        val session = FocusSession(
            recipeId = id,
            currentStepIndex = state.currentStepIndex,
            completedStepIds = state.completedStepIds,
            startedAtEpochMs = sessionStartedAt,
            elapsedMs = state.elapsedMs + (System.currentTimeMillis() - sessionStartedAt),
            keepScreenOn = state.keepScreenOn,
        )
        viewModelScope.launch {
            runCatching { focusSessionRepository.save(session) }
        }
    }

    override fun onCleared() {
        cancelTimer()
        super.onCleared()
    }

    companion object {
        const val ARG_RECIPE_ID = "recipeId"
    }
}
