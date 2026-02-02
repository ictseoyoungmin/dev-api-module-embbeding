package com.poc.search.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
