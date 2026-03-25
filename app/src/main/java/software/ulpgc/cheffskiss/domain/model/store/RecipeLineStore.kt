package software.ulpgc.cheffskiss.domain.model.store

import software.ulpgc.cheffskiss.domain.model.RecipeLine
import software.ulpgc.cheffskiss.domain.model.RecipeState
import java.util.UUID
import java.util.stream.Stream

class RecipeLineStore {
    private val values: MutableMap<UUID, MutableList<RecipeLine>> = HashMap()

    fun save(recipeLine: RecipeLine) {
        values.getOrPut(recipeLine.recipeState.recipe.id) { mutableListOf() }.add(recipeLine)
    }

    fun of(recipeState: RecipeState): Stream<RecipeLine> {
        return values[recipeState.recipe.id]?.stream()
            ?.filter { it.recipeState.id == recipeState.id } ?: Stream.empty()
    }

    fun history(recipeId: UUID): Stream<RecipeLine> {
        return values[recipeId]?.stream() ?: Stream.empty()
    }
}
