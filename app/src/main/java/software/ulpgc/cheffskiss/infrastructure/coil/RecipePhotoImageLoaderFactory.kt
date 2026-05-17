package software.ulpgc.cheffskiss.infrastructure.coil

import android.content.Context
import coil.ImageLoader
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import software.ulpgc.cheffskiss.application.config.RecipePhotoSettings

object RecipePhotoImageLoaderFactory {

    fun create(context: Context): ImageLoader {
        val settings = RecipePhotoSettings.fromBuildConfig()
        val photoHost = settings.photoHost

        val okHttp = OkHttpClient.Builder()
            .addNetworkInterceptor(photoApiKeyInterceptor(settings.apiKey, photoHost))
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttp)
            .components {
                add(RecipePhotoUrlMapper())
            }
            .build()
    }

    private fun photoApiKeyInterceptor(apiKey: String, photoHost: String?): Interceptor =
        Interceptor { chain ->
            val request = chain.request()
            val onPhotoHost = photoHost != null && request.url.host.equals(photoHost, ignoreCase = true)
            val authenticated = if (apiKey.isNotBlank() && onPhotoHost) {
                request.newBuilder()
                    .header("X-Api-Key", apiKey)
                    .build()
            } else {
                request
            }
            chain.proceed(authenticated)
        }
}
