package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader

class FirebaseRecipeReader : RecipeReader {

    private val collection = Firebase.firestore.collection("recipes")

    override fun getAll(): Flow<List<Recipe>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val recipes = snapshot?.documents
                ?.mapNotNull { it.toObject<Recipe>() }
                ?: emptyList()
            trySend(recipes)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getById(id: String): Recipe? =
        collection.document(id).get().await().toObject<Recipe>()

    override fun search(query: String): Flow<List<Recipe>> = callbackFlow {
        val listener = collection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject<Recipe>() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}
