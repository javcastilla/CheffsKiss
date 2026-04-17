package software.ulpgc.cheffskiss.application.port.output

import android.net.Uri

interface ImageStoragePort {
    /**
     * Persists the image at [uri] under the given [name] and returns
     * a stable URI string that Coil can load across sessions.
     */
    suspend fun save(uri: Uri, folder: String, name: String): String
}
