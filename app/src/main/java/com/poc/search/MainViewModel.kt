package com.poc.search

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poc.search.data.db.UploadState
import com.poc.search.data.repo.GalleryRepository
import com.poc.search.data.api.dto.ImageMetaResponse
import com.poc.search.ui.model.ServerImageItem
import com.poc.search.data.db.LocalImageEntity
import com.poc.search.data.db.LocalInstanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class SelectionMode { MANUAL, AUTO_LARGEST }

data class SearchHistoryItem(val petId: String, val instanceIds: List<String>)

data class UiState(
    val baseUrl: String = "http://10.0.2.2:8001",
    val daycareId: String = "dc_001",
    val trainerId: String = "",
    val isBusy: Boolean = false,
    val message: String? = null,
    val selectedLocalUri: String? = null,
    val selectedInstanceId: String? = null,
    val exemplarInstanceIds: List<String> = emptyList(),
    val jpegMaxSidePx: Int = 2048,
    val jpegQuality: Int = 92,
    val gridColumns: Int = 4,
    val selectionMode: SelectionMode = SelectionMode.MANUAL,
    val searchHistory: List<SearchHistoryItem> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext
    private val repo: GalleryRepository = GalleryRepository(
        context = context,
        db = (app as App).db
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val _serverImagesRaw = MutableStateFlow<List<ServerImageItem>>(emptyList())
    private val _serverOrder = MutableStateFlow<List<String>>(emptyList())
    private val _serverSelectedMeta = MutableStateFlow<ImageMetaResponse?>(null)

    val serverSelectedMeta: StateFlow<ImageMetaResponse?> = _serverSelectedMeta

    val serverImages: StateFlow<List<ServerImageItem>> = combine(_serverImagesRaw, _serverOrder) { items, order ->
        if (order.isEmpty()) return@combine items
        val byId = items.associateBy { it.imageId }
        val ordered = order.mapNotNull { byId[it] }
        val remaining = items.filter { !order.contains(it.imageId) }
        ordered + remaining
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val images: StateFlow<List<LocalImageEntity>> = _ui
        .map { it.daycareId }
        .distinctUntilChanged()
        .flatMapLatest { daycareId -> repo.observeImages(daycareId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedInstances: StateFlow<List<LocalInstanceEntity>> = _ui
        .map { it.selectedLocalUri }
        .distinctUntilChanged()
        .flatMapLatest { localUri ->
            if (localUri.isNullOrBlank()) flowOf(emptyList()) else repo.observeInstances(localUri)
        }
        .onEach { instances ->
            if (_ui.value.selectionMode == SelectionMode.AUTO_LARGEST && instances.isNotEmpty()) {
                instances.maxByOrNull { (it.x2 - it.x1) * (it.y2 - it.y1) }?.let { selectInstance(it.instanceId) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setGridColumns(cols: Int) = _ui.update { it.copy(gridColumns = cols) }
    fun setSelectionMode(mode: SelectionMode) = _ui.update { it.copy(selectionMode = mode) }
    fun setBaseUrl(v: String) = _ui.update { it.copy(baseUrl = v.trim()) }
    fun setDaycareId(v: String) = _ui.update { it.copy(daycareId = v.trim()) }
    fun setTrainerId(v: String) = _ui.update { it.copy(trainerId = v) }
    fun clearMessage() = _ui.update { it.copy(message = null) }

    fun selectLocalUri(localUri: String?) {
        _ui.update { it.copy(selectedLocalUri = localUri, selectedInstanceId = null) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try {
                (context as App).db.clearAllTables() 
                _ui.update { it.copy(isBusy = false, message = "초기화 완료") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    private fun joinApi(baseUrl: String, path: String): String {
        val base = baseUrl.trimEnd('/')
        return base + (if (path.startsWith("/")) path else "/$path")
    }

    fun scanDefaultFolder() {
        val path = "/sdcard/Download/images"
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            _ui.update { it.copy(message = "폴더 없음: $path") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = "스캔 중...") }
            try {
                val files = dir.listFiles { f -> 
                    val name = f.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                }?.map { Uri.fromFile(it) } ?: emptyList()
                
                if (files.isNotEmpty()) {
                    repo.addPickedUris(_ui.value.daycareId, files)
                    _ui.update { it.copy(message = "${files.size}장 스캔 완료. 자동 업로드 시작...") }
                    
                    files.forEachIndexed { index, uri ->
                        _ui.update { it.copy(message = "업로드 중 (${index + 1}/${files.size})") }
                        try {
                            repo.ingestOne(_ui.value.baseUrl, _ui.value.daycareId, _ui.value.trainerId, uri.toString())
                        } catch (e: Exception) { }
                    }
                    _ui.update { it.copy(isBusy = false, message = "모든 사진 업로드 완료!") }
                } else {
                    _ui.update { it.copy(isBusy = false, message = "사진 없음") }
                }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun refreshServerGallery() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            try {
                val resp = repo.listServerImages(baseUrl = s.baseUrl, daycareId = s.daycareId)
                val mapped = resp.items.map {
                    ServerImageItem(
                        imageId = it.imageId,
                        daycareId = it.daycareId,
                        thumbUrl = joinApi(s.baseUrl, it.thumbUrl),
                        rawUrl = joinApi(s.baseUrl, it.rawUrl),
                        capturedAt = it.capturedAt,
                        uploadedAt = it.uploadedAt,
                        width = it.width,
                        height = it.height,
                        instanceCount = it.instanceCount
                    )
                }
                _serverImagesRaw.value = mapped
                _serverOrder.value = emptyList()
                _ui.update { it.copy(isBusy = false, message = "${mapped.size}장 로드됨") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "로드 실패") } }
        }
    }

    fun selectServerImage(imageId: String) {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            try {
                _serverSelectedMeta.value = null
                val meta = repo.getServerImageMeta(baseUrl = s.baseUrl, imageId = imageId)
                _serverSelectedMeta.value = meta
                
                if (s.selectionMode == SelectionMode.AUTO_LARGEST && !meta.instances.isNullOrEmpty()) {
                    val largest = meta.instances.maxByOrNull { (it.bbox.x2 - it.bbox.x1) * (it.bbox.y2 - it.bbox.y1) }
                    largest?.let { selectInstance(it.instanceId) }
                }
                _ui.update { it.copy(isBusy = false) }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "로드 실패") } }
        }
    }

    fun searchAndSortServer(petId: String) {
        val s = _ui.value
        val instanceIds = if (s.exemplarInstanceIds.isNotEmpty()) s.exemplarInstanceIds else listOfNotNull(s.selectedInstanceId)
        if (instanceIds.isEmpty()) return

        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = "$petId 서버 정렬 중...") }
            try {
                val ordered = repo.searchByInstances(s.baseUrl, s.daycareId, instanceIds, "DOG")
                _serverOrder.value = ordered
                val newHistory = (listOf(SearchHistoryItem(petId, instanceIds)) + s.searchHistory).distinctBy { it.petId }.take(10)
                _ui.update { it.copy(isBusy = false, message = "정렬 완료", searchHistory = newHistory) }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun searchAndSort(petId: String) {
        val s = _ui.value
        val instanceIds = if (s.exemplarInstanceIds.isNotEmpty()) s.exemplarInstanceIds else listOfNotNull(s.selectedInstanceId)
        if (instanceIds.isEmpty()) return

        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = "$petId 정렬 중...") }
            try {
                val ordered = repo.searchByInstances(s.baseUrl, s.daycareId, instanceIds, "DOG")
                repo.applySearchOrder(s.daycareId, ordered)
                val newHistory = (listOf(SearchHistoryItem(petId, instanceIds)) + s.searchHistory).distinctBy { it.petId }.take(10)
                _ui.update { it.copy(isBusy = false, message = "정렬 완료", searchHistory = newHistory) }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun searchByHistory(item: SearchHistoryItem) {
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = "${item.petId} 정렬...") }
            try {
                val ordered = repo.searchByInstances(_ui.value.baseUrl, _ui.value.daycareId, item.instanceIds, "DOG")
                repo.applySearchOrder(_ui.value.daycareId, ordered)
                _serverOrder.value = ordered
                _ui.update { it.copy(isBusy = false, message = "정렬 완료") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun labelSelectedInstance(petId: String) {
        val s = _ui.value
        val id = s.selectedInstanceId ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try {
                repo.labelInstance(s.baseUrl, s.daycareId, s.trainerId, id, petId)
                _ui.update { it.copy(isBusy = false, message = "라벨링 완료") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun ingest(localUri: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try {
                repo.ingestOne(_ui.value.baseUrl, _ui.value.daycareId, _ui.value.trainerId, localUri)
                _ui.update { it.copy(isBusy = false, message = "업로드 완료") }
                onDone?.invoke()
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }

    fun selectInstance(instanceId: String?) { _ui.update { it.copy(selectedInstanceId = instanceId) } }
    fun addExemplar(instanceId: String) {
        val cur = _ui.value.exemplarInstanceIds
        if (!cur.contains(instanceId)) _ui.update { it.copy(exemplarInstanceIds = cur + instanceId) }
    }
    fun clearExemplars() { _ui.update { it.copy(exemplarInstanceIds = emptyList()) } }
    fun addPickedUris(uris: List<Uri>) {
        viewModelScope.launch { try { repo.addPickedUris(_ui.value.daycareId, uris) } catch (e: Exception) { _ui.update { it.copy(message = "실패") } } }
    }
    fun clearSort() { viewModelScope.launch { repo.applySearchOrder(_ui.value.daycareId, emptyList()) } }
    fun clearServerSort() { _serverOrder.value = emptyList() }
    fun addSelectedToExemplar() { _ui.value.selectedInstanceId?.let { addExemplar(it) } }
    fun testHealth() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try {
                val api = com.poc.search.data.api.PetApiClient(s.baseUrl, context.contentResolver)
                val resp = api.health()
                _ui.update { it.copy(isBusy = false, message = "Health OK") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "연결 실패") } }
        }
    }
}
