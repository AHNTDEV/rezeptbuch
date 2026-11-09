package de.martinkloeckner.rezeptbuch.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Photos live as JPEG files in `filesDir/photos/`; the database only stores file names. */
class PhotoStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    val dir: File = File(context.applicationContext.filesDir, "photos").also { it.mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun exists(name: String): Boolean = file(name).isFile

    /** Reads, rotates (EXIF), downsizes to [maxSide] px and stores as JPEG. Returns the new file name or null. */
    suspend fun import(uri: Uri, maxSide: Int = 2000): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val rotation = resolver.openInputStream(uri)?.use { rotationDegrees(it) } ?: 0
            val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
            val m = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation.toFloat())
            }
            val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            val name = "${UUID.randomUUID()}.jpg"
            file(name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (out !== decoded) out.recycle()
            decoded.recycle()
            name
        }.getOrNull()
    }

    /** Decodes a stored photo for display, at most [maxSide] px on the long edge. */
    suspend fun load(name: String, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(name)
        if (!f.isFile) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun delete(name: String) {
        file(name).delete()
    }

    private fun rotationDegrees(stream: java.io.InputStream): Int =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}
