package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class GetRecipeCollectionQuery(private val port: RecipeCollectionRepository)  {
    operator fun invoke(userId: UUID): Flow<List<RecipeCollection>> = port.get(userId)
}

