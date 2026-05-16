package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.enum.Measurement
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.recipe.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.IngredientStore
import software.ulpgc.cheffskiss.domain.port.input.RecipeLineStore
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.domain.port.input.StepStore
import software.ulpgc.cheffskiss.domain.model.user.User
import java.util.UUID
import java.net.URI
import kotlin.time.Duration.Companion.seconds

class FirebaseRecipeReader : RecipeReader, RecipeLineStore, StepStore, IngredientStore {

    private val db          = Firebase.firestore
    private val recipes     = db.collection("Recipes")
    private val ingredients = db.collection("Ingredients")

    // ── RecipeReader ──────────────────────────────────────────────────────────

    override fun getAll(): Flow<List<Recipe>> = callbackFlow {
        val listener = recipes.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getById(id: String): Recipe? =
        recipes.document(id).get().await().toRecipe()

    override fun search(query: String): Flow<List<Recipe>> = callbackFlow {
        val listener = recipes
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getByAuthor(author: String): Flow<List<Recipe>> = callbackFlow {
        val listener = recipes
            .whereEqualTo("creatorId", author)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── RecipeLineStore ───────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    override fun linesOf(recipe: Recipe): Flow<List<RecipeLine>> = callbackFlow {
        val listener = recipes.document(recipe.id.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val raw = (snapshot?.get("lines") as? List<Map<String, Any>>) ?: emptyList()
                trySend(raw)
            }
        awaitClose { listener.remove() }
    }.map { rawLines ->
        rawLines.mapNotNull { map -> mapToRecipeLine(map) }
    }

    // ── StepStore ─────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    override fun stepsOf(recipe: Recipe): Flow<List<Step>> = callbackFlow {
        val listener = recipes.document(recipe.id.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val steps = (snapshot?.get("steps") as? List<*>)
                    ?.mapNotNull { it.toStep() } ?: emptyList()
                trySend(steps)
            }
        awaitClose { listener.remove() }
    }

    // ── IngredientStore ───────────────────────────────────────────────────────

    override suspend fun ingredientOf(recipeLine: RecipeLine): Ingredient? {
        recipeLine.ingredient?.let { return it }
        return null
    }

    suspend fun getIngredientById(id: String): Ingredient? {
        if (id.isBlank()) return null
        return ingredients.document(id).get().await().toIngredient()
    }

    suspend fun findByName(name: String): Ingredient? {
        if (name.isBlank()) return null
        val snapshot = ingredients
            .whereEqualTo("name", name)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toIngredient()
    }

    suspend fun findOrCreateByName(name: String): Ingredient {
        findByName(name)?.let { return it }
        val ingredient = Ingredient(id = UUID.randomUUID(), name = name)
        saveIngredient(ingredient)
        return ingredient
    }

    suspend fun saveIngredient(ingredient: Ingredient) {
        ingredients.document(ingredient.id.toString()).set(
            mapOf(
                "id" to ingredient.id.toString(),
                "name" to ingredient.name,
                "image" to ingredient.image?.toString(),
            )
        ).await()
    }

    private suspend fun mapToRecipeLine(map: Map<String, Any>): RecipeLine? = runCatching {
        val ingredientId = map["ingredientId"] as? String
        val embedded = map["ingredient"] as? Map<*, *>
        val ingredient = when {
            embedded != null -> embeddedToIngredient(embedded)
            !ingredientId.isNullOrBlank() -> getIngredientById(ingredientId)
            else -> null
        }
        RecipeLine(
            id = UUID.fromString(map["id"] as? String ?: return null),
            amount = (map["amount"] as? Number)?.toInt() ?: 1,
            ingredient = ingredient,
            measurement = (map["measurement"] as? String)?.let { Measurement.valueOf(it) },
        )
    }.getOrNull()

    private fun embeddedToIngredient(map: Map<*, *>): Ingredient? = runCatching {
        Ingredient(
            id = UUID.fromString(map["id"] as? String ?: return null),
            name = map["name"] as? String ?: return null,
        )
    }.getOrNull()

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun DocumentSnapshot.toRecipe(): Recipe? {
        val id = getString("id") ?: return null
        val creatorId = getString("creatorId") ?: return null
        
        val creator = try {
            User(UUID.fromString(creatorId))
        } catch (e: Exception) {
            User(UUID.nameUUIDFromBytes(creatorId.toByteArray()))
        }
        
        return runCatching {
            Recipe(
                id          = UUID.fromString(id),
                version     = getLong("version")?.toInt() ?: 0,
                title       = getString("title") ?: "",
                description = getString("description") ?: "",
                duration    = (getLong("duration") ?: 0L).seconds,
                tags        = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                servings    = getLong("servings")?.toInt() ?: 1,
                image       = getString("image")?.let { 
                    try { URI(it) } catch (e: Exception) { null }
                },
                timestamp   = getString("timestamp")?.let { Instant.parse(it) }
                    ?: Clock.System.now(),
                creator     = creator
            )
        }.getOrNull()
    }

    private fun Any?.toStep(): Step? {
        val map = this as? Map<String, Any> ?: return null
        return runCatching {
            Step(
                id          = UUID.fromString(map["id"] as? String ?: return null),
                description = map["description"] as? String ?: "",
                duration    = ((map["duration"] as? Long) ?: 0L).takeIf { it > 0 }?.seconds,
                cardinal    = ((map["cardinal"] as? Long) ?: 0L).toInt()
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.toIngredient(): Ingredient? = runCatching {
        Ingredient(
            id    = UUID.fromString(getString("id") ?: return null),
            name  = getString("name") ?: "",
            image = getString("image")?.let { 
                try { URI(it) } catch (e: Exception) { null }
            }
        )
    }.getOrNull()
}