package software.ulpgc.cheffskiss.ui.screen.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.focus.FocusPhase
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.ui.FocusModeViewModel
import software.ulpgc.cheffskiss.ui.components.SaveRecipeToListHost

@Composable
fun FocusModeScreen(
    viewModel: FocusModeViewModel,
    onExit: () -> Unit,
    onViewRecipeDetail: () -> Unit,
    seedRecipe: Recipe? = null,
    seedLines: List<RecipeLine> = emptyList(),
    seedSteps: List<Step> = emptyList(),
) {
    val state by viewModel.uiState.collectAsState()
    val savePickerState by viewModel.savePickerState.collectAsState()
    val view = LocalView.current

    LaunchedEffect(seedRecipe?.id, seedLines.size, seedSteps.size) {
        if (seedRecipe != null && seedSteps.isNotEmpty()) {
            viewModel.bootstrap(seedRecipe, seedLines, seedSteps)
        } else {
            viewModel.ensureNetworkLoad()
        }
    }

    DisposableEffect(state.keepScreenOn, state.timer.isRunning, state.phase) {
        view.keepScreenOn = state.keepScreenOn ||
            state.timer.isRunning ||
            state.phase == FocusPhase.STEP
        onDispose { view.keepScreenOn = false }
    }

    val handleBack: () -> Unit = {
        when (state.phase) {
            FocusPhase.COMPLETE -> onExit()
            else -> viewModel.requestExit()
        }
    }

    BackHandler(onBack = handleBack)

    SaveRecipeToListHost(
        pickerState = savePickerState,
        onDismiss = viewModel::closeSavePicker,
        onSelect = viewModel::selectSaveDestination,
        onConfirm = viewModel::confirmSaveToList,
        onConsumeMessage = viewModel::consumeSavePickerMessage,
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null -> {
                    FocusErrorState(message = state.error!!, onExit = onExit)
                }
                state.recipe != null -> {
                    AnimatedContent(
                        targetState = state.phase,
                        transitionSpec = {
                            when {
                                initialState == FocusPhase.INTRO && targetState == FocusPhase.STEP ->
                                    (fadeIn(tween(FocusMotion.PhaseEnter)) +
                                        slideInVertically(tween(FocusMotion.PhaseEnter)) { it / 5 })
                                        .togetherWith(fadeOut(tween(FocusMotion.Micro)))
                                targetState == FocusPhase.COMPLETE ->
                                    fadeIn(tween(FocusMotion.Standard))
                                        .togetherWith(fadeOut(tween(FocusMotion.Micro)))
                                else ->
                                    fadeIn(tween(FocusMotion.Micro))
                                        .togetherWith(fadeOut(tween(FocusMotion.Micro)))
                            }
                        },
                        label = "focusPhase",
                    ) { phase ->
                        when (phase) {
                            FocusPhase.INTRO -> FocusIntroScreen(
                                recipe = state.recipe!!,
                                lines = state.lines,
                                steps = state.steps,
                                keepScreenOn = state.keepScreenOn,
                                autoAdvanceAfterTimer = state.autoAdvanceAfterTimer,
                                resumedSession = state.resumedSession,
                                checkedIngredients = state.checkedIngredientIndices,
                                onKeepScreenOnChange = viewModel::setKeepScreenOn,
                                onAutoAdvanceChange = viewModel::setAutoAdvanceAfterTimer,
                                onToggleIngredient = viewModel::toggleIngredient,
                                onStart = viewModel::startCooking,
                                onExit = viewModel::requestExit,
                            )
                            FocusPhase.STEP -> {
                                val step = viewModel.currentStep()
                                if (step != null) {
                                    FocusStepScreen(
                                        recipe = state.recipe!!,
                                        step = step,
                                        stepIndex = state.currentStepIndex,
                                        totalSteps = state.steps.size,
                                        timer = state.timer,
                                        largeTextMode = state.largeTextMode,
                                        showTimerOverlay = state.showTimerCompleteOverlay,
                                        onExit = viewModel::requestExit,
                                        onToggleLargeText = viewModel::toggleLargeTextMode,
                                        onPrevious = viewModel::previousStep,
                                        onNext = viewModel::markCurrentStepCompleted,
                                        onStartTimer = viewModel::startTimer,
                                        onPauseTimer = viewModel::pauseTimer,
                                        onResumeTimer = viewModel::resumeTimer,
                                        onCancelTimer = viewModel::cancelActiveTimer,
                                        onDismissTimerOverlay = viewModel::dismissTimerOverlay,
                                        canGoPrevious = state.currentStepIndex > 0,
                                    )
                                }
                            }
                            FocusPhase.COMPLETE -> FocusCompleteScreen(
                                recipeTitle = state.recipe!!.title,
                                onSaveToList = viewModel::openSavePicker,
                                onExit = onExit,
                            )
                        }
                    }
                }
            }

            FocusExitBottomSheet(
                visible = state.showExitSheet,
                currentStep = state.currentStepIndex,
                totalSteps = state.steps.size.coerceAtLeast(1),
                onContinue = viewModel::continueCooking,
                onSaveAndExit = {
                    viewModel.exitAndSaveProgress()
                    onExit()
                },
                onExitWithoutSaving = { viewModel.exitWithoutSaving(onExit) },
            )
        }
    }
}

@Composable
private fun FocusErrorState(message: String, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            Button(onClick = onExit) {
                Text("Go back")
            }
        }
    }
}
