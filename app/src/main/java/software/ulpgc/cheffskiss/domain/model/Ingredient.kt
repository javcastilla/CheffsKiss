package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

data class Ingredient(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val normalizedName: String = "",
    val category: String = "",
    val subcategory: String = "",
    val image: String = "",
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)