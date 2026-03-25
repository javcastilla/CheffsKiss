package software.ulpgc.cheffskiss.domain.model.store

import software.ulpgc.cheffskiss.domain.model.RecipeState
import software.ulpgc.cheffskiss.domain.model.Step
import java.util.UUID
import java.util.stream.Stream

class StepStore {
    private val values: MutableMap<UUID, MutableList<Step>> = HashMap()

    fun save(step: Step) {
        values.getOrPut(step.recipeState.recipe.id) { mutableListOf() }.add(step)
    }

    fun of(recipeState: RecipeState): Stream<Step> {
        return values[recipeState.recipe.id]?.stream()
            ?.filter { it.recipeState.id == recipeState.id } ?: Stream.empty()
    }

    fun history(recipeId: UUID): Stream<Step> {
        return values[recipeId]?.stream() ?: Stream.empty()
    }
}

