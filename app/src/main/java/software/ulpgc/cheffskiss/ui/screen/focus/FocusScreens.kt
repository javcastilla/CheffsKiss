package software.ulpgc.cheffskiss.ui.screen.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.ui.FocusTimerState

@Composable
fun FocusIntroScreen(
    recipe: Recipe,
    lines: List<RecipeLine>,
    steps: List<Step>,
    keepScreenOn: Boolean,
    autoAdvanceAfterTimer: Boolean,
    resumedSession: Boolean,
    checkedIngredients: Set<Int>,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onToggleIngredient: (Int) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val heroImage = recipe.image?.toString() ?: steps.firstOrNull { it.hasMedia() }?.imageUrl
    val allIngredientsChecked = lines.isEmpty() || lines.indices.all { it in checkedIngredients }
    val canStart = steps.isNotEmpty() && allIngredientsChecked

    FocusImmersiveBackground(imageUrl = heroImage) {
        Column(Modifier.fillMaxSize()) {
            FocusFloatingHeader(
                recipeTitle = recipe.title,
                stepLabel = null,
                currentStep = 0,
                totalSteps = steps.size.coerceAtLeast(1),
                onExit = onExit,
                showLargeTextToggle = false,
                showProgress = false,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ready to cook",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.surface,
                    )
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.surface.copy(alpha = 0.88f),
                    )
                }

                if (lines.isNotEmpty()) {
                    FocusSurfaceCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Ingredients",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                            )
                            Text(
                                "Check off each item before you start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                            lines.forEachIndexed { index, line ->
                                FocusCheckRow(
                                    label = lineLabel(line),
                                    checked = index in checkedIngredients,
                                    onToggle = { onToggleIngredient(index) },
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                FocusSurfaceCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FocusPreferenceRow(
                            title = "Keep screen awake",
                            subtitle = "Screen stays on while you cook",
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange,
                        )
                        FocusPreferenceRow(
                            title = "Auto-advance after timer",
                            subtitle = "Move to the next step when time is up",
                            checked = autoAdvanceAfterTimer,
                            onCheckedChange = onAutoAdvanceChange,
                        )
                    }
                }

                Spacer(Modifier.height(100.dp))
            }

            FocusFloatingPrimaryCta(
                text = if (resumedSession) "Resume experience" else "Start experience",
                onClick = onStart,
                enabled = canStart,
            )
        }
    }
}

@Composable
fun FocusStepScreen(
    recipe: Recipe,
    step: Step,
    stepIndex: Int,
    totalSteps: Int,
    timer: FocusTimerState,
    largeTextMode: Boolean,
    showTimerOverlay: Boolean,
    onExit: () -> Unit,
    onToggleLargeText: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismissTimerOverlay: () -> Unit,
    canGoPrevious: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val bgImage = recipe.image?.toString()
    val instructionStyle = if (largeTextMode) {
        MaterialTheme.typography.displaySmall.copy(
            fontSize = 34.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
        )
    } else {
        MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp,
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusSwipeNavigation(
                onSwipePrevious = { if (canGoPrevious) onPrevious() },
                onSwipeNext = onNext,
            ),
    ) {
        FocusImmersiveBackground(imageUrl = bgImage) {
            Column(Modifier.fillMaxSize()) {
                FocusFloatingHeader(
                    recipeTitle = recipe.title,
                    stepLabel = "Step ${stepIndex + 1} of $totalSteps",
                    currentStep = stepIndex,
                    totalSteps = totalSteps,
                    onExit = onExit,
                    onToggleLargeText = onToggleLargeText,
                    largeTextEnabled = largeTextMode,
                )

                if (!largeTextMode && step.hasMedia()) {
                    FocusStepHeroMedia(
                        imageUrl = step.imageUrl!!,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    key(stepIndex) {
                        FocusSurfaceCard {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Step ${stepIndex + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.primary,
                                )
                                Text(
                                    step.description,
                                    style = instructionStyle,
                                    color = scheme.onSurface,
                                )
                                step.duration?.let { duration ->
                                    if (duration.inWholeSeconds > 0) {
                                        Text(
                                            "About ${formatFocusDuration(duration.inWholeSeconds)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = scheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (step.hasTimer()) {
                                    HorizontalDivider(color = scheme.outline.copy(alpha = 0.15f))
                                    FocusCircularTimer(
                                        timer = timer,
                                        onStart = onStartTimer,
                                        onPause = onPauseTimer,
                                        onResume = onResumeTimer,
                                        onCancel = onCancelTimer,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                FocusStepNavButtons(
                    onPrevious = onPrevious,
                    onNext = onNext,
                    canGoPrevious = canGoPrevious,
                    isLastStep = stepIndex >= totalSteps - 1,
                )
            }
        }

        FocusTimerCompleteOverlay(
            visible = showTimerOverlay,
            onContinue = onDismissTimerOverlay,
        )
    }
}

@Composable
fun FocusCompleteScreen(
    recipeTitle: String,
    onSaveToList: () -> Unit,
    onExit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    FocusImmersiveBackground(imageUrl = null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.7f, animationSpec = tween(FocusMotion.Hero)) +
                    fadeIn(tween(FocusMotion.Standard)),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = scheme.secondary,
                    modifier = Modifier.size(80.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "You did it!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.surface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                recipeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.surface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onSaveToList,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text("Save to list", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Done", color = scheme.surface)
            }
        }
    }
}

@Composable
private fun FocusPreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = scheme.onPrimary,
                checkedTrackColor = scheme.primary,
                uncheckedThumbColor = scheme.outline,
                uncheckedTrackColor = scheme.surfaceVariant,
            ),
        )
    }
}

private fun lineLabel(line: RecipeLine): String = buildString {
    append(line.amount)
    line.measurement?.let { append(" ${it.name.lowercase()}") }
    line.ingredient?.name?.let { append(" $it") }
}.trim()
