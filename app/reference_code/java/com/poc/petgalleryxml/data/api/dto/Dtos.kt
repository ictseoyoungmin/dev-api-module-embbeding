package com.poc.petgalleryxml.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BBox(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double
)

@Serializable
data class ImageMeta(
    @SerialName("image_id") val imageId: String,
    @SerialName("daycare_id") val daycareId: String,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String,
    val width: Int,
    val height: Int,
    @SerialName("storage_path") val storagePath: String
)

@Serializable
data class EmbeddingMeta(
    @SerialName("embedding_type") val embeddingType: String,
    val dim: Int,
    val dtype: String,
    @SerialName("l2_normalized") val l2Normalized: Boolean,
    @SerialName("model_version") val modelVersion: String
)

@Serializable
data class InstanceOut(
    @SerialName("instance_id") val instanceId: String,
    @SerialName("class_id") val classId: Int,
    val species: String,
    val confidence: Double,
    val bbox: BBox,
    @SerialName("pet_id") val petId: String? = null,
    val embedding: List<Double>? = null,
    @SerialName("embedding_meta") val embeddingMeta: EmbeddingMeta? = null
)

@Serializable
data class IngestResponse(
    val image: ImageMeta,
    val instances: List<InstanceOut>
)

// ---- Labels ----

@Serializable
data class LabelAssignment(
    @SerialName("instance_id") val instanceId: String,
    @SerialName("pet_id") val petId: String,
    val source: String = "MANUAL",
    val confidence: Double = 1.0
)

@Serializable
data class LabelRequest(
    @SerialName("daycare_id") val daycareId: String,
    val assignments: List<LabelAssignment>,
    @SerialName("labeled_by") val labeledBy: String? = null
)

@Serializable
data class LabelResponseItem(
    @SerialName("instance_id") val instanceId: String,
    @SerialName("pet_id") val petId: String,
    val updated: Boolean
)

@Serializable
data class LabelResponse(
    @SerialName("labeled_at") val labeledAt: String,
    val items: List<LabelResponseItem>
)

// ---- Search ----

@Serializable
data class SearchQuery(
    @SerialName("instance_ids") val instanceIds: List<String>? = null,
    val merge: String = "RRF" // "MAX" or "RRF"
)

@Serializable
data class SearchFilters(
    val species: String? = null, // "DOG" or "CAT"
    @SerialName("captured_from") val capturedFrom: String? = null,
    @SerialName("captured_to") val capturedTo: String? = null
)

@Serializable
data class SearchRequest(
    @SerialName("daycare_id") val daycareId: String,
    val query: SearchQuery,
    val filters: SearchFilters? = null,
    @SerialName("top_k_images") val topKImages: Int = 200,
    @SerialName("per_query_limit") val perQueryLimit: Int = 400
)

@Serializable
data class BestMatch(
    @SerialName("instance_id") val instanceId: String,
    val bbox: BBox? = null,
    val score: Double
)

@Serializable
data class SearchResultItem(
    @SerialName("image_id") val imageId: String,
    val score: Double,
    @SerialName("best_match") val bestMatch: BestMatch
)

@Serializable
data class SearchResponse(
    @SerialName("query_debug") val queryDebug: Map<String, JsonElement> = emptyMap(),
    val results: List<SearchResultItem>
)

// ---- Server Gallery ----

@Serializable
data class GalleryImageItem(
    @SerialName("image_id") val imageId: String,
    @SerialName("daycare_id") val daycareId: String,
    @SerialName("trainer_id") val trainerId: String? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String,
    val width: Int,
    val height: Int,
    @SerialName("raw_url") val rawUrl: String,
    @SerialName("thumb_url") val thumbUrl: String,
    @SerialName("instance_count") val instanceCount: Int = 0
)

@Serializable
data class ImagesListResponse(
    @SerialName("daycare_id") val daycareId: String,
    val count: Int,
    val items: List<GalleryImageItem>
)

@Serializable
data class ImageMetaResponse(
    val image: GalleryImageItem,
    val instances: List<InstanceOut>
)
