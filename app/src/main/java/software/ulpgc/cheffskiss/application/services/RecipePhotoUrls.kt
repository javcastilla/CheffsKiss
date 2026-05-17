package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.application.config.RecipePhotoSettings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object RecipePhotoUrls {

    /** URL de GET con API key en header: `.../get.php?id=...` */
    fun photoGetUrl(photoId: String): String {
        val base = RecipePhotoSettings.fromBuildConfig().baseUrl.trimEnd('/')
        val encodedId = URLEncoder.encode(photoId, StandardCharsets.UTF_8.name())
        return "$base/get.php?id=$encodedId"
    }

    /** Normaliza URLs guardadas en Firestore (legacy `/photos/{id}` → `get.php?id=...`). */
    fun resolveForDisplay(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        if (stored.startsWith("content:") || stored.startsWith("file:")) return stored

        val settings = RecipePhotoSettings.fromBuildConfig()
        val base = settings.baseUrl.trimEnd('/')
        val host = settings.photoHost

        if (stored.contains("get.php")) return stored

        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            if (host != null && !stored.contains(host)) return stored
            val path = stored.substringBefore('?')
                .removePrefix(base)
                .trim('/')
            val photoId = when {
                path.isEmpty() -> return stored
                path.startsWith("storage/") -> path.removePrefix("storage/")
                else -> path.substringAfterLast('/').ifEmpty { path }
            }
            return photoGetUrl(photoId)
        }

        return photoGetUrl(stored)
    }

    fun isRemotePhotoUrl(url: String): Boolean {
        val host = RecipePhotoSettings.fromBuildConfig().photoHost ?: return false
        return url.contains(host)
    }
}
