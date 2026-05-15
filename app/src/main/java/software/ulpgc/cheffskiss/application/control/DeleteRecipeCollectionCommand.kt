package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.control.Command
import java.util.UUID

class DeleteRecipeCollectionCommand(private val port: RecipeCollectionRepository, private val input: DeleteRecipeCollectionCommandInput) :
    Command {
    override suspend fun execute() {
        port.delete(input.id(), input.userId())
    }
}

interface DeleteRecipeCollectionCommandInput {
    fun id(): UUID
    fun userId(): UUID
}
