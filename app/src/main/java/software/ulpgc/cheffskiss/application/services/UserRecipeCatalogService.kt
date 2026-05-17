package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.first
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.recipe.Recipe
import software.ulpgc.cheffskiss.domain.port.input.RecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeService

class UserRecipeCatalogService(
    private val recipeReader: RecipeReader = FirebaseRecipeReader(),
    private val recipeRepository: RecipeRepository = FirebaseRecipeService(),
    private val collectionRepository: RecipeCollectionRepository = FirebaseRecipeCollectionService(),
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService(),
) {
    suspend fun loadOwnedAndSaved(): List<Recipe> {
        val firebaseUid = currentUserPort.getCurrentUser() ?: return emptyList()
        val authorId = UserIds.creatorIdStringFromFirebaseUid(firebaseUid)
        val userUuid = UserIds.creatorIdFromFirebaseUid(firebaseUid)

        val owned = recipeReader.getByAuthor(authorId).first()
        val saved = GetSavedRecipesQuery(recipeRepository)(firebaseUid).first()
            .mapNotNull { recipeReader.getById(it.recipeId.toString()) }
        val collectionRecipeIds = GetRecipeCollectionQuery(collectionRepository)(userUuid).first()
            .flatMap { it.recipes }
            .distinct()
        val fromCollections = collectionRecipeIds.mapNotNull { recipeReader.getById(it.toString()) }

        return (owned + saved + fromCollections).distinctBy { it.id }
    }
}
