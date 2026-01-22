package com.poc.search.data.api

import android.content.ContentResolver
import android.net.Uri
import com.poc.search.model.ResponseFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class BatchEmbedResult(
    val modelVersion: String?,
    val embeddings: ParsedBatchEmbeddings
)

class DogfaceApiClient(
    baseUrl: String,
    private val resolver: ContentResolver,
    private val okHttp: OkHttpClient = defaultOkHttp()
) {
    private val base = baseUrl.trimEnd('/')
    private val jpegType = "image/jpeg".toMediaType()

    suspend fun healthRaw(): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$base/v1/health")
            .get()
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Health failed: HTTP ${resp.code} $body")
            body
        }
    }

    suspend fun embedBatch(
        uris: List<Uri>,
        format: ResponseFormat,
        maxSidePx: Int,
        jpegQuality: Int
    ): BatchEmbedResult = withContext(Dispatchers.IO) {
        require(uris.isNotEmpty()) { "uris is empty" }

        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)

        uris.forEachIndexed { idx, uri ->
            val jpg = ImageEncoder.uriToJpegBytes(
                resolver = resolver,
                uri = uri,
                maxSidePx = maxSidePx,
                jpegQuality = jpegQuality
            )
            val filename = "img_$idx.jpg"
            multipart.addFormDataPart(
                "files",
                filename,
                jpg.toRequestBody(jpegType)
            )
        }

        val url = "$base/v1/embed/batch?format=${format.value}"
        val req = Request.Builder()
            .url(url)
            .post(multipart.build())
            .build()

        okHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string()
                error("HTTP ${resp.code}: $err")
            }

            val modelVersion = resp.header("X-Model-Version")
            val bytes = resp.body?.bytes() ?: error("Empty body")
            val parsed = DogfaceBatchParser.parseDogfaceBatchV1(bytes)
            BatchEmbedResult(modelVersion = modelVersion, embeddings = parsed)
        }
    }

    companion object {
        fun defaultOkHttp(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build()
    }
}
