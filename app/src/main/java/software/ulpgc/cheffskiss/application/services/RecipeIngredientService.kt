package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.enum.Measurement
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.IngredientStore
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.store.CachingIngredientStore
import java.util.UUID

data class IngredientDraft(
    val name: String,
    val amount: String,
    val unit: String,
)

class RecipeIngredientService(
    firebaseReader: FirebaseRecipeReader = FirebaseRecipeReader(),
) {
    private val ingredientStore: IngredientStore = CachingIngredientStore(firebaseReader)
    private val reader = firebaseReader

    suspend fun resolveLines(drafts: List<IngredientDraft>): List<RecipeLine> {
        return drafts
            .filter { it.name.isNotBlank() }
            .map { draft ->
                val measurement = runCatching { Measurement.valueOf(draft.unit.uppercase()) }
                    .getOrDefault(Measurement.UNIT)
                val ingredient = reader.findOrCreateByName(draft.name.trim())
                val line = RecipeLine(
                    id = UUID.randomUUID(),
                    amount = draft.amount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    ingredient = ingredient,
                    measurement = measurement,
                )
                ingredientStore.ingredientOf(line)
                line
            }
    }
}
