package software.ulpgc.cheffskiss.domain.model

import java.util.UUID


data class Ingredient(
    val id: UUID,
    val name: String,
    val image: String
)
