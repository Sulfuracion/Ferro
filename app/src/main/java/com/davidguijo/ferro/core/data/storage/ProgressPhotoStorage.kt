package com.davidguijo.ferro.core.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestiona las fotos de progreso en el almacenamiento privado de la app (`filesDir/photos`).
 * Las fotos elegidas o tomadas se copian siempre a `filesDir/photos` para no depender de URIs
 * externas con permisos temporales.
 */
@Singleton
class ProgressPhotoStorage
@Inject
constructor(@ApplicationContext private val context: Context) {
    private val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR_NAME).apply { mkdirs() }

    private val tempCameraDir: File
        get() = File(context.cacheDir, CAMERA_TEMP_DIR_NAME).apply { mkdirs() }

    /** Devuelve el archivo local correspondiente a [fileName] dentro de `filesDir/photos`. */
    fun resolvePhotoFile(fileName: String): File = File(photosDir, fileName)

    /** Crea un archivo temporal en caché y su URI de `FileProvider` para `TakePicture`. */
    fun createTempCameraUri(): Pair<Uri, File> {
        val tempFile = File(tempCameraDir, "cam_${UUID.randomUUID()}.jpg")
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        return uri to tempFile
    }

    /** Copia el contenido de [sourceUri] a `filesDir/photos/<uuid>.jpg` y devuelve el `fileName`. */
    suspend fun importFromUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val destFile = File(photosDir, fileName)
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.length() <= 0L) {
                destFile.delete()
                null
            } else {
                fileName
            }
        } catch (_: IOException) {
            destFile.delete()
            null
        } catch (_: SecurityException) {
            destFile.delete()
            null
        }
    }

    /** Mueve o copia [tempFile] de la cámara a `filesDir/photos/<uuid>.jpg` y borra el temporal. */
    suspend fun commitCameraFile(tempFile: File): String? = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() <= 0L) {
            tempFile.delete()
            return@withContext null
        }
        val fileName = "${UUID.randomUUID()}.jpg"
        val destFile = File(photosDir, fileName)
        try {
            tempFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
            fileName
        } catch (_: IOException) {
            destFile.delete()
            tempFile.delete()
            null
        }
    }

    /** Elimina el archivo de foto [fileName] de `filesDir/photos` si existe. */
    suspend fun deletePhotoFile(fileName: String) {
        withContext(Dispatchers.IO) {
            val file = File(photosDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private companion object {
        const val PHOTOS_DIR_NAME = "photos"
        const val CAMERA_TEMP_DIR_NAME = "camera"
    }
}
