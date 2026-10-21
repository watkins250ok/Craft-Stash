package com.example.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PhotoManager {
    private const val PHOTO_DIR_NAME = "craft_stash_photos"
    private const val MAX_DIMENSION = 1600

    fun getPhotoDir(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun savePhotoFromUri(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val photoDir = getPhotoDir(context)
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val targetFile = File(photoDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // Decode bounds first to prevent OutOfMemory
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val tempBytes = inputStream.readBytes()
                BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

                var inSampleSize = 1
                if (options.outHeight > MAX_DIMENSION || options.outWidth > MAX_DIMENSION) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= MAX_DIMENSION && (halfWidth / inSampleSize) >= MAX_DIMENSION) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val bitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
                    ?: return@withContext null

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                bitmap.recycle()
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveBitmap(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val photoDir = getPhotoDir(context)
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val targetFile = File(photoDir, fileName)

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deletePhoto(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
