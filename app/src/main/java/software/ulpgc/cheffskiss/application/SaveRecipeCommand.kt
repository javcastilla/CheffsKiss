package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import java.util.UUID

class SaveRecipeCommand(private val recipePort : RecipePort,private val saveRecipeInput:SaveRecipeInput) : Command {
    override suspend fun execute() {
        recipePort.saveRecipe(createSavedRecipe(saveRecipeInput))
    }
    private fun createSavedRecipe(saveRecipeInput:SaveRecipeInput): SavedRecipe {
        return SavedRecipe(
            userId = saveRecipeInput.userId(),
            recipeId = saveRecipeInput.recipeId()
        )
    }
}

interface SaveRecipeInput {
    fun recipeId(): UUID
    fun userId(): UUID
}