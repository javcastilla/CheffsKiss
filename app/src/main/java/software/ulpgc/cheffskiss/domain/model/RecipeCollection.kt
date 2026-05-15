package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

data class RecipeCollection(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val description: String,
    val image: String?,
    val recipes: List<UUID>)
