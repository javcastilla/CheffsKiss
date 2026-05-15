package software.ulpgc.cheffskiss.application.control

import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import java.util.UUID

class UpdateRecipeCollectionCommand(private val port: RecipeCollectionRepository, private val input: UpdateRecipeCollectionInput) : Command {
    override suspend fun execute() {
        port.update(
            RecipeCollection(
                id = input.id(),
                userId = input.userId(),
                name = input.name(),
                image = input.image(),
                createdAt = input.createdAt(),
                recipes = input.recipes()
            ))
    }
}
interface UpdateRecipeCollectionInput{
    fun id(): UUID
    fun userId(): UUID
    fun name(): String
    fun image(): String
    fun createdAt(): Instant
    fun recipes(): List<UUID>
}