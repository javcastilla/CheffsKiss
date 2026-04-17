package software.ulpgc.cheffskiss.infrastructure.adapter.output

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.ulpgc.cheffskiss.application.port.output.ImageStoragePort
import java.io.File

class LocalImageStorage(private val context: Context) : ImageStoragePort {

    private fun dir(folder: String): File =
        File(context.filesDir, "recipe_images/$folder").also { it.mkdirs() }

    /**
     * Copies the image from [uri] (content:// from the media picker) into the app's
     * private files directory under [folder]/[name] and returns a stable file:// URI
     * string that Coil can load across app restarts.
     */
    override suspend fun save(uri: Uri, folder: String, name: String): String = withContext(Dispatchers.IO) {
        val dest = File(dir(folder), name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot read image: $uri")
        dest.toUri().toString()   // → file:///data/data/<pkg>/files/recipe_images/<name>
    }
}
