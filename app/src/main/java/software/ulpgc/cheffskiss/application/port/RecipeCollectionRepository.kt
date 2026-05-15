package software.ulpgc.cheffskiss.application.port

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import java.util.UUID

interface RecipeCollectionRepository {
    suspend fun create(recipeCollection: RecipeCollection)
    suspend fun update(recipeCollection: RecipeCollection)
    suspend fun delete(recipeCollectionID: UUID,userId: UUID)
    fun get(userId: UUID): Flow<List<RecipeCollection>>

}