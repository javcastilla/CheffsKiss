package software.ulpgc.cheffskiss.domain.port.input

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine

interface RecipeLineStore {
    fun linesOf(recipe: Recipe): Flow<List<RecipeLine>>
}