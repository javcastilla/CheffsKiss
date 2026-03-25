package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.*
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

class CreateRecipeCommand(
    private val recipePort: RecipePort,
    private val recipeInput: RecipeInput
) : Command {

    override suspend fun execute() {
        require(recipeInput.title().isNotBlank()) { "Title cannot be empty" }
        require(recipeInput.duration() > Duration.ZERO) { "Duration is not valid" }
        require(recipeInput.stepRows().isNotEmpty()) { "Steps cannot be empty" }

        // 1. Construimos Recipe (ya no tiene steps/lines dentro)
        val recipe = Recipe(
            id       = UUID.randomUUID(),
            title    = recipeInput.title(),
            duration = recipeInput.duration(),
            tags     = recipeInput.tags(),
            image    = recipeInput.image(),
            user     = recipeInput.user()
        )

        // 2. Creamos el RecipeState — lo necesitan Step y RecipeLine
        val recipeState = RecipeState(
            timestamp     = Instant.now(),
            recipeStatus  = RecipeStatus.CREATED,
            recipe        = recipe
        )

        // 3. Construimos Steps ligados al RecipeState
        val steps = recipeInput.stepRows().mapIndexed { index, row ->
            Step(
                id          = UUID.randomUUID(),
                description = row.description,
                duration    = row.duration,
                cardinal    = index + 1,
                recipeState = recipeState
            )
        }

        // 4. Construimos RecipeLines ligadas al RecipeState
        val recipeLines = recipeInput.ingredientRows().map { row ->
            RecipeLine(
                id          = UUID.randomUUID(),
                amount      = row.amount,
                measurement = row.measurement,
                ingredient  = row.ingredient,
                recipeState = recipeState
            )
        }

        recipePort.createRecipe(recipe, steps, recipeLines)
    }
}

// ── Datos crudos que vienen de la UI ─────────────────────────────────────────

data class StepInputRow(
    val description: String,
    val duration: Duration
)

data class IngredientInputRow(
    val amount: Int,
    val measurement: Measurement,
    val ingredient: Ingredient
)

// ── Interfaz que implementa el ViewModel ──────────────────────────────────────

interface RecipeInput {
    fun user(): User
    fun title(): String
    fun duration(): Duration
    fun stepRows(): List<StepInputRow>
    fun ingredientRows(): List<IngredientInputRow>
    fun tags(): List<String>
    fun image(): String
}