package com.poc.search.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val instances: List<InstanceOut> // IngestDto.kt에 있는 InstanceOut을 사용
)
