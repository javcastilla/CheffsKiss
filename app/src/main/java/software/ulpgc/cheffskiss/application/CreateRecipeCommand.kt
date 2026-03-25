package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import java.util.UUID
import kotlin.time.Duration

class CreateRecipeCommand( private val recipePort: RecipePort, private val recipeInput: RecipeInput) : Command {
    override suspend fun execute() {
        require(recipeInput.title().isNotBlank()){ "Title cannot be empty" }
        require(recipeInput.author() != UUID(0, 0)){ "Author is not valid" }
        require(recipeInput.duration() > Duration.ZERO){ "Duration is not valid" }
        require(recipeInput.ingredients().isNotEmpty()){ "Ingredients cannot be empty" }
        require(recipeInput.steps().isNotEmpty()){ "Steps cannot be empty" }
        recipePort.createRecipe(Recipe(UUID.randomUUID(), recipeInput.author(), recipeInput.title(), recipeInput.duration(), recipeInput.ingredients(), recipeInput.steps(), recipeInput.tags(), recipeInput.image()))
    }
}

interface RecipeInput {
    fun author() : UUID
    fun title(): String
    fun duration(): Duration
    fun ingredients(): List<String>
    fun steps(): List<Step>
    fun tags(): List<String>
    fun image(): String
}
