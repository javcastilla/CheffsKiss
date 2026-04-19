package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.IngredientRepository
import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.domain.port.input.IngredientSearchPort
import java.util.UUID

class FirebaseIngredientRepository : IngredientRepository, IngredientSearchPort {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("Ingredients")

    override suspend fun searchByName(query: String): List<Ingredient> = search(query)

    override suspend fun getAll(): List<Ingredient> {
        return collection.get().await().documents.mapNotNull { doc ->
            runCatching {
                Ingredient(
                    id = UUID.fromString(doc.getString("id") ?: doc.id),
                    name = doc.getString("name") ?: "",
                    normalizedName = doc.getString("normalized_name") ?: "",
                    category = doc.getString("category") ?: "",
                    subcategory = doc.getString("subcategory") ?: "",
                    image = doc.getString("image") ?: "",
                    aliases = (doc.get("aliases") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }.getOrNull()
        }
    }

    override suspend fun getById(id: String): Ingredient? =
        collection.document(id).get().await().toIngredient()

    private fun DocumentSnapshot.toIngredient(): Ingredient? {
        val id = getString("id") ?: return null
        return runCatching {
            Ingredient(
                id = UUID.fromString(id),
                name = getString("name") ?: "",
                normalizedName = getString("normalizedName") ?: "",
                category = getString("category") ?: "",
                subcategory = getString("subcategory") ?: "",
                image = getString("image") ?: "",
                aliases = (get("aliases") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }.getOrNull()
    }

    override suspend fun search(query: String): List<Ingredient> {
        if (query.isBlank()) return emptyList()
        val normalized = query.trim().lowercase()
        return getAll().filter { ingredient ->
            ingredient.name.lowercase().contains(normalized) ||
                    ingredient.normalizedName.contains(normalized) ||
                    ingredient.aliases.any { it.lowercase().contains(normalized) }
        }.sortedWith(compareByDescending {
            when {
                it.normalizedName.startsWith(normalized) -> 3
                it.name.lowercase().startsWith(normalized) -> 2
                it.aliases.any { a -> a.lowercase().startsWith(normalized) } -> 1
                else -> 0
            }
        }).take(8)
    }
}
