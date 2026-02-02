package com.poc.search.ui.model

/**
 * Server gallery item used by UI.
 * URLs are absolute (including baseUrl).
 */
data class ServerImageItem(
    val imageId: String,
    val daycareId: String,
    val thumbUrl: String,
    val rawUrl: String,
    val capturedAt: String? = null,
    val uploadedAt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val instanceCount: Int = 0
)
