package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe
import java.util.UUID

class FirebaseRecipeService : RecipePort {

    // ── Authored recipes ──────────────────────────────────────────────────────

    override suspend fun createRecipe(recipe: Recipe) {
        Firebase.firestore
            .collection("recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap())
            .await()
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        Firebase.firestore
            .collection("recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap())
            .await()
    }

    override suspend fun deleteRecipe(recipeId: String) {
        Firebase.firestore
            .collection("recipes")
            .document(recipeId)
            .delete()
            .await()
    }

    // ── Saved recipes — path: users/{userUuid}/savedRecipes/{recipeId} ────────
    // userId en SavedRecipe ya es UUID (convertido por el Command con nameUUIDFromBytes)

    override suspend fun saveRecipe(savedRecipe: SavedRecipe) {
        Firebase.firestore
            .collection("userLibrary")
            .document(savedRecipe.userId.toString())
            .collection("savedRecipes")
            .document(savedRecipe.recipeId.toString())
            .set(savedRecipe.toMap())
            .await()
    }

    override suspend fun deleteSavedRecipe(savedRecipe: SavedRecipe) {
        Firebase.firestore
            .collection("userLibrary")
            .document(savedRecipe.userId.toString())
            .collection("savedRecipes")
            .document(savedRecipe.recipeId.toString())
            .delete()
            .await()
    }

    override fun getSavedRecipes(userId: String): Flow<List<SavedRecipe>> = callbackFlow {
        // Convertir Firebase UID → UUID para construir el path correcto
        val userUuid = UUID.nameUUIDFromBytes(userId.toByteArray())

        val listener = Firebase.firestore
            .collection("userLibrary")
            .document(userUuid.toString())
            .collection("savedRecipes")
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

    private fun Recipe.toMap(): Map<String, Any?> = mapOf(
        "id"          to id.toString(),
        "author"      to author,
        "title"       to title,
        "description" to description,
        "servings"    to servings,
        "duration"    to duration.inWholeSeconds,
        "ingredients" to ingredients,
        "steps"       to steps.map { step ->
            mapOf(
                "id"          to step.id.toString(),
                "description" to step.description,
                "duration"    to step.duration.inWholeSeconds,
                "cardinal"    to step.cardinal,
                "image"       to step.image
            )
        },
        "tags"  to tags,
        "image" to image
    )

    private fun SavedRecipe.toMap(): Map<String, Any?> = mapOf(
        "userId"   to userId.toString(),
        "recipeId" to recipeId.toString(),
        "savedAt"  to savedAt.toString()
    )
}
