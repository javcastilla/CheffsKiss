package software.ulpgc.cheffskiss.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import software.ulpgc.cheffskiss.application.config.RecipePhotoSettings
import software.ulpgc.cheffskiss.application.services.RecipePhotoUrls

@Composable
fun RecipeAsyncImage(
    url: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val model = remember(url, context) {
        when (url) {
            null -> null
            is String -> buildModel(context, url)
            is java.net.URI -> buildModel(context, url.toString())
            is android.net.Uri -> url
            else -> url
        }
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

private fun buildModel(context: android.content.Context, url: String): Any {
    val resolved = RecipePhotoUrls.resolveForDisplay(url) ?: url
    if (!RecipePhotoUrls.isRemotePhotoUrl(resolved)) return resolved

    val apiKey = RecipePhotoSettings.fromBuildConfig().apiKey
    if (apiKey.isBlank()) return resolved

    return ImageRequest.Builder(context)
        .data(resolved)
        .addHeader("X-Api-Key", apiKey)
        .crossfade(true)
        .build()
}
