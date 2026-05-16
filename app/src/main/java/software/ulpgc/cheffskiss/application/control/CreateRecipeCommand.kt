package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.model.user.User
import java.util.UUID
import kotlin.time.Duration
import java.net.URI

class CreateRecipeCommand(
    private val recipeRepository: RecipeRepository,
    private val recipeInput: RecipeInput
) : Command {
    override suspend fun execute() {
        val image = recipeInput.image().let { 
            if (it.isNotBlank()) {
                try { URI(it) } catch (e: Exception) { null }
            } else null
        }
        
        val recipe = Recipe(
            id          = recipeInput.id(),
            title       = recipeInput.title(),
            description = recipeInput.description(),
            duration    = recipeInput.duration(),
            tags        = recipeInput.tags(),
            image       = image,
            servings    = recipeInput.servings(),
            creator     = recipeInput.creator(),
        )
        recipeRepository.createRecipe(recipe, recipeInput.lines(), recipeInput.steps())
    }
}

interface RecipeInput {
    fun id(): UUID
    fun creator(): User
    fun title(): String
    fun description(): String
    fun servings(): Int
    fun duration(): Duration
    fun lines(): List<RecipeLine>
    fun steps(): List<Step>
    fun tags(): List<String>
    fun image(): String
}