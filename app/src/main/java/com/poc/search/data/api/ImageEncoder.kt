package com.poc.search.data.api

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object ImageEncoder {

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

            val fullBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Failed to decode bitmap")

            // 서버가 요구하는 440x440 크기를 유지하되, 비율을 지키기 위해 Letterboxing 적용
            val finalBitmap = rotateAndLetterbox(fullBitmap, orientation, 440)
            
            val out = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            
            if (fullBitmap != finalBitmap) fullBitmap.recycle()
            finalBitmap.recycle()
            
            return out.toByteArray()
        }
    }

    private fun rotateAndLetterbox(bitmap: Bitmap, orientation: Int, targetSize: Int): Bitmap {
        // 1. 먼저 회전 적용
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 2. 비율을 유지하며 targetSize에 맞게 리사이징 비율 계산
        val scale = targetSize.toFloat() / Math.max(rotated.width, rotated.height)
        val scaledW = (rotated.width * scale).toInt()
        val scaledH = (rotated.height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(rotated, scaledW, scaledH, true)

        // 3. 440x440 검은색 배경 생성 후 중앙에 배치 (Letterboxing)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK) // 배경색
        
        val left = (targetSize - scaledW) / 2f
        val top = (targetSize - scaledH) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, Paint(Paint.FILTER_BITMAP_FLAG))

        // 중간 비트맵들 정리
        if (rotated != bitmap) rotated.recycle()
        scaledBitmap.recycle()

        return output
    }
}
