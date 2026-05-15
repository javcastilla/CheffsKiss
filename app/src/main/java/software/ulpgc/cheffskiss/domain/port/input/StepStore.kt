package software.ulpgc.cheffskiss.domain.port.input

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Step

interface StepStore {
    fun stepsOf(recipe: Recipe): Flow<List<Step>>
}