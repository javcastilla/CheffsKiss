package software.ulpgc.cheffskiss.domain.port.input

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.User

interface RecipeReader {
    fun getAll(): Flow<List<Recipe>>
    suspend fun getById(id: String): Recipe?
    fun search(query: String): Flow<List<Recipe>>
    suspend fun from(user: User): Flow<Recipe>
}