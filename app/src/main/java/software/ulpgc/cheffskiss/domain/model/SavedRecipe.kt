package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Instant
import java.util.UUID
import kotlinx.datetime.Clock

data class SavedRecipe(
    val userId: UUID,
    val recipeId: UUID,
    val savedAt: Instant = Clock.System.now()
)