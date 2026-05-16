package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.enum.Measurement
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.IngredientStore
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.store.CachingIngredientStore
import java.util.UUID

data class IngredientDraft(
    val ingredientId: UUID?,
    val name: String,
    val amount: String,
    val measurement: Measurement = Measurement.UNIT,
)

class RecipeIngredientService(
    firebaseReader: FirebaseRecipeReader = FirebaseRecipeReader(),
    private val catalog: IngredientCatalogService = IngredientCatalogService(firebaseReader),
) {
    private val ingredientStore: IngredientStore = CachingIngredientStore(firebaseReader)
    private val reader = firebaseReader

    suspend fun resolveLines(drafts: List<IngredientDraft>): List<RecipeLine> {
        return drafts
            .filter { it.ingredientId != null || it.name.isNotBlank() }
            .map { draft ->
                val ingredient = resolveIngredient(draft)
                val line = RecipeLine(
                    id = UUID.randomUUID(),
                    amount = draft.amount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    ingredient = ingredient,
                    measurement = draft.measurement,
                )
                ingredientStore.ingredientOf(line)
                line
            }
    }

    private suspend fun resolveIngredient(draft: IngredientDraft): Ingredient {
        draft.ingredientId?.let { id ->
            catalog.getById(id.toString())?.let { return it }
            reader.getIngredientById(id.toString())?.let { return it }
        }
        val byName = draft.name.trim().takeIf { it.isNotBlank() }?.let { reader.findByName(it) }
        return byName ?: error("Select an ingredient from the catalog")
    }
}
