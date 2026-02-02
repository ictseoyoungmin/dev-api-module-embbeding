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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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
    val jpegQuality: Int = 92
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
            if (localUri.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repo.observeInstances(localUri)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "실패: ${e.message}") }
            }
        }
    }

    private fun joinApi(baseUrl: String, path: String): String {
        if (path.startsWith("http")) return path
        val base = baseUrl.trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return base + cleanPath
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
                _ui.update { it.copy(isBusy = false, message = "${mapped.size}장의 사진 로드 완료") }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "로드 실패: ${e.message}") }
            }
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
                _ui.update { it.copy(isBusy = false, selectedInstanceId = null) }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "실패: ${e.message}") }
            }
        }
    }

    fun searchAndSortServer() {
        val s = _ui.value
        val instanceIds = if (s.exemplarInstanceIds.isNotEmpty()) s.exemplarInstanceIds else listOfNotNull(s.selectedInstanceId)
        if (instanceIds.isEmpty()) return

        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            try {
                val ordered = repo.searchByInstances(
                    baseUrl = s.baseUrl,
                    daycareId = s.daycareId,
                    instanceIds = instanceIds,
                    species = "DOG"
                )
                _serverOrder.value = ordered
                _ui.update { it.copy(isBusy = false, message = "서버 정렬 완료") }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "검색 실패: ${e.message}") }
            }
        }
    }

    fun searchAndSort() {
        val s = _ui.value
        val instanceIds = if (s.exemplarInstanceIds.isNotEmpty()) s.exemplarInstanceIds else listOfNotNull(s.selectedInstanceId)
        if (instanceIds.isEmpty()) return

        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = "정렬 중...") }
            try {
                val ordered = repo.searchByInstances(
                    baseUrl = s.baseUrl,
                    daycareId = s.daycareId,
                    instanceIds = instanceIds,
                    species = "DOG"
                )
                repo.applySearchOrder(s.daycareId, ordered)
                _ui.update { it.copy(isBusy = false, message = "정렬이 완료되었습니다. (${ordered.size}개)") }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "정렬 실패: ${e.message}") }
            }
        }
    }

    fun scanDefaultFolder() {
        val daycareId = _ui.value.daycareId
        val path = "/sdcard/Download/images"
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            _ui.update { it.copy(message = "폴더 없음: $path") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try {
                val files = dir.listFiles { f -> 
                    val name = f.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                }?.map { Uri.fromFile(it) } ?: emptyList()
                if (files.isNotEmpty()) {
                    repo.addPickedUris(daycareId, files)
                    _ui.update { it.copy(isBusy = false, message = "${files.size}장 스캔 완료") }
                } else {
                    _ui.update { it.copy(isBusy = false, message = "사진 파일 없음") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "스캔 실패") }
            }
        }
    }

    fun ingest(localUri: String) {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            try {
                repo.ingestOne(
                    baseUrl = s.baseUrl,
                    daycareId = s.daycareId,
                    trainerId = s.trainerId.takeIf { it.isNotBlank() },
                    localUri = localUri,
                    jpegMaxSidePx = s.jpegMaxSidePx,
                    jpegQuality = s.jpegQuality
                )
                _ui.update { it.copy(isBusy = false, message = "업로드 및 분석 완료") }
            } catch (e: Exception) {
                _ui.update { it.copy(isBusy = false, message = "실패: ${e.message}") }
            }
        }
    }

    fun selectInstance(instanceId: String?) { _ui.update { it.copy(selectedInstanceId = instanceId) } }
    fun addExemplar(instanceId: String) {
        val cur = _ui.value.exemplarInstanceIds
        if (!cur.contains(instanceId)) _ui.update { it.copy(exemplarInstanceIds = cur + instanceId) }
    }
    fun removeExemplar(instanceId: String) {
        _ui.update { it.copy(exemplarInstanceIds = _ui.value.exemplarInstanceIds.filter { it != instanceId }) }
    }
    fun clearExemplars() { _ui.update { it.copy(exemplarInstanceIds = emptyList()) } }
    fun addPickedUris(uris: List<Uri>) {
        viewModelScope.launch { try { repo.addPickedUris(_ui.value.daycareId, uris) } catch (e: Exception) { _ui.update { it.copy(message = "추가 실패") } } }
    }
    fun clearSort() { viewModelScope.launch { repo.applySearchOrder(_ui.value.daycareId, emptyList()) } }
    fun clearServerSort() { _serverOrder.value = emptyList() }
    fun labelSelectedInstance(petId: String) {
        val s = _ui.value
        val id = s.selectedInstanceId ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true) }
            try { repo.labelInstance(s.baseUrl, s.daycareId, s.trainerId, id, petId)
                _ui.update { it.copy(isBusy = false, message = "라벨링 완료") }
            } catch (e: Exception) { _ui.update { it.copy(isBusy = false, message = "실패") } }
        }
    }
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
