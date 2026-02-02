package com.poc.petgalleryxml.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max

object ImageEncoder {

    /**
     * URI -> JPEG bytes
     * - EXIF 회전 보정
     * - 긴 변 기준 maxSidePx로 다운스케일 (원본이 더 작으면 그대로)
     */
    fun uriToJpegBytes(
        resolver: ContentResolver,
        uri: Uri,
        maxSidePx: Int,
        jpegQuality: Int
    ): ByteArray {
        val inputStream: InputStream = if (uri.scheme == "file") {
            File(uri.path!!).inputStream()
        } else {
            resolver.openInputStream(uri) ?: error("Failed to open input stream for $uri")
        }

        inputStream.use { stream ->
            val bytes = stream.readBytes()
            val exif = ExifInterface(bytes.inputStream())
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Failed to decode bitmap for $uri")

            val rotated = applyExifRotation(decoded, orientation)
            if (rotated != decoded) decoded.recycle()

            val scaled = scaleDownIfNeeded(rotated, maxSidePx)
            if (scaled != rotated) rotated.recycle()

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            scaled.recycle()
            return out.toByteArray()
        }
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxSidePx) return bitmap

        val scale = maxSidePx.toFloat() / maxSide.toFloat()
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
