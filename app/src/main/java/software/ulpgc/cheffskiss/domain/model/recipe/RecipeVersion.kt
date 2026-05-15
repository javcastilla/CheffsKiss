package software.ulpgc.cheffskiss.domain.model.recipe

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.enum.RecipeStatus
import java.util.UUID

data class RecipeVersion(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Clock.System.now(),
    val recipe: Recipe,
    val status: RecipeStatus,
) {
    fun update(recipe: Recipe): RecipeVersion = copy(recipe = recipe, timestamp = Clock.System.now())
    fun withStatus(status: RecipeStatus): RecipeVersion = copy(status = status)
}