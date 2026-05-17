package software.ulpgc.cheffskiss.ui.screen.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import software.ulpgc.cheffskiss.ui.components.RecipeAsyncImage
import software.ulpgc.cheffskiss.ui.theme.OnPrimary
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import software.ulpgc.cheffskiss.ui.FocusTimerState
import kotlin.math.abs

@Composable
fun FocusFloatingHeader(
    recipeTitle: String,
    stepLabel: String?,
    currentStep: Int,
    totalSteps: Int,
    onExit: () -> Unit,
    onToggleLargeText: () -> Unit = {},
    largeTextEnabled: Boolean = false,
    showLargeTextToggle: Boolean = true,
    showProgress: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Exit",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            ) {
                Text(
                    recipeTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.surface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                stepLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.surface.copy(alpha = 0.78f),
                    )
                }
            }
            if (showLargeTextToggle) {
                IconButton(onClick = onToggleLargeText) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (largeTextEnabled) Primary else Surface,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Large text",
                            tint = if (largeTextEnabled) OnPrimary else OnSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(36.dp))
            }
        }
        if (showProgress) {
            FocusSegmentedStepStack(
                totalSteps = totalSteps,
                currentIndex = currentStep,
            )
        }
    }
}

@Composable
fun FocusSegmentedStepStack(
    totalSteps: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (totalSteps <= 0) return
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(totalSteps) { index ->
            val isCurrent = index == currentIndex
            val isDone = index < currentIndex
            val segmentHeight by animateDpAsState(
                targetValue = if (isCurrent) 40.dp else 30.dp,
                animationSpec = tween(FocusMotion.Standard),
                label = "segmentHeight$index",
            )
            val segmentWidth = if (isCurrent) 52.dp else 40.dp
            Box(
                modifier = Modifier
                    .width(segmentWidth)
                    .height(segmentHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isCurrent -> Primary
                            isDone -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                            else -> Surface.copy(alpha = 0.72f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone && !isCurrent) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent || isDone) OnPrimary else OnSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun FocusStepDots(
    total: Int,
    currentIndex: Int,
    completedCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total.coerceAtMost(12)) { index ->
            val isDone = index < completedCount
            val isCurrent = index == currentIndex
            val size = when {
                isCurrent -> 10.dp
                isDone -> 8.dp
                else -> 6.dp
            }
            val color = when {
                isDone -> MaterialTheme.colorScheme.secondary
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(color),
            )
            if (index < total - 1 && index < 11) {
                Box(
                    Modifier
                        .width(10.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                )
            }
        }
        if (total > 12) {
            Spacer(Modifier.width(6.dp))
            Text(
                "+${total - 12}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun FocusCircularTimer(
    timer: FocusTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isActive = timer.isRunning || timer.isPaused || timer.isFinished
    val moduleHeight by animateDpAsState(
        targetValue = if (isActive) 220.dp else 168.dp,
        animationSpec = tween(FocusMotion.Standard),
        label = "timerHeight",
    )
    val progress = if (timer.totalSeconds > 0) {
        1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(FocusMotion.Standard),
        label = "timerArc",
    )
    val urgent = timer.isRunning && timer.remainingSeconds in 1..10

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant.copy(alpha = if (isActive) 0.55f else 0.35f))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(moduleHeight.coerceAtLeast(168.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                drawArc(
                    color = scheme.surfaceVariant.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = if (urgent) scheme.secondary else scheme.primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatFocusDuration(timer.remainingSeconds),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (urgent) 40.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = scheme.onSurface,
                )
                Text(
                    when {
                        timer.isFinished -> "Done"
                        timer.isPaused -> "Paused"
                        timer.isRunning -> "Running"
                        else -> "Ready"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                timer.isFinished -> Unit
                timer.isRunning -> {
                    TextButton(onClick = onPause) { Text("Pause") }
                }
                timer.isPaused -> {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Resume")
                    }
                }
                else -> {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start timer")
                    }
                }
            }
            if (timer.isRunning || timer.isPaused) {
                TextButton(onClick = onCancel) {
                    Text("Reset", color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FocusTimerCompleteOverlay(
    visible: Boolean,
    onContinue: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FocusMotion.Standard)) + scaleIn(initialScale = 0.92f),
        exit = fadeOut(tween(FocusMotion.Micro)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    "Time's up!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha + 0.35f),
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusExitBottomSheet(
    visible: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onContinue: () -> Unit,
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onContinue,
        sheetState = sheetState,
        containerColor = scheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Leave cooking mode?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Text(
                "You're on step ${currentStep + 1} of $totalSteps. Progress can be saved to pick up later.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text("Keep cooking", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSaveAndExit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.surfaceVariant,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Text("Save & exit", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onExitWithoutSaving, modifier = Modifier.fillMaxWidth()) {
                Text("Exit without saving", color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FocusCheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = focusPressScale(pressed)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .background(
                if (checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (checked) Icons.Outlined.CheckCircleOutline else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun FocusFloatingPrimaryCta(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberFocusPulse()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = focusPressScale(pressed) * pulse
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .scale(scale)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            disabledContainerColor = scheme.surfaceVariant,
            disabledContentColor = scheme.onSurfaceVariant,
        ),
        interactionSource = interaction,
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun Modifier.focusSwipeNavigation(
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
): Modifier = pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragEnd = {
            when {
                totalDrag > 80f -> onSwipePrevious()
                totalDrag < -80f -> onSwipeNext()
            }
            totalDrag = 0f
        },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
    )
}

@Composable
fun FocusStepHeroMedia(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        RecipeAsyncImage(
            url = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    ),
                ),
        )
    }
}

@Composable
fun FocusStepNavButtons(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoPrevious: Boolean,
    isLastStep: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Surface.copy(alpha = 0.95f),
                disabledContainerColor = Surface.copy(alpha = 0.72f),
                contentColor = OnSurface,
                disabledContentColor = scheme.onSurfaceVariant,
            ),
        ) {
            Text("Back", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1.35f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
            ),
        ) {
            Text(
                if (isLastStep) "Finish" else "Next step",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

fun formatFocusDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds) else "${seconds}s"
}

fun formatFocusElapsed(ms: Long): String {
    val totalMinutes = (ms / 1000 / 60).coerceAtLeast(0)
    return if (totalMinutes < 60) "$totalMinutes min" else "${totalMinutes / 60} h ${totalMinutes % 60} min"
}
