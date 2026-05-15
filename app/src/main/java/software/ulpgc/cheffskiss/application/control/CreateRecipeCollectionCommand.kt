package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import java.util.UUID

class CreateRecipeCollectionCommand(
    private val input: CreateRecipeCollectionInput,
    private val port: RecipeCollectionRepository) : Command {
    override suspend fun execute() {
        port.create(RecipeCollection(
            userId = input.userId(),
            name = input.name(),
            recipes = emptyList()
        ))
    }
}
interface CreateRecipeCollectionInput{
    fun userId(): UUID
    fun name(): String
}