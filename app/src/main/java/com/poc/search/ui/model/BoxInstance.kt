package com.poc.search.ui.model

/**
 * UI-friendly instance box model.
 * All bbox coordinates are normalized to [0..1].
 */
data class BoxInstance(
    val instanceId: String,
    val confidence: Double,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val species: String? = null,
    val classId: Int? = null,
    val petId: String? = null
)
