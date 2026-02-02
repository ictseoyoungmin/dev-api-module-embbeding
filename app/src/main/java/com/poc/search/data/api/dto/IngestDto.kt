package com.poc.search.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("class_id") val classId: Int = 0,
    val species: String,
    val confidence: Double = 0.0,
    val bbox: BBox,
    @SerialName("pet_id") val petId: String? = null,
    @SerialName("is_exemplar") val isExemplar: Boolean = false,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val embedding: List<Double>? = null,
    @SerialName("embedding_meta") val embeddingMeta: EmbeddingMeta? = null
)

@Serializable
data class IngestResponse(
    val image: ImageMeta,
    val instances: List<InstanceOut>
)
