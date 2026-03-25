package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeLine
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.store.RecipeLineStore
import software.ulpgc.cheffskiss.domain.model.store.RecipeStore
import software.ulpgc.cheffskiss.domain.model.store.StepStore

class FirebaseRecipeService(
    private val recipeStore: RecipeStore,
    private val stepStore: StepStore,
    private val recipeLineStore: RecipeLineStore
) : RecipePort {

    override suspend fun createRecipe(
        recipe: Recipe,
        steps: List<Step>,
        recipeLines: List<RecipeLine>
    ) {
        // 1. Persistimos en Firebase
        Firebase.firestore.collection("recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap(steps, recipeLines))
            .await()

        // 2. El RecipeState ya viene construido dentro de los Steps/RecipeLines
        //    (lo creó CreateRecipeCommand). Lo recuperamos para guardarlo en el store.
        val recipeState = steps.firstOrNull()?.recipeState
            ?: recipeLines.firstOrNull()?.recipeState

        recipeState?.let { recipeStore.save(it) }
        steps.forEach { stepStore.save(it) }
        recipeLines.forEach { recipeLineStore.save(it) }
    }

    // ── Mappers a Firestore ────────────────────────────────────────────────────

    private fun Recipe.toMap(
        steps: List<Step>,
        recipeLines: List<RecipeLine>
    ): Map<String, Any?> = mapOf(
        "id"          to id.toString(),
        "title"       to title,
        "duration"    to duration.inWholeSeconds,
        "tags"        to tags,
        "image"       to image,
        "userId"      to user.id.toString(),
        "ingredients" to recipeLines.map { it.toMap() },
        "steps"       to steps.map { step ->
            mapOf(
                "id"          to step.id.toString(),
                "description" to step.description,
                "duration"    to step.duration.inWholeSeconds,
                "cardinal"    to step.cardinal
            )
        }
    )

    private fun RecipeLine.toMap(): Map<String, Any?> = mapOf(
        "id"          to id.toString(),
        "amount"      to amount,
        "measurement" to measurement.name,
        "ingredient"  to mapOf(
            "id"    to ingredient.id.toString(),
            "name"  to ingredient.name,
            "image" to ingredient.image
        )
    )
}