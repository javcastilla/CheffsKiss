package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import software.ulpgc.cheffskiss.application.control.AddRecipeToCollectionCommand
import software.ulpgc.cheffskiss.application.control.SaveRecipeCommand
import software.ulpgc.cheffskiss.application.control.SaveRecipeInput
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.port.RecipeRepository
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.domain.model.RecipeLibraryDestination
import java.util.UUID

data class RecipeLibraryPickerContext(
    val collections: List<RecipeCollection> = emptyList(),
    val isInSaved: Boolean = false,
    val collectionIdsContainingRecipe: Set<UUID> = emptySet(),
)

data class RecipeLibraryAddResult(
    val message: String,
    val alreadyPresent: Boolean = false,
)

class RecipeLibraryService(
    private val recipeRepository: RecipeRepository,
    private val collectionRepository: RecipeCollectionRepository,
) {

    fun observePickerContext(recipeId: UUID, firebaseUid: String): Flow<RecipeLibraryPickerContext> {
        val userUuid = UserIds.creatorIdFromFirebaseUid(firebaseUid)
        return combine(
            GetSavedRecipesQuery(recipeRepository)(firebaseUid),
            GetRecipeCollectionQuery(collectionRepository)(userUuid),
        ) { saved, collections ->
            RecipeLibraryPickerContext(
                collections = collections,
                isInSaved = saved.any { it.recipeId == recipeId },
                collectionIdsContainingRecipe = collections
                    .filter { recipeId in it.recipes }
                    .map { it.id }
                    .toSet(),
            )
        }
    }

    suspend fun addRecipeTo(
        destination: RecipeLibraryDestination,
        recipeId: UUID,
        firebaseUid: String,
    ): RecipeLibraryAddResult {
        val userUuid = UserIds.creatorIdFromFirebaseUid(firebaseUid)
        return when (destination) {
            RecipeLibraryDestination.Saved -> addToSaved(recipeId, firebaseUid)
            is RecipeLibraryDestination.Collection -> addToCollection(
                collectionId = destination.collectionId,
                recipeId = recipeId,
                userUuid = userUuid,
            )
        }
    }

    private suspend fun addToSaved(recipeId: UUID, firebaseUid: String): RecipeLibraryAddResult {
        val alreadySaved = GetSavedRecipesQuery(recipeRepository)(firebaseUid)
            .first()
            .any { it.recipeId == recipeId }
        if (alreadySaved) {
            return RecipeLibraryAddResult("This recipe is already in Guardados", alreadyPresent = true)
        }
        SaveRecipeCommand(
            recipeRepository,
            object : SaveRecipeInput {
                override fun recipeId() = recipeId
                override fun userId() = firebaseUid
            },
        ).execute()
        return RecipeLibraryAddResult("Added to Guardados")
    }

    private suspend fun addToCollection(
        collectionId: UUID,
        recipeId: UUID,
        userUuid: UUID,
    ): RecipeLibraryAddResult {
        val collection = GetRecipeCollectionQuery(collectionRepository)(userUuid)
            .first()
            .firstOrNull { it.id == collectionId }
            ?: return RecipeLibraryAddResult("List not found")

        if (recipeId in collection.recipes) {
            return RecipeLibraryAddResult(
                "Already in \"${collection.name}\"",
                alreadyPresent = true,
            )
        }

        AddRecipeToCollectionCommand(
            port = collectionRepository,
            collectionId = collectionId,
            userId = userUuid,
            recipeId = recipeId,
        ).execute()

        return RecipeLibraryAddResult("Added to \"${collection.name}\"")
    }
}
