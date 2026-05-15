package software.ulpgc.cheffskiss.domain.model.recipe

import software.ulpgc.cheffskiss.domain.vo.IngredientCategory
import java.net.URI
import java.util.UUID


data class Ingredient(
    val id: UUID,
    val name: String,
    val image: URI? = null,
    val categories: List<IngredientCategory> = emptyList(),
)