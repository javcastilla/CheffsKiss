package software.ulpgc.cheffskiss.infrastructure.store

import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.IngredientStore
import java.util.concurrent.ConcurrentHashMap

class CachingIngredientStore(
    private val delegate: IngredientStore,
) : IngredientStore {
    private val cache = ConcurrentHashMap<String, Ingredient>()

    override suspend fun ingredientOf(recipeLine: RecipeLine): Ingredient? {
        recipeLine.ingredient?.let { ingredient ->
            cache[ingredient.id.toString()] = ingredient
            return ingredient
        }
        val ingredientId = recipeLine.id.toString()
        cache[ingredientId]?.let { return it }
        return delegate.ingredientOf(recipeLine)?.also { cache[it.id.toString()] = it }
    }

    suspend fun findById(id: String): Ingredient? {
        cache[id]?.let { return it }
        val reader = delegate as? software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
            ?: return null
        return reader.getIngredientById(id)?.also { cache[id] = it }
    }

    fun clear() = cache.clear()
}
