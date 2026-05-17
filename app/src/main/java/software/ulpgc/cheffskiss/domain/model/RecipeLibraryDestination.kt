package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

/** Destino al guardar una receta en la biblioteca del usuario. */
sealed class RecipeLibraryDestination {
    data object Saved : RecipeLibraryDestination()
    data class Collection(val collectionId: UUID) : RecipeLibraryDestination()
}
