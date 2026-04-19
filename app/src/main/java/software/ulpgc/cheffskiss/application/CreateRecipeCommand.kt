package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
import java.util.UUID
import kotlin.time.Duration

class CreateRecipeCommand(
    private val recipeRepository: RecipeRepository,
    private val recipeInput: RecipeInput
) : Command {
    override suspend fun execute() {
        val recipe = Recipe(
            id          = recipeInput.id(),
            author      = recipeInput.author(),
            title       = recipeInput.title(),
            description = recipeInput.description(),
            duration    = recipeInput.duration(),
            tags        = recipeInput.tags(),
            image       = recipeInput.image(),
            servings    = recipeInput.servings()
        )
        recipeRepository.createRecipe(recipe, recipeInput.lines(), recipeInput.steps())
    }
}

interface RecipeInput {
    fun id(): UUID
    fun author(): String
    fun title(): String
    fun description(): String
    fun servings(): Int
    fun duration(): Duration
    fun lines(): List<RecipeLine>
    fun steps(): List<Step>
    fun tags(): List<String>
    fun image(): String
}