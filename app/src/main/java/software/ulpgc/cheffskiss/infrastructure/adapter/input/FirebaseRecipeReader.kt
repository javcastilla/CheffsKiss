package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FirebaseRecipeReader : RecipeReader {

    private val collection = Firebase.firestore.collection("recipes")

    override fun getAll(): Flow<List<Recipe>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val recipes = snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList()
            trySend(recipes)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getById(id: String): Recipe? =
        collection.document(id).get().await().toRecipe()

    override fun search(query: String): Flow<List<Recipe>> = callbackFlow {
        val listener = collection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toRecipe(): Recipe? {
        val id = getString("id") ?: return null
        val author = getString("author") ?: return null
        return try {
            Recipe(
                id = UUID.fromString(id),
                author = UUID.fromString(author),
                title = getString("title") ?: "",
                duration = (getLong("duration") ?: 0L).seconds,
                ingredients = (get("ingredients") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                steps = (get("steps") as? List<*>)
                    ?.mapNotNull { it.toStep() } ?: emptyList(),
                tags = (get("tags") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                image = getString("image") ?: ""
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toStep(): Step? {
        val map = this as? Map<String, Any> ?: return null
        return Step(
            id = UUID.fromString(map["id"] as? String ?: return null),
            description = map["description"] as? String ?: "",
            duration = ((map["duration"] as? Long) ?: 0L).seconds,
            cardinal = ((map["cardinal"] as? Long) ?: 0L).toInt()
        )
    }
}
