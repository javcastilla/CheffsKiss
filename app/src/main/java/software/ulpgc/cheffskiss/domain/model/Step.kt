package software.ulpgc.cheffskiss.domain.model

import java.util.UUID
import kotlin.time.Duration

data class Step(
    val id: UUID,
    val description: String,
    val duration: Duration,
    val cardinal: Int,
    val recipeState: RecipeState
)
