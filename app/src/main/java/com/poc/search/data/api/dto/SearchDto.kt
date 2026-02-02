package com.poc.search.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchQuery(
    @SerialName("instance_ids") val instanceIds: List<String>? = null,
    val merge: String = "RRF" // "MAX" or "RRF"
)

@Serializable
data class SearchFilters(
    val species: String? = null, // "DOG" or "CAT"
    @SerialName("captured_from") val capturedFrom: String? = null, // ISO8601
    @SerialName("captured_to") val capturedTo: String? = null      // ISO8601
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
