package software.ulpgc.cheffskiss.infrastructure.util

import android.content.Context
import android.net.Uri
import java.io.File

internal fun copyUriToTempFile(context: Context, uri: Uri, fileName: String): File {
    val dest = File(context.cacheDir, "photo_upload_${System.currentTimeMillis()}_$fileName")
    context.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Cannot read image: $uri")
    return dest
}
