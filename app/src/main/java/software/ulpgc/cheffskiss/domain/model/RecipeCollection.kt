package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

data class RecipeCollection(
    val id: UUID=UUID.randomUUID(),
    val userId: UUID,
    val name: String,
    val image: String = "",
    val createdAt: Instant = Clock.System.now(),
    val recipes: List<UUID>)
