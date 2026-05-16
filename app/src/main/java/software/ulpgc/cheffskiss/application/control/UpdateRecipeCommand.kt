package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.enum.RecipeStatus
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeVersion

class UpdateRecipeCommand(
    private val recipeRepository: RecipeRepository,
    private val current: Recipe,
    private val recipeInput: RecipeInput,
) : Command {
    override suspend fun execute() {
        val image = recipeInput.image().let {
            if (it.isNotBlank()) {
                try { java.net.URI(it) } catch (_: Exception) { null }
            } else null
        }

        val updated = current.copy(
            title       = recipeInput.title(),
            description = recipeInput.description(),
            duration    = recipeInput.duration(),
            tags        = recipeInput.tags(),
            image       = image ?: current.image,
            servings    = recipeInput.servings(),
        ).nextVersion()

        recipeRepository.updateRecipe(
            recipe = updated,
            lines  = recipeInput.lines(),
            steps  = recipeInput.steps(),
            versionSnapshot = RecipeVersion(recipe = current, status = RecipeStatus.PUBLISHED),
        )
    }
}
