package software.ulpgc.cheffskiss.domain.model

import java.time.Instant
import java.util.UUID

data class RecipeState(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant,
    val recipeStatus: RecipeStatus,
    val recipe: Recipe
)
