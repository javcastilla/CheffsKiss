package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import java.util.UUID
import kotlin.time.Duration

class CreateRecipeCommand(
    private val recipePort: RecipePort,
    private val recipeInput: RecipeInput
) : Command {
    override suspend fun execute() {
        val recipe = Recipe(
            id          = recipeInput.id(),
            author      = recipeInput.author(),
            title       = recipeInput.title(),
            description = recipeInput.description(),
            duration    = recipeInput.duration(),
            ingredients = recipeInput.ingredients(),
            steps       = recipeInput.steps(),
            tags        = recipeInput.tags(),
            image       = recipeInput.image(),
            servings    = recipeInput.servings()
        )
        recipePort.createRecipe(recipe)
    }
}

interface RecipeInput {
    fun id(): UUID
    fun author(): String
    fun title(): String
    fun description(): String
    fun servings(): Int
    fun duration(): Duration
    fun ingredients(): List<String>
    fun steps(): List<Step>
    fun tags(): List<String>
    fun image(): String
}