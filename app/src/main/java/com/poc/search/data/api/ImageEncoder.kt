package com.poc.search.data.api

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageEncoder {
    fun uriToJpegBytes(
        resolver: ContentResolver,
        uri: Uri,
        maxSidePx: Int = 1024,
        jpegQuality: Int = 90
    ): ByteArray {
        val orientation = resolver.openInputStream(uri).use { input ->
            if (input == null) ExifInterface.ORIENTATION_UNDEFINED
            else ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open uri: $uri" }
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        require(srcW > 0 && srcH > 0) { "Invalid image size: $srcW x $srcH" }

        val maxSrcSide = maxOf(srcW, srcH)
        var sample = 1
        while (maxSrcSide / sample > maxSidePx) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open uri: $uri" }
            BitmapFactory.decodeStream(input, null, opts)
        } ?: error("Bitmap decode failed: $uri")

        val rotated = applyExifRotation(bitmap, orientation)

        return ByteArrayOutputStream().use { bos ->
            rotated.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bos)
            bos.toByteArray()
        }
    }

    private fun applyExifRotation(src: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return src
        }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (out != src) src.recycle()
        return out
    }
}
