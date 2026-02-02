package com.poc.petgalleryxml.ui.work

import com.poc.petgalleryxml.data.api.dto.BBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExemplarItem(
    val imageId: String,
    val instanceId: String,
    val thumbUrl: String? = null,
    val bbox: BBox? = null
)

object ExemplarStore {
    private val _items = MutableStateFlow<List<ExemplarItem>>(emptyList())
    val items: StateFlow<List<ExemplarItem>> = _items.asStateFlow()

    fun add(item: ExemplarItem) {
        val cur = _items.value
        if (cur.any { it.instanceId == item.instanceId }) return
        _items.value = cur + item
    }

    fun remove(instanceId: String) {
        _items.value = _items.value.filterNot { it.instanceId == instanceId }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
