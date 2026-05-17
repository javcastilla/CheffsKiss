package software.ulpgc.cheffskiss.application.port

import android.net.Uri

interface ImageStorage {
    /**
     * Sube la imagen en [uri] y devuelve la URL remota para guardar en el modelo.
     */
    suspend fun save(uri: Uri, folder: String, name: String): String
}
