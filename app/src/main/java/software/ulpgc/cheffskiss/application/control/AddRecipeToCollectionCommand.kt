package software.ulpgc.cheffskiss.application.control

import kotlinx.coroutines.flow.first
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.control.Command
import java.util.UUID

class AddRecipeToCollectionCommand(
    private val port: RecipeCollectionRepository,
    private val collectionId: UUID,
    private val userId: UUID,
    private val recipeId: UUID,
) : Command {

    override suspend fun execute() {
        val collection = port.get(userId).first()
            .firstOrNull { it.id == collectionId }
            ?: error("Collection not found")
        if (recipeId in collection.recipes) return

        UpdateRecipeCollectionCommand(
            port = port,
            input = object : UpdateRecipeCollectionInput {
                override fun id() = collection.id
                override fun userId() = collection.userId
                override fun name() = collection.name
                override fun image() = collection.image
                override fun createdAt() = collection.createdAt
                override fun recipes() = collection.recipes + recipeId
            },
        ).execute()
    }
}
