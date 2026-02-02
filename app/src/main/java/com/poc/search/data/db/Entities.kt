package com.poc.search.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UploadState { PENDING, UPLOADED, FAILED }

@Entity(tableName = "local_images")
data class LocalImageEntity(
    @PrimaryKey val localUri: String,
    val daycareId: String,
    val serverImageId: String? = null,
    val capturedAtEpochMs: Long,
    val uploadedAtEpochMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val sortRank: Int? = null
)

@Entity(tableName = "local_instances")
data class LocalInstanceEntity(
    @PrimaryKey val instanceId: String,
    val localUri: String,
    val serverImageId: String,
    val classId: Int,
    val species: String,
    val confidence: Double,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val petId: String? = null,
    val embeddingType: String? = null,
    val modelVersion: String? = null
)
