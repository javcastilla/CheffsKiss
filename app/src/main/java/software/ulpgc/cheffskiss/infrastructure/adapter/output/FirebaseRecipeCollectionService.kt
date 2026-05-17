package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import java.util.UUID

import kotlin.collections.emptyList

class FirebaseRecipeCollectionService: RecipeCollectionRepository {
    private fun plansCollection(userId: UUID) = Firebase.firestore
        .collection("Users")
        .document(userId.toString())
        .collection("RecipeCollections")

    override suspend fun create(recipeCollection: RecipeCollection) {
        plansCollection(recipeCollection.userId)
            .document(recipeCollection.id.toString())
            .set(recipeCollection.toMap())
            .await()
    }

    override suspend fun update(recipeCollection: RecipeCollection) {
        plansCollection(recipeCollection.userId)
            .document(recipeCollection.id.toString())
            .set(recipeCollection.toMap())
            .await()
    }

    override suspend fun delete(recipeCollectionID: UUID,userId: UUID) {
        plansCollection(userId)
            .document(recipeCollectionID.toString())
            .delete()
            .await()
    }

    override fun get(userId: UUID): Flow<List<RecipeCollection>> = callbackFlow {
        val listener = plansCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val plans = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toRecipeCollection(userId) }.getOrNull()
                } ?: emptyList()
                trySend(plans)
            }
        awaitClose { listener.remove() }
    }
    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toRecipeCollection(userId: UUID): RecipeCollection {
        val id = getString("id")?.let { UUID.fromString(it) }
            ?: UUID.fromString(this.id)
        val name      = getString("name") ?: ""
        val image      = getString("image") ?: ""
        val createdAt = getString("createdAt")?.let { Instant.parse(it) } ?: Clock.System.now()
        val recipes = parseRecipeIds(get("recipes"))
        return RecipeCollection(
            id        = id,
            userId    = userId,
            name      = name,
            image = image,
            createdAt = createdAt,
            recipes = recipes,
        )

    }
    private fun parseRecipeIds(raw: Any?): List<UUID> {
        val entries = when (raw) {
            is List<*> -> raw
            null -> emptyList()
            else -> listOf(raw)
        }
        return entries.mapNotNull { entry ->
            val idString = when (entry) {
                is String -> entry
                else -> entry?.toString()
            }?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            runCatching { UUID.fromString(idString) }.getOrNull()
        }
    }

    private fun RecipeCollection.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "userId" to userId.toString(),
        "name" to name,
        "image" to image,
        "createdAt" to createdAt.toString(),
        "recipes" to recipes.map { it.toString() }
    )
}