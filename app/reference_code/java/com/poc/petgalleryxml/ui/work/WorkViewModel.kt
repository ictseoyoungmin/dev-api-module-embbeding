package com.poc.petgalleryxml.ui.work

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.poc.petgalleryxml.data.api.PetApiClient
import com.poc.petgalleryxml.data.api.dto.GalleryImageItem
import com.poc.petgalleryxml.data.api.dto.SearchRequest
import com.poc.petgalleryxml.data.api.dto.SearchQuery
import com.poc.petgalleryxml.data.prefs.AppPrefs
import kotlinx.coroutines.launch

class WorkViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs(app)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _mode = MutableLiveData(Mode.SERVER)
    val mode: LiveData<Mode> = _mode

    private val _images = MutableLiveData<List<GalleryImageItem>>(emptyList())
    val images: LiveData<List<GalleryImageItem>> = _images

    /**
     * image_id -> score (유사도 정렬일 때만 사용)
     */
    private val _scores = MutableLiveData<Map<String, Double>>(emptyMap())
    val scores: LiveData<Map<String, Double>> = _scores

    private val _sortHint = MutableLiveData("최신순")
    val sortHint: LiveData<String> = _sortHint

    fun setMode(m: Mode) {
        _mode.value = m
        // 현재는 SERVER만 지원. LOCAL은 placeholder.
        if (m == Mode.SERVER) refreshServerImages()
        else {
            _images.value = emptyList()
            _scores.value = emptyMap()
            _sortHint.value = "(내 기기 모드는 PoC에서 미지원)"
        }
    }

    fun refreshServerImages() {
        if (_mode.value != Mode.SERVER) return
        val baseUrl = prefs.baseUrl
        val daycareId = prefs.daycareId

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val api = PetApiClient(baseUrl, getApplication<Application>().contentResolver)
                val resp = api.listImages(daycareId = daycareId, limit = 200, offset = 0)
                _images.value = resp.items
                _scores.value = emptyMap()
                _sortHint.value = "최신순"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun rrfSearchWithExemplars(exemplars: List<ExemplarItem>) {
        if (exemplars.isEmpty()) return
        val baseUrl = prefs.baseUrl
        val daycareId = prefs.daycareId

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val api = PetApiClient(baseUrl, getApplication<Application>().contentResolver)
                val req = SearchRequest(
                    daycareId = daycareId,
                    query = SearchQuery(instanceIds = exemplars.map { it.instanceId }, merge = "RRF"),
                    filters = null,
                    topKImages = 200,
                    perQueryLimit = 400
                )
                val resp = api.search(req)

                val scoreMap = resp.results.associate { it.imageId to it.score }
                _scores.value = scoreMap

                val current = _images.value.orEmpty()
                val rank = resp.results.mapIndexed { idx, it -> it.imageId to idx }.toMap()

                val sorted = current.sortedWith(compareBy(
                    { rank[it.imageId] ?: Int.MAX_VALUE },
                    { it.uploadedAt }
                ))

                _images.value = sorted
                _sortHint.value = "대표샷 ${exemplars.size}개 RRF 유사도 순"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    enum class Mode { LOCAL, SERVER }
}
