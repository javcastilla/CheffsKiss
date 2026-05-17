package software.ulpgc.cheffskiss.application.config

import software.ulpgc.cheffskiss.BuildConfig

/**
 * Configuración de la API de fotos. Añade en `local.properties` (no versionado):
 * - `recipe.photo.api.key=TU_CLAVE`
 * - `recipe.photo.base.url=https://plytrox.com/photos` (opcional)
 */
data class RecipePhotoSettings(
    val apiKey: String,
    val baseUrl: String,
) {
    val photoHost: String?
        get() = runCatching { java.net.URI(baseUrl).host }.getOrNull()

    fun requireConfigured() {
        require(apiKey.isNotBlank()) {
            "Recipe photo API key not set. Add recipe.photo.api.key to local.properties"
        }
    }

    companion object {
        fun fromBuildConfig(): RecipePhotoSettings = RecipePhotoSettings(
            apiKey = BuildConfig.RECIPE_PHOTO_API_KEY,
            baseUrl = BuildConfig.RECIPE_PHOTO_BASE_URL.trimEnd('/'),
        )
    }
}
