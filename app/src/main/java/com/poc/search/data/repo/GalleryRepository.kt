package com.poc.search.data.repo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.poc.search.data.api.PetApiClient
import com.poc.search.data.api.dto.*
import com.poc.search.data.db.AppDatabase
import com.poc.search.data.db.LocalImageEntity
import com.poc.search.data.db.LocalInstanceEntity
import com.poc.search.data.db.UploadState
import com.poc.search.util.TimeUtils
import com.poc.search.util.UriUtils
import com.poc.search.data.api.ImageEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val imageDao = db.imageDao()
    private val instanceDao = db.instanceDao()

    fun observeImages(daycareId: String): Flow<List<LocalImageEntity>> =
        imageDao.observeImages(daycareId)

    fun observeInstances(localUri: String) =
        instanceDao.observeInstancesForLocalUri(localUri)

    suspend fun addPickedUris(daycareId: String, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val now = TimeUtils.nowEpochMs()

        val items = uris.map { uri ->
            UriUtils.takePersistableReadPermission(context, uri)
            val bounds = UriUtils.readImageBounds(resolver, uri)
            LocalImageEntity(
                localUri = uri.toString(),
                daycareId = daycareId,
                serverImageId = null,
                capturedAtEpochMs = now,
                width = bounds?.first,
                height = bounds?.second,
                uploadState = UploadState.PENDING,
                sortRank = null
            )
        }
        imageDao.upsertImages(items)
    }

    suspend fun ingestOne(
        baseUrl: String,
        daycareId: String,
        trainerId: String?,
        localUri: String,
        jpegMaxSidePx: Int = 2048,
        jpegQuality: Int = 92
    ) = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val api = PetApiClient(baseUrl = baseUrl, resolver = resolver)
        val uri = Uri.parse(localUri)

        try {
            val resp = api.ingest(
                uri = uri,
                daycareId = daycareId,
                trainerId = trainerId,
                capturedAtIso = TimeUtils.nowIsoUtc(),
                includeEmbedding = false,
                jpegMaxSidePx = jpegMaxSidePx,
                jpegQuality = jpegQuality
            )

            imageDao.markUploaded(
                localUri = localUri,
                state = UploadState.UPLOADED,
                serverImageId = resp.image.imageId,
                uploadedAt = TimeUtils.nowEpochMs(),
                width = resp.image.width,
                height = resp.image.height
            )

            val instances = resp.instances.map { inst ->
                LocalInstanceEntity(
                    instanceId = inst.instanceId,
                    localUri = localUri,
                    serverImageId = resp.image.imageId,
                    classId = inst.classId,
                    species = inst.species,
                    confidence = inst.confidence,
                    x1 = inst.bbox.x1.toDouble(),
                    y1 = inst.bbox.y1.toDouble(),
                    x2 = inst.bbox.x2.toDouble(),
                    y2 = inst.bbox.y2.toDouble(),
                    petId = inst.petId,
                    embeddingType = inst.embeddingMeta?.embeddingType,
                    modelVersion = inst.embeddingMeta?.modelVersion
                )
            }
            instanceDao.upsertInstances(instances)

        } catch (e: Exception) {
            imageDao.markUploaded(
                localUri = localUri,
                state = UploadState.FAILED,
                serverImageId = null,
                uploadedAt = null,
                width = null,
                height = null
            )
            throw e
        }
    }

    suspend fun applySearchOrder(daycareId: String, orderedServerImageIds: List<String>) = withContext(Dispatchers.IO) {
        imageDao.clearSortRanks(daycareId)
        orderedServerImageIds.forEachIndexed { idx, imgId ->
            imageDao.setSortRank(daycareId, imgId, idx)
        }
    }

    suspend fun searchByInstances(
        baseUrl: String,
        daycareId: String,
        instanceIds: List<String>,
        species: String? = "DOG",
        topKImages: Int = 200,
        perQueryLimit: Int = 500
    ): List<String> = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val api = PetApiClient(baseUrl = baseUrl, resolver = resolver)

        val uniq = instanceIds.distinct()
        val req = SearchRequest(
            daycareId = daycareId,
            query = SearchQuery(instanceIds = uniq, merge = "RRF"),
            filters = SearchFilters(species = species),
            topKImages = topKImages,
            perQueryLimit = perQueryLimit
        )

        val resp = api.search(req)
        resp.results.map { it.imageId }
    }

    suspend fun listServerImages(
        baseUrl: String,
        daycareId: String,
        limit: Int = 300,
        offset: Int = 0
    ): ImagesListResponse = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val api = PetApiClient(baseUrl = baseUrl, resolver = resolver)
        api.listImages(daycareId = daycareId, limit = limit, offset = offset)
    }

    suspend fun getServerImageMeta(
        baseUrl: String,
        imageId: String
    ): ImageMetaResponse = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val api = PetApiClient(baseUrl = baseUrl, resolver = resolver)
        val meta = api.getImageMeta(imageId)
        val img = meta.image
        val fixedImg = img.copy(
            rawUrl = api.absoluteImageUrl(img.rawUrl),
            thumbUrl = api.absoluteImageUrl(img.thumbUrl)
        )
        meta.copy(image = fixedImg)
    }

    suspend fun labelInstance(
        baseUrl: String,
        daycareId: String,
        labeledBy: String?,
        instanceId: String,
        petId: String
    ) = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val api = PetApiClient(baseUrl = baseUrl, resolver = resolver)

        api.setLabels(
            LabelRequest(
                daycareId = daycareId,
                assignments = listOf(LabelAssignment(instanceId = instanceId, petId = petId)),
                labeledBy = labeledBy
            )
        )

        instanceDao.setPetId(instanceId, petId)
    }
}
