package software.ulpgc.cheffskiss.infrastructure.adapter.output

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.ulpgc.cheffskiss.application.config.RecipePhotoSettings
import software.ulpgc.cheffskiss.application.port.ImageStorage
import software.ulpgc.cheffskiss.infrastructure.network.RecipePhotoClient
import software.ulpgc.cheffskiss.infrastructure.util.copyUriToTempFile

class RemoteImageStorage(
    private val context: Context,
    private val settings: RecipePhotoSettings,
) : ImageStorage {

    override suspend fun save(uri: Uri, folder: String, name: String): String = withContext(Dispatchers.IO) {
        settings.requireConfigured()
        val client = RecipePhotoClient(settings.apiKey, settings.baseUrl)
        val tempFile = copyUriToTempFile(context, uri, name)
        try {
            client.uploadPhoto(tempFile)
                ?: error("Photo upload failed. Check your connection and API key.")
        } finally {
            tempFile.delete()
        }
    }
}
