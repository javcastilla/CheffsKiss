package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeVersion
import java.util.UUID

class FirebaseRecipeService : RecipeRepository {

    private val db = Firebase.firestore

    // ── Authored recipes ──────────────────────────────────────────────────────

    override suspend fun createRecipe(recipe: Recipe, lines: List<RecipeLine>, steps: List<Step>) {
        db.collection("Recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap(lines, steps))
            .await()
    }

    override suspend fun updateRecipe(
        recipe: Recipe,
        lines: List<RecipeLine>,
        steps: List<Step>,
        versionSnapshot: RecipeVersion?,
    ) {
        val doc = db.collection("Recipes").document(recipe.id.toString())
        versionSnapshot?.let { snapshot ->
            doc.collection("versions")
                .document(recipe.version.toString())
                .set(snapshot.toMap(lines, steps))
                .await()
        }
        doc.set(recipe.toMap(lines, steps)).await()
    }

    override suspend fun deleteRecipe(recipeId: String) {
        db.collection("Recipes")
            .document(recipeId)
            .delete()
            .await()
    }

    // ── Saved recipes ─────────────────────────────────────────────────────────

    override suspend fun saveRecipe(savedRecipe: SavedRecipe) {
        db.collection("UserLibrary")
            .document(savedRecipe.userId.toString())
            .collection("SavedRecipes")
            .document(savedRecipe.recipeId.toString())
            .set(savedRecipe.toMap())
            .await()
    }

    override suspend fun deleteSavedRecipe(savedRecipe: SavedRecipe) {
        db.collection("UserLibrary")
            .document(savedRecipe.userId.toString())
            .collection("SavedRecipes")
            .document(savedRecipe.recipeId.toString())
            .delete()
            .await()
    }

    override fun getSavedRecipes(userId: String): Flow<List<SavedRecipe>> = callbackFlow {
        val userUuid = UUID.nameUUIDFromBytes(userId.toByteArray())
        val listener = db.collection("UserLibrary")
            .document(userUuid.toString())
            .collection("SavedRecipes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val saved = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        SavedRecipe(
                            userId   = userUuid,
                            recipeId = UUID.fromString(doc.id),
                            savedAt  = doc.getString("savedAt")
                                ?.let { Instant.parse(it) }
                                ?: kotlinx.datetime.Clock.System.now()
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(saved)
            }
        awaitClose { listener.remove() }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Recipe.toMap(lines: List<RecipeLine>, steps: List<Step>): Map<String, Any?> = mapOf(
        "id"          to id.toString(),
        "version"     to version,
        "title"       to title,
        "description" to description,
        "duration"    to duration.inWholeSeconds,
        "tags"        to tags,
        "servings"    to servings,
        "image"       to image?.toString(),
        "timestamp"   to timestamp.toString(),
        "creatorId"   to creator.id.toString(),
        "lines"       to lines.map { line ->
            mapOf(
                "id"           to line.id.toString(),
                "amount"       to line.amount,
                "ingredientId" to line.ingredient?.id?.toString(),
                "measurement"  to line.measurement?.name
            )
        },
        "steps"       to steps.map { step ->
            mapOf(
                "id"          to step.id.toString(),
                "description" to step.description,
                "duration"    to step.duration?.inWholeSeconds,
                "cardinal"    to step.cardinal
            )
        }
    )

    private fun SavedRecipe.toMap(): Map<String, Any?> = mapOf(
        "userId"   to userId.toString(),
        "recipeId" to recipeId.toString(),
        "savedAt"  to savedAt.toString()
    )

    private fun RecipeVersion.toMap(lines: List<RecipeLine>, steps: List<Step>): Map<String, Any?> =
        mapOf(
            "id"        to id.toString(),
            "timestamp" to timestamp.toString(),
            "status"    to status.name,
            "recipe"    to recipe.toMap(lines, steps),
        )
}