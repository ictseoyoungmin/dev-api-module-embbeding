package com.poc.search


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poc.search.data.api.DogfaceApiClient
import com.poc.search.data.file.DocumentTreeScanner
import com.poc.search.data.file.Exporter
import com.poc.search.domain.Classifier
import com.poc.search.domain.PrototypeBuilder
import com.poc.search.domain.SessionEngine
import com.poc.search.domain.VectorMath
import com.poc.search.model.PhotoItem
import com.poc.search.model.Progress
import com.poc.search.model.ResponseFormat
import com.poc.search.model.SessionConfig
import com.poc.search.model.UNKNOWN_KEY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val baseUrl: String = "http://10.0.2.2:8000",
    val incomingRoot: Uri? = Uri.fromFile(File("/sdcard/Download/images")),
    val referenceRoot: Uri? = Uri.fromFile(File("/sdcard/Download/PetPoC/ref")),
    val outputRoot: Uri? = Uri.fromFile(File("/sdcard/Download/PetPoC/outputs")),
    val topK: Int = 5,
    val threshold: Float = 0.42f,
    val batchSize: Int = 16,
    val format: ResponseFormat = ResponseFormat.F16,

    val progress: Progress? = null,
    val grouped: Map<String, List<PhotoItem>>? = null,
    val message: String? = null,
    val searchResults: List<PhotoItem>? = null // 검색(정렬) 결과용 필드 추가
)

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val context = getApplication<Application>()
    private val scanner = DocumentTreeScanner(context)
    private val exporter = Exporter(context)
    private val okHttp = DogfaceApiClient.defaultOkHttp()

    private var job: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun setBaseUrl(v: String) = _state.update { it.copy(baseUrl = v) }
    fun setIncomingRoot(uri: Uri) = _state.update { it.copy(incomingRoot = uri) }
    fun setReferenceRoot(uri: Uri) = _state.update { it.copy(referenceRoot = uri) }
    fun setOutputRoot(uri: Uri) = _state.update { it.copy(outputRoot = uri) }
    fun setTopK(v: Int) = _state.update { it.copy(topK = v.coerceIn(1, 20)) }
    fun setThreshold(v: Float) = _state.update { it.copy(threshold = v.coerceIn(0f, 1f)) }
    fun setBatchSize(v: Int) = _state.update { it.copy(batchSize = v.coerceIn(1, 64)) }
    fun setFormat(v: ResponseFormat) = _state.update { it.copy(format = v) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun reset() {
        job?.cancel()
        _state.update { it.copy(progress = null, grouped = null, message = null, searchResults = null) }
    }

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(progress = null, message = "Cancelled") }
    }

    // ✅ 시나리오의 핵심: 특정 사진을 기준으로 유사도 정렬
    fun searchByPhoto(targetPhoto: PhotoItem) {
        val grouped = _state.value.grouped ?: return
        val targetVector = targetPhoto.assignment.vector ?: return
        
        // 모든 그룹의 사진들을 하나로 합쳐서 유사도 순으로 정렬
        val allPhotos = grouped.values.flatten()
        val sorted = allPhotos.map { photo ->
            val score = photo.assignment.vector?.let { VectorMath.cosineSimilarity(targetVector, it) } ?: 0f
            photo.copy(tempScore = score) // 임시 점수 부여
        }.sortedByDescending { it.tempScore }

        _state.update { it.copy(searchResults = sorted, message = "유사도 순으로 정렬되었습니다.") }
    }

    fun testHealth() {
        val url = _state.value.baseUrl
        val api = DogfaceApiClient(url, context.contentResolver, okHttp)
        viewModelScope.launch {
            try {
                val raw = api.healthRaw()
                _state.update { it.copy(message = "Health OK: $raw") }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Health FAIL: ${e.message}") }
            }
        }
    }

    fun start() {
        val s = _state.value
        val incoming = s.incomingRoot
        val reference = s.referenceRoot
        val output = s.outputRoot

        if (incoming == null || reference == null || output == null) {
            _state.update { it.copy(message = "폴더를 모두 선택하세요.") }
            return
        }

        val cfg = SessionConfig(
            baseUrl = s.baseUrl,
            incomingRoot = incoming,
            referenceRoot = reference,
            outputRoot = output,
            topK = s.topK,
            unknownThreshold = s.threshold,
            batchSize = s.batchSize,
            responseFormat = s.format
        )

        job?.cancel()
        job = viewModelScope.launch {
            try {
                _state.update { it.copy(grouped = null, searchResults = null, progress = Progress.Scanning(0), message = null) }

                val api = DogfaceApiClient(cfg.baseUrl, context.contentResolver, okHttp)
                val engine = SessionEngine(
                    scanner = scanner,
                    api = api,
                    prototypeBuilder = PrototypeBuilder(api),
                    classifier = Classifier()
                )

                val grouped = engine.run(cfg) { p ->
                    _state.update { it.copy(progress = p) }
                }

                _state.update { it.copy(progress = null, grouped = grouped) }
            } catch (e: Exception) {
                _state.update { it.copy(progress = null, message = "실패: ${e.message}") }
            }
        }
    }

    fun reassign(photoUri: Uri, newDogIdOrNull: String?) {
        val grouped = _state.value.grouped ?: return
        val mutable = grouped.mapValues { it.value.toMutableList() }.toMutableMap()

        var moved: PhotoItem? = null
        for ((k, list) in mutable) {
            val idx = list.indexOfFirst { it.uri == photoUri }
            if (idx >= 0) {
                moved = list.removeAt(idx)
                break
            }
        }
        if (moved == null) return

        val newKey = newDogIdOrNull ?: UNKNOWN_KEY
        val updated = moved.copy(
            assignment = moved.assignment.copy(bestDogId = newDogIdOrNull)
        )
        mutable.getOrPut(newKey) { mutableListOf() }.add(updated)
        mutable.entries.removeIf { (k, v) -> k != UNKNOWN_KEY && v.isEmpty() }

        _state.update { it.copy(grouped = mutable) }
    }

    fun export() {
        val s = _state.value
        val output = s.outputRoot ?: return
        val grouped = s.grouped ?: return

        viewModelScope.launch {
            try {
                _state.update { it.copy(progress = Progress.Exporting(0, 1), message = null) }
                exporter.exportGroupedToFolders(output, grouped) { done, total ->
                    _state.update { it.copy(progress = Progress.Exporting(done, total)) }
                }
                _state.update { it.copy(progress = null, message = "Export 완료") }
            } catch (e: Exception) {
                _state.update { it.copy(progress = null, message = "Export 실패: ${e.message}") }
            }
        }
    }
}
