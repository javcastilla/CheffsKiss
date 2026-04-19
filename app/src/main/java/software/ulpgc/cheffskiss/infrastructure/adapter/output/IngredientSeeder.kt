// app/src/main/java/software/ulpgc/cheffskiss/infrastructure/adapter/output/IngredientSeeder.kt
package software.ulpgc.cheffskiss.infrastructure.adapter.output

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object IngredientSeeder {

    private const val TAG = "IngredientSeeder"
    // Flag doc en Firestore para no re-seedear nunca más
    private const val SEED_FLAG_COLLECTION = "AppMeta"
    private const val SEED_FLAG_DOC = "ingredientsSeed"

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val db = Firebase.firestore

        // 1. Comprueba si ya se hizo el seed
        val flagDoc = db.collection(SEED_FLAG_COLLECTION)
            .document(SEED_FLAG_DOC)
            .get()
            .await()

        if (flagDoc.getBoolean("seeded") == true) {
            Log.d(TAG, "Ingredients already seeded, skipping.")
            return@withContext
        }

        // 2. Lee el JSON desde assets/ingredients.json
        val json = context.assets.open("ingredients.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(json)
        val ingredients: JSONArray = root.getJSONArray("ingredients")

        Log.d(TAG, "Seeding ${ingredients.length()} ingredients...")

        // 3. Batch write (Firestore admite 500 ops/batch)
        var batch = db.batch()
        var opsInBatch = 0

        for (i in 0 until ingredients.length()) {
            val item: JSONObject = ingredients.getJSONObject(i)

            val normalizedName = item.getString("normalized_name")
            // UUID v3 determinístico (nameUUIDFromBytes) → mismo nombre = mismo UUID siempre
            val id = UUID.nameUUIDFromBytes(normalizedName.toByteArray(Charsets.UTF_8))

            val aliasesJson = item.getJSONArray("aliases")
            val aliases = (0 until aliasesJson.length()).map { aliasesJson.getString(it) }

            val tagsJson = item.getJSONArray("tags")
            val tags = (0 until tagsJson.length()).map { tagsJson.getString(it) }

            val docRef = db.collection("Ingredients").document(id.toString())

            batch.set(
                docRef,
                hashMapOf(
                    "id" to id.toString(),
                    "name" to item.getString("name"),
                    "normalized_name" to normalizedName,
                    "category" to item.getString("category"),
                    "subcategory" to item.getString("subcategory"),
                    "aliases" to aliases,
                    "tags" to tags,
                    "image" to ""
                )
            )

            opsInBatch++

            // Flush cada 499 operaciones
            if (opsInBatch >= 499) {
                batch.commit().await()
                Log.d(TAG, "Committed batch of $opsInBatch ingredients")
                batch = db.batch()
                opsInBatch = 0
            }
        }

        // Último batch
        if (opsInBatch > 0) {
            batch.commit().await()
            Log.d(TAG, "Committed final batch of $opsInBatch ingredients")
        }

        // 4. Marca el flag para no re-ejecutar
        db.collection(SEED_FLAG_COLLECTION)
            .document(SEED_FLAG_DOC)
            .set(hashMapOf("seeded" to true, "seededAt" to System.currentTimeMillis()))
            .await()

        Log.d(TAG, "Seeding complete!")
    }
}