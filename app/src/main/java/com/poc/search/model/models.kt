package com.poc.search.model

import android.net.Uri

const val UNKNOWN_KEY = "__UNKNOWN__"

data class SessionConfig(
    val baseUrl: String,
    val incomingRoot: Uri,
    val referenceRoot: Uri,
    val outputRoot: Uri,
    val topK: Int = 5,
    val unknownThreshold: Float = 0.42f,
    val batchSize: Int = 16,
    val maxSidePx: Int = 1024,
    val jpegQuality: Int = 90,
    val responseFormat: ResponseFormat = ResponseFormat.F16
)

data class DogRef(
    val dogId: String,
    val refUris: List<Uri>
)

data class DogPrototype(
    val dogId: String,
    val vector: FloatArray // L2 normalized
)

data class Candidate(val dogId: String, val score: Float)

data class Assignment(
    val bestDogId: String?,   // null이면 Unknown
    val bestScore: Float,
    val top: List<Candidate>,
    val vector: FloatArray? = null // 원본 벡터 보관용 추가
)

data class PhotoItem(
    val uri: Uri,
    val assignment: Assignment,
    val tempScore: Float = 0f // 검색 시 임시 점수 보관용 추가
)

sealed interface Progress {
    data class Scanning(val found: Int) : Progress
    data class EmbeddingRef(val dogDone: Int, val dogTotal: Int) : Progress
    data class EmbeddingIncoming(val done: Int, val total: Int) : Progress
    data class Classifying(val done: Int, val total: Int) : Progress
    data class Exporting(val done: Int, val total: Int) : Progress
    data object Done : Progress
}

enum class ResponseFormat(val value: String) { F32("f32"), F16("f16") }
