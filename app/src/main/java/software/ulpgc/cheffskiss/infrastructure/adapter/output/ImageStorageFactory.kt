package software.ulpgc.cheffskiss.infrastructure.adapter.output

import android.content.Context
import software.ulpgc.cheffskiss.application.config.RecipePhotoSettings
import software.ulpgc.cheffskiss.application.port.ImageStorage
object ImageStorageFactory {

    fun create(context: Context): ImageStorage {
        val settings = RecipePhotoSettings.fromBuildConfig()
        return RemoteImageStorage(context.applicationContext, settings)
    }
}
