package com.poc.search.data.api

import android.content.ContentResolver
import android.net.Uri
import com.poc.search.data.api.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PetApiClient(
    baseUrl: String,
    private val resolver: ContentResolver,
    private val okHttp: OkHttpClient = defaultOkHttp()
) {
    private val base = baseUrl.trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val jpegType = "image/jpeg".toMediaType()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun absUrl(relativeOrAbs: String): String {
        val s = relativeOrAbs.trim()
        return if (s.startsWith("http://") || s.startsWith("https://")) s else base + s
    }

    suspend fun health(): String = withContext(Dispatchers.IO) {
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

    suspend fun ingest(
        uri: Uri,
        daycareId: String,
        trainerId: String? = null,
        capturedAtIso: String? = null,
        includeEmbedding: Boolean = false,
        jpegMaxSidePx: Int = 2048,
        jpegQuality: Int = 92
    ): IngestResponse = withContext(Dispatchers.IO) {
        val jpg = ImageEncoder.uriToJpegBytes(
            resolver = resolver,
            uri = uri,
            maxSidePx = jpegMaxSidePx,
            jpegQuality = jpegQuality
        )

        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("daycare_id", daycareId)
            .apply {
                if (!trainerId.isNullOrBlank()) addFormDataPart("trainer_id", trainerId)
                if (!capturedAtIso.isNullOrBlank()) addFormDataPart("captured_at", capturedAtIso)
            }
            .addFormDataPart("file", "upload.jpg", jpg.toRequestBody(jpegType))
            .build()

        val url = "$base/v1/ingest?include_embedding=$includeEmbedding"
        val req = Request.Builder()
            .url(url)
            .post(multipart)
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Ingest failed: HTTP ${resp.code} $body")
            json.decodeFromString(IngestResponse.serializer(), body)
        }
    }

    suspend fun search(body: SearchRequest): SearchResponse = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(SearchRequest.serializer(), body)
        val req = Request.Builder()
            .url("$base/v1/search")
            .post(payload.toRequestBody(jsonType))
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Search failed: HTTP ${resp.code} $s")
            json.decodeFromString(SearchResponse.serializer(), s)
        }
    }

    suspend fun setLabels(body: LabelRequest): LabelResponse = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(LabelRequest.serializer(), body)
        val req = Request.Builder()
            .url("$base/v1/labels")
            .post(payload.toRequestBody(jsonType))
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Labels failed: HTTP ${resp.code} $s")
            json.decodeFromString(LabelResponse.serializer(), s)
        }
    }

    suspend fun listImages(daycareId: String, limit: Int = 200, offset: Int = 0): ImagesListResponse = withContext(Dispatchers.IO) {
        val url = "$base/v1/images?daycare_id=$daycareId&limit=$limit&offset=$offset"
        val req = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("List images failed: HTTP ${resp.code} $s")
            json.decodeFromString(ImagesListResponse.serializer(), s)
        }
    }

    suspend fun getImageMeta(imageId: String): ImageMetaResponse = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$base/v1/images/$imageId/meta")
            .get()
            .build()

        okHttp.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Get image meta failed: HTTP ${resp.code} $s")
            json.decodeFromString(ImageMetaResponse.serializer(), s)
        }
    }

    fun absoluteImageUrl(relativeOrAbs: String): String = absUrl(relativeOrAbs)

    companion object {
        fun defaultOkHttp(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build()
    }
}
