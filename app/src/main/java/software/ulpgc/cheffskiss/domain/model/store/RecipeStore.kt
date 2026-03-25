package software.ulpgc.cheffskiss.domain.model.store

import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeState
import software.ulpgc.cheffskiss.domain.model.RecipeStatus
import software.ulpgc.cheffskiss.domain.model.User
import java.util.UUID
import java.util.stream.Stream

class RecipeStore {
    private val values: MutableMap<UUID, MutableList<RecipeState>> = HashMap()

    fun save(recipeState: RecipeState) {
        values.getOrPut(recipeState.recipe.id) { mutableListOf() }
            .add(recipeState)
    }

    fun of(user: User): Stream<Recipe> {
        return values.values
            .stream()
            .map { states -> states.last() }
            .filter { it.recipe.user == user }
            .filter { it.recipeStatus == RecipeStatus.CREATED }
            .map { it.recipe }
    }

    fun stateOf(recipe: Recipe): RecipeState? {
        return values[recipe.id]?.last();
    }

    fun history(recipeId: UUID): Stream<RecipeState> {
        return values[recipeId]?.stream() ?: Stream.empty()
    }
}
