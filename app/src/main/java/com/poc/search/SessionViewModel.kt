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

data class UiState(
    val baseUrl: String = "http://<SERVER_IP>:8000",
    val incomingRoot: Uri? = null,
    val referenceRoot: Uri? = null,
    val outputRoot: Uri? = null,
    val topK: Int = 5,
    val threshold: Float = 0.42f,
    val batchSize: Int = 16,
    val format: ResponseFormat = ResponseFormat.F16,

    val progress: Progress? = null,
    val grouped: Map<String, List<PhotoItem>>? = null,
    val message: String? = null
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
        _state.update { it.copy(progress = null, grouped = null, message = null) }
    }

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(progress = null, message = "Cancelled") }
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
            _state.update { it.copy(message = "폴더(incoming/reference/output)를 모두 선택하세요.") }
            return
        }
        if (!s.baseUrl.startsWith("http")) {
            _state.update { it.copy(message = "서버 URL이 올바르지 않습니다.") }
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
                _state.update { it.copy(grouped = null, progress = Progress.Scanning(0), message = null) }

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

                // 빈 그룹 제거(Unknown 제외)
                val cleaned = grouped
                    .mapValues { it.value.toMutableList() }
                    .toMutableMap()
                cleaned.entries.removeIf { (k, v) -> k != UNKNOWN_KEY && v.isEmpty() }

                _state.update { it.copy(progress = null, grouped = cleaned) }
            } catch (e: Exception) {
                _state.update { it.copy(progress = null, message = "실패: ${e.message}") }
            }
        }
    }

    fun reassign(photoUri: Uri, newDogIdOrNull: String?) {
        val grouped = _state.value.grouped ?: return
        val mutable = grouped.mapValues { it.value.toMutableList() }.toMutableMap()

        var moved: PhotoItem? = null
        var oldKey: String? = null

        for ((k, list) in mutable) {
            val idx = list.indexOfFirst { it.uri == photoUri }
            if (idx >= 0) {
                moved = list.removeAt(idx)
                oldKey = k
                break
            }
        }
        if (moved == null) return

        val newKey = newDogIdOrNull ?: UNKNOWN_KEY
        val updated = moved.copy(
            assignment = moved.assignment.copy(bestDogId = newDogIdOrNull)
        )
        mutable.getOrPut(newKey) { mutableListOf() }.add(updated)

        // 빈 그룹 제거(Unknown 제외)
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
