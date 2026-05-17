package software.ulpgc.cheffskiss.ui.screen.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import software.ulpgc.cheffskiss.ui.components.RecipeAsyncImage
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.focus.FocusCapabilities
import software.ulpgc.cheffskiss.domain.model.focus.FocusPhase
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.ui.FocusModeViewModel
import software.ulpgc.cheffskiss.ui.FocusTimerState
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKSecondary
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnPrimary
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import software.ulpgc.cheffskiss.ui.theme.Surface

@Composable
fun FocusModeScreen(
    viewModel: FocusModeViewModel,
    onExit: () -> Unit,
    onViewRecipeDetail: () -> Unit,
    seedRecipe: Recipe? = null,
    seedLines: List<RecipeLine> = emptyList(),
    seedSteps: List<Step> = emptyList(),
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current

    LaunchedEffect(seedRecipe?.id, seedLines.size, seedSteps.size) {
        if (seedRecipe != null && seedSteps.isNotEmpty()) {
            viewModel.bootstrap(seedRecipe, seedLines, seedSteps)
        } else {
            viewModel.ensureNetworkLoad()
        }
    }

    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val handleBack: () -> Unit = {
        when (state.phase) {
            FocusPhase.COMPLETE -> onExit()
            else -> viewModel.requestExit()
        }
    }

    BackHandler(onBack = handleBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            state.error != null -> {
                FocusErrorState(message = state.error!!, onExit = onExit)
            }
            state.recipe != null -> {
                when (state.phase) {
                    FocusPhase.INTRO -> FocusIntroContent(
                        recipe = state.recipe!!,
                        capabilities = state.capabilities,
                        lines = state.lines,
                        steps = state.steps,
                        keepScreenOn = state.keepScreenOn,
                        resumedSession = state.resumedSession,
                        usingCachedData = state.usingCachedData,
                        onKeepScreenOnChange = viewModel::setKeepScreenOn,
                        onStart = viewModel::startCooking,
                        onExit = { viewModel.requestExit() },
                    )
                    FocusPhase.STEP -> {
                        val step = viewModel.currentStep()
                        if (step != null) {
                            FocusStepContent(
                                recipeTitle = state.recipe!!.title,
                                step = step,
                                stepIndex = state.currentStepIndex,
                                totalSteps = state.steps.size,
                                timer = state.timer,
                                onExit = { viewModel.requestExit() },
                                onPrevious = viewModel::previousStep,
                                onNext = viewModel::nextStep,
                                onStartTimer = viewModel::startTimer,
                                onPauseTimer = viewModel::pauseTimer,
                                onResumeTimer = viewModel::resumeTimer,
                                onCancelTimer = viewModel::cancelActiveTimer,
                                onMarkCompleted = viewModel::markCurrentStepCompleted,
                                canGoPrevious = state.currentStepIndex > 0,
                                canGoNext = true,
                            )
                        }
                    }
                    FocusPhase.COMPLETE -> FocusCompleteContent(
                        recipeTitle = state.recipe!!.title,
                        elapsedMs = state.elapsedMs,
                        isSaved = isSaved,
                        onToggleSave = onToggleSave,
                        onViewDetail = onViewRecipeDetail,
                        onExit = onExit,
                    )
                }
            }
        }

        if (state.showExitDialog) {
            FocusExitDialog(
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
private fun FocusIntroContent(
    recipe: Recipe,
    capabilities: FocusCapabilities?,
    lines: List<RecipeLine>,
    steps: List<Step>,
    keepScreenOn: Boolean,
    resumedSession: Boolean,
    usingCachedData: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        FocusHeader(
            title = "Preparación",
            subtitle = recipe.title,
            stepLabel = null,
            progress = null,
            onExit = onExit,
        )
        Spacer(Modifier.height(16.dp))
        if (usingCachedData) {
            Text(
                "Sin conexión: usando los datos ya cargados de la receta.",
                fontSize = 13.sp,
                color = CKSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (resumedSession) {
            Text(
                "Tienes una preparación guardada. Puedes continuar donde la dejaste.",
                fontSize = 13.sp,
                color = CKSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Antes de empezar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Primary,
                )
                Text(
                    "Lee todos los ingredientes y ten los utensilios listos.",
                    fontSize = 14.sp,
                    color = CKOnSurfaceVariant,
                    lineHeight = 20.sp,
                )
                capabilities?.let { caps ->
                    HorizontalDivider(color = CKSurfaceVariant)
                    FocusStatRow("Tiempo total", "${caps.totalDurationMinutes} min")
                    FocusStatRow("Pasos", "${caps.stepCount}")
                    if (caps.timedStepCount > 0) FocusStatRow("Con temporizador", "${caps.timedStepCount}")
                    if (caps.mediaStepCount > 0) FocusStatRow("Con foto", "${caps.mediaStepCount}")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (lines.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingredientes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnSurface)
                    lines.take(8).forEach { line ->
                        Text(
                            buildString {
                                append(line.amount)
                                line.measurement?.let { append(" ${it.name.lowercase()}") }
                                line.ingredient?.name?.let { append(" $it") }
                            }.trim(),
                            fontSize = 14.sp,
                            color = CKOnSurfaceVariant,
                        )
                    }
                    if (lines.size > 8) {
                        Text("+${lines.size - 8} más", fontSize = 12.sp, color = CKOnSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (steps.isNotEmpty()) {
            Text("Vista previa del flujo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OnSurface)
            Spacer(Modifier.height(8.dp))
            steps.take(4).forEach { step ->
                Text(
                    "${step.cardinal}. ${step.description.take(60)}${if (step.description.length > 60) "…" else ""}",
                    fontSize = 13.sp,
                    color = CKOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            if (steps.size > 4) {
                Text("… y ${steps.size - 4} pasos más", fontSize = 12.sp, color = CKOnSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mantener pantalla activa", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSurface)
                Text("Evita que se apague mientras cocinas", fontSize = 12.sp, color = CKOnSurfaceVariant)
            }
            Switch(
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
                colors = SwitchDefaults.colors(checkedThumbColor = OnPrimary, checkedTrackColor = Primary),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            enabled = steps.isNotEmpty(),
        ) {
            Text(
                if (resumedSession) "Continuar preparación" else "Iniciar paso 1",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FocusStepContent(
    recipeTitle: String,
    step: Step,
    stepIndex: Int,
    totalSteps: Int,
    timer: FocusTimerState,
    onExit: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onMarkCompleted: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
) {
    val progress = if (totalSteps > 0) (stepIndex + 1f) / totalSteps else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        FocusHeader(
            title = recipeTitle,
            subtitle = null,
            stepLabel = "Paso ${stepIndex + 1} de $totalSteps",
            progress = progress,
            onExit = onExit,
        )
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (step.hasMedia()) {
                RecipeAsyncImage(
                    url = step.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Paso ${step.cardinal}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CKSecondary,
                    )
                    Text(
                        step.description,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                        lineHeight = 30.sp,
                    )
                    if (step.hasTimer()) {
                        HorizontalDivider(color = CKSurfaceVariant)
                        if (timer.isRunning || timer.isPaused || timer.isFinished) {
                            FocusActiveTimer(
                                timer = timer,
                                onPause = onPauseTimer,
                                onResume = onResumeTimer,
                                onCancel = onCancelTimer,
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Text(
                                        formatDuration(step.timerSeconds()),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary,
                                    )
                                }
                                Button(
                                    onClick = onStartTimer,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Iniciar temporizador", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Anterior", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onMarkCompleted,
                modifier = Modifier.weight(1.2f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                Text(
                    if (stepIndex >= totalSteps - 1) "Terminar" else "Siguiente",
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FocusActiveTimer(
    timer: FocusTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val progress = if (timer.totalSeconds > 0) {
        1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
    } else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            formatDuration(timer.remainingSeconds),
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Primary,
            textAlign = TextAlign.Center,
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Primary,
            trackColor = CKSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (timer.isRunning) {
                OutlinedButton(onClick = onPause, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Pausar")
                }
            } else if (timer.isPaused && !timer.isFinished) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Reanudar")
                }
            }
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = CKOnSurfaceVariant)
            }
        }
        if (timer.isFinished) {
            Text("¡Tiempo!", fontWeight = FontWeight.Bold, color = CKSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun FocusCompleteContent(
    recipeTitle: String,
    elapsedMs: Long,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onViewDetail: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("¡Receta completada!", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Primary)
        Spacer(Modifier.height(8.dp))
        Text(recipeTitle, fontSize = 16.sp, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = CKOnSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                "Tiempo: ${formatElapsed(elapsedMs)}",
                fontSize = 14.sp,
                color = CKOnSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onToggleSave,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
        ) {
            Text(if (isSaved) "Guardada" else "Guardar como favorita", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onViewDetail,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Ver receta completa", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onExit) {
            Text("Volver al inicio", color = CKOnSurfaceVariant)
        }
    }
}

@Composable
private fun FocusExitDialog(
    onContinue: () -> Unit,
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
) {
    Dialog(onDismissRequest = onContinue) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "¿Salir de la preparación?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Primary,
                )
                Text(
                    "Tu progreso se puede guardar para continuar después.",
                    fontSize = 14.sp,
                    color = CKOnSurfaceVariant,
                    lineHeight = 20.sp,
                )
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                ) {
                    Text("Continuar cocinando", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSaveAndExit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Salir y guardar progreso", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onExitWithoutSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Salir sin guardar", color = CKOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FocusHeader(
    title: String,
    subtitle: String?,
    stepLabel: String?,
    progress: Float?,
    onExit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(44.dp)
                    .background(Surface, CircleShape),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Salir", tint = OnSurface)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OnSurface, maxLines = 1)
                subtitle?.let {
                    Text(it, fontSize = 12.sp, color = CKOnSurfaceVariant, maxLines = 1)
                }
            }
        }
        stepLabel?.let {
            Text(it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CKOnSurfaceVariant)
        }
        progress?.let { value ->
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = CKSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FocusStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = CKOnSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
    }
}

@Composable
private fun FocusErrorState(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExit) { Text("Volver") }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds) else "${seconds}s"
}

private fun formatElapsed(ms: Long): String {
    val totalMinutes = (ms / 1000 / 60).coerceAtLeast(0)
    return if (totalMinutes < 60) "$totalMinutes min" else "${totalMinutes / 60} h ${totalMinutes % 60} min"
}
