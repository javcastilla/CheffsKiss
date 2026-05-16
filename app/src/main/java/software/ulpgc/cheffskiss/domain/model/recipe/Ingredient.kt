package software.ulpgc.cheffskiss.domain.model.recipe

import software.ulpgc.cheffskiss.domain.vo.IngredientCategory
import java.net.URI
import java.util.UUID


data class Ingredient(
    val id: UUID,
    val name: String,
    val normalizedName: String = "",
    val image: URI? = null,
    val category: String = "",
    val subcategory: String = "",
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val categories: List<IngredientCategory> = emptyList(),
)