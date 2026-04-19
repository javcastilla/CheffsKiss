package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.vo.Measurement
import software.ulpgc.cheffskiss.domain.model.vo.RecipeLine
import software.ulpgc.cheffskiss.domain.port.input.IngredientStore
import software.ulpgc.cheffskiss.domain.port.input.RecipeLineStore
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.domain.port.input.StepStore
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FirebaseRecipeReader : RecipeReader, RecipeLineStore, StepStore, IngredientStore {

    private val db          = Firebase.firestore
    private val recipes     = db.collection("recipes")
    private val ingredients = db.collection("ingredients")

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
            .whereEqualTo("author", author)
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
                val lines = (snapshot?.get("lines") as? List<Map<String, Any>>)
                    ?.mapNotNull { it.toRecipeLine() } ?: emptyList()
                trySend(lines)
            }
        awaitClose { listener.remove() }
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

    override suspend fun ingredientOf(recipeLine: RecipeLine): Ingredient? =
        ingredients.document(recipeLine.ingredientId.toString()).get().await().toIngredient()

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun DocumentSnapshot.toRecipe(): Recipe? {
        val id     = getString("id")     ?: return null
        val author = getString("author") ?: return null
        return runCatching {
            Recipe(
                id          = UUID.fromString(id),
                author      = author,
                title       = getString("title") ?: "",
                description = getString("description") ?: "",
                servings    = getLong("servings")?.toInt() ?: 1,
                duration    = (getLong("duration") ?: 0L).seconds,
                tags        = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                image       = getString("image") ?: "",
                createdAt   = getString("createdAt")?.let { Instant.parse(it) }
                    ?: kotlinx.datetime.Clock.System.now()
            )
        }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toRecipeLine(): RecipeLine? = runCatching {
        RecipeLine(
            id           = UUID.fromString(this["id"] as? String ?: return null),
            ingredientId = UUID.fromString(this["ingredientId"] as? String ?: return null),
            amount       = (this["amount"] as? Number)?.toInt() ?: return null,
            measurement  = Measurement.valueOf(this["measurement"] as? String ?: "UNIT")
        )
    }.getOrNull()

    private fun Any?.toStep(): Step? {
        val map = this as? Map<String, Any> ?: return null
        return runCatching {
            Step(
                id          = UUID.fromString(map["id"] as? String ?: return null),
                description = map["description"] as? String ?: "",
                duration    = ((map["duration"] as? Long) ?: 0L).seconds,
                cardinal    = ((map["cardinal"] as? Long) ?: 0L).toInt(),
                image       = map["image"] as? String ?: ""
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.toIngredient(): Ingredient? = runCatching {
        Ingredient(
            id    = UUID.fromString(getString("id") ?: return null),
            name  = getString("name") ?: "",
            image = getString("image") ?: ""
        )
    }.getOrNull()
}