package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.output.RecipePort
import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.SavedRecipe

class FirebaseRecipeService : RecipePort {
    override suspend fun createRecipe(recipe: Recipe) {
        Firebase.firestore.collection("recipes")
            .document(recipe.id.toString())
            .set(recipe.toMap()).await()
    }

    override suspend fun deleteRecipe(recipeId: String) {
        Firebase.firestore.collection("recipes")
            .document(recipeId)
            .delete().await()

    }

    override suspend fun saveRecipe(savedRecipe:SavedRecipe) {
        Firebase.firestore
            .collection("savedRecipes")
            .document("${savedRecipe.userId}_${savedRecipe.recipeId}")
            .set(savedRecipe.toMap()).await()
    }

    override suspend fun deleteSavedRecipe(savedRecipe: SavedRecipe) {
        Firebase.firestore
            .collection("savedRecipes")
            .document("${savedRecipe.userId}_${savedRecipe.recipeId}")
            .delete().await()

    }

    private fun Recipe.toMap(): Map<String, Any?> = mapOf(
        "id"          to id.toString(),
        "author"      to author,
        "title"       to title,
        "description" to description,
        "servings"     to servings,
        "duration"    to duration.inWholeSeconds,   // Long
        "ingredients" to ingredients,               // List<String> — Firestore lo soporta
        "steps"       to steps.map { step ->
            mapOf(
                "id"          to step.id.toString(),
                "description" to step.description,
                "duration"    to step.duration.inWholeSeconds,
                "cardinal"    to step.cardinal
            )
        },
        "tags"        to tags,
        "image"       to image
    )
    private fun SavedRecipe.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "recipeId" to recipeId.toString(),
        "savedAt" to savedAt.toString()
    )
}