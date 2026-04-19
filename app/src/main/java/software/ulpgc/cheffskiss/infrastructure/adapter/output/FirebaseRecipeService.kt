package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
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

    override suspend fun updateRecipe(recipe: Recipe, lines: List<RecipeLine>, steps: List<Step>) {
        db.collection("Recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap(lines, steps))
            .await()
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
        "author"      to author,
        "title"       to title,
        "description" to description,
        "servings"    to servings,
        "duration"    to duration.inWholeSeconds,
        "tags"        to tags,
        "image"       to image,
        "createdAt"   to createdAt.toString(),
        "lines"       to lines.map { line ->
            mapOf(
                "id"           to line.id.toString(),
                "ingredientId" to line.ingredientId.toString(),
                "amount"       to line.amount,
                "measurement"  to line.measurement.name
            )
        },
        "steps"       to steps.map { step ->
            mapOf(
                "id"          to step.id.toString(),
                "description" to step.description,
                "duration"    to step.duration.inWholeSeconds,
                "cardinal"    to step.cardinal,
                "image"       to step.image
            )
        }
    )

    private fun SavedRecipe.toMap(): Map<String, Any?> = mapOf(
        "userId"   to userId.toString(),
        "recipeId" to recipeId.toString(),
        "savedAt"  to savedAt.toString()
    )
}