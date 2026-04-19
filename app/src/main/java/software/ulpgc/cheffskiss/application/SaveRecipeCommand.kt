package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import java.util.UUID

class SaveRecipeCommand(private val recipeRepository: RecipeRepository, private val input: SaveRecipeInput) : Command {
    override suspend fun execute() {
        recipeRepository.saveRecipe(
            SavedRecipe(
                userId   = UUID.nameUUIDFromBytes(input.userId().toByteArray()),
                recipeId = input.recipeId()
            )
        )
    }
}

interface SaveRecipeInput {
    fun recipeId(): UUID
    fun userId(): String   // Firebase UID — conversión a UUID ocurre en el Command
}