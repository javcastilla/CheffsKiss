package software.ulpgc.cheffskiss.infrastructure.coil

import android.net.Uri
import coil.map.Mapper
import coil.request.Options
import software.ulpgc.cheffskiss.application.services.RecipePhotoUrls
import java.net.URI

class RecipePhotoUrlMapper : Mapper<Any, Any> {
    override fun map(data: Any, options: Options): Any = when (data) {
        is String -> RecipePhotoUrls.resolveForDisplay(data) ?: data
        is Uri -> Uri.parse(RecipePhotoUrls.resolveForDisplay(data.toString()) ?: data.toString())
        is URI -> URI(RecipePhotoUrls.resolveForDisplay(data.toString()) ?: data.toString())
        else -> data
    }
}
