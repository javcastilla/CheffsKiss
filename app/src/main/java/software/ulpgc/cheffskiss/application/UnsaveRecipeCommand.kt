package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import java.util.UUID

class UnsaveRecipeCommand(
    private val recipePort: RecipePort,
    private val userId: String,   // Firebase UID
    private val recipeId: UUID
) : Command {
    override suspend fun execute() {
        recipePort.deleteSavedRecipe(
            SavedRecipe(
                userId   = UUID.nameUUIDFromBytes(userId.toByteArray()),
                recipeId = recipeId
            )
        )
    }
}
