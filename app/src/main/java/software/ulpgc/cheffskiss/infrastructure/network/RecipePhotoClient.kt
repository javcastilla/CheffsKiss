package software.ulpgc.cheffskiss.infrastructure.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import software.ulpgc.cheffskiss.application.services.RecipePhotoUrls
import java.io.File
import java.io.IOException

class RecipePhotoClient(
    private val apiKey: String,
    private val baseUrl: String = "https://plytrox.com/photos",
) {

    private val client = OkHttpClient()
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    /**
     * Sube una foto y devuelve la URL completa lista para guardar en la receta.
     * @return URL completa de la imagen, o null si falla
     */
    fun uploadPhoto(file: File): String? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "photo",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            .build()

        val request = Request.Builder()
            .url("$normalizedBaseUrl/upload.php")
            .addHeader("X-Api-Key", apiKey)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                RecipePhotoUrls.photoGetUrl(json.getString("id"))
            }
        } catch (_: IOException) {
            null
        }
    }
}
