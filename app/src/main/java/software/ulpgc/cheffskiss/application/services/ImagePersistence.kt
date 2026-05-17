package software.ulpgc.cheffskiss.application.services

import android.net.Uri
import software.ulpgc.cheffskiss.application.port.ImageStorage

object ImagePersistence {

    /** Devuelve la URL remota existente o sube el URI local vía [imageStorage]. */
    suspend fun persistIfLocal(
        imageStorage: ImageStorage,
        source: String?,
        folder: String,
        fileName: String,
    ): String {
        if (source.isNullOrBlank()) return ""
        if (source.startsWith("http://") || source.startsWith("https://")) return source
        return imageStorage.save(Uri.parse(source), folder, fileName)
    }
}
