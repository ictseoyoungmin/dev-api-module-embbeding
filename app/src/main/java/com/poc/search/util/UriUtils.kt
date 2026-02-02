package com.poc.search.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri

object UriUtils {

    fun takePersistableReadPermission(context: Context, uri: Uri) {
        if (uri.scheme == "file") return // file:// 경로는 권한 유지가 필요 없음
        
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Persistable 권한을 지원하지 않는 URI는 무시
        }
    }

    fun readImageBounds(resolver: ContentResolver, uri: Uri): Pair<Int, Int>? {
        return try {
            val stream = if (uri.scheme == "file") {
                java.io.File(uri.path!!).inputStream()
            } else {
                resolver.openInputStream(uri)
            }
            
            stream?.use { input ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, opts)
                val w = opts.outWidth
                val h = opts.outHeight
                if (w > 0 && h > 0) Pair(w, h) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
