package software.ulpgc.cheffskiss.ui.screen.focus

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import software.ulpgc.cheffskiss.ui.components.RecipeAsyncImage

object FocusMotion {
    const val Micro = 120
    const val Standard = 250
    const val PhaseEnter = 260
    const val StepSlide = 280
    const val Hero = 550
    const val StepCompletePulse = 380
}

@Composable
fun focusScrimStrong(): Color =
    MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f)

@Composable
fun focusScrimSoft(): Color =
    MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)

@Composable
fun focusGlassSurface(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)

@Composable
fun focusGlassBorder(): Color =
    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

@Composable
fun focusGradientTop(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to scheme.scrim.copy(alpha = 0.78f),
        0.06f to scheme.scrim.copy(alpha = 0.62f),
        0.18f to scheme.scrim.copy(alpha = 0.45f),
        0.5f to scheme.scrim.copy(alpha = 0.28f),
        1f to scheme.scrim.copy(alpha = 0.1f),
    )
}

@Composable
fun focusGradientBottom(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to Color.Transparent,
        0.55f to scheme.scrim.copy(alpha = 0.2f),
        1f to scheme.scrim.copy(alpha = 0.72f),
    )
}

@Composable
fun FocusImmersiveBackground(
    imageUrl: Any?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "kenBurns")
    val scale by infinite.animateFloat(
        initialValue = 1.04f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kenBurnsScale",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (imageUrl != null) {
            RecipeAsyncImage(
                url = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Subtle depth without external blur libs
                            alpha = 0.92f
                        }
                    },
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )
        }
        Box(Modifier.fillMaxSize().background(focusScrimStrong()))
        Box(
            Modifier
                .fillMaxSize()
                .background(focusGradientTop()),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(focusGradientBottom()),
        )
        content()
    }
}

@Composable
fun FocusGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(focusGlassSurface())
            .border(1.dp, focusGlassBorder(), shape),
        content = content,
    )
}

@Composable
fun FocusSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Box(Modifier.fillMaxWidth(), content = content) },
    )
}

@Composable
fun rememberFocusPulse(): Float {
    val infinite = rememberInfiniteTransition(label = "pulse")
    return infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    ).value
}

@Composable
fun focusPressScale(pressed: Boolean): Float = animateFloatAsState(
    targetValue = if (pressed) 0.96f else 1f,
    animationSpec = tween(FocusMotion.Micro),
    label = "pressScale",
).value

@Composable
fun focusBlendPrimary(progress: Float): Color {
    val scheme = MaterialTheme.colorScheme
    return lerp(scheme.surfaceVariant, scheme.primary, progress.coerceIn(0f, 1f))
}
