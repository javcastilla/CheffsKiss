package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeLine
import software.ulpgc.cheffskiss.domain.model.RecipeState
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.store.RecipeLineStore
import software.ulpgc.cheffskiss.domain.model.store.RecipeStore
import software.ulpgc.cheffskiss.domain.model.store.StepStore

class FirebaseRecipeService(
    private val recipeStore: RecipeStore,
    private val stepStore: StepStore,
    private val recipeLineStore: RecipeLineStore
) : RecipePort {

    private val db get() = Firebase.firestore

    override suspend fun createRecipe(recipe: Recipe, steps: List<Step>, lines: List<RecipeLine>) {
        // 1. DOMINIO (inmutable, solo memoria)
        val state = steps.firstOrNull()?.recipeState ?: return
        recipeStore.save(state)
        steps.forEach { stepStore.save(it) }
        lines.forEach { recipeLineStore.save(it) }

        // 2. FIRESTORE (solo estado actual, simple)
        db.collection("recipes")
            .document(recipe.id.toString())
            .set(currentRecipeMap(recipe, state, steps, lines))
            .await()
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private fun currentRecipeMap(
        recipe: Recipe,
        state: RecipeState,
        steps: List<Step>,
        lines: List<RecipeLine>
    ): Map<String, Any?> = mapOf(
        "title" to recipe.title,
        "durationMs" to recipe.duration.inWholeMilliseconds,
        "tags" to recipe.tags,
        "image" to recipe.image,
        "userId" to recipe.user.id.toString(),
        "status" to state.recipeStatus.name,
        "updatedAt" to Timestamp.now(),
        // Arrays planos para el estado actual
        "steps" to steps.map { step ->
            mapOf(
                "id" to step.id.toString(),
                "description" to step.description,
                "durationMs" to step.duration.inWholeMilliseconds,
                "cardinal" to step.cardinal
            )
        },
        "ingredients" to lines.map { line ->
            mapOf(
                "id" to line.id.toString(),
                "amount" to line.amount,
                "measurement" to line.measurement.name,
                "ingredientId" to line.ingredient.id.toString(),
                "ingredientName" to line.ingredient.name,  // denormalizado para UX
                "ingredientImage" to line.ingredient.image
            )
        }
    )
}
