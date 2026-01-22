package com.poc.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import com.poc.search.ui.processing.ProcessingScreen
import com.poc.search.ui.results.ResultsScreen
import com.poc.search.ui.setup.SetupScreen

class MainActivity : ComponentActivity() {

    private val vm: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {

                // ✅ 여기만 핵심 수정: collectAsState는 확장 함수로 호출
                val ui = vm.state.collectAsState().value

                when {
                    ui.grouped != null -> ResultsScreen(
                        uiState = ui,
                        onBackToSetup = { vm.reset() },
                        onExport = { vm.export() },
                        onReassign = { photoUri, newDogIdOrNull -> vm.reassign(photoUri, newDogIdOrNull) },
                        onClearMessage = { vm.clearMessage() }
                    )

                    ui.progress != null -> ProcessingScreen(
                        uiState = ui,
                        onCancel = { vm.cancel() },
                        onClearMessage = { vm.clearMessage() }
                    )

                    else -> SetupScreen(
                        uiState = ui,
                        onSetBaseUrl = vm::setBaseUrl,
                        onSetTopK = vm::setTopK,
                        onSetThreshold = vm::setThreshold,
                        onSetBatchSize = vm::setBatchSize,
                        onSetFormat = vm::setFormat,
                        onPickIncoming = { uri -> takePersistable(uri); vm.setIncomingRoot(uri) },
                        onPickReference = { uri -> takePersistable(uri); vm.setReferenceRoot(uri) },
                        onPickOutput = { uri -> takePersistable(uri); vm.setOutputRoot(uri) },
                        onStart = { vm.start() },
                        onTestHealth = { vm.testHealth() },
                        onClearMessage = { vm.clearMessage() }
                    )
                }
            }
        }
    }

    private fun takePersistable(uri: android.net.Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // 일부 기기/상황에서 persist 실패 가능(현재 세션 접근은 보통 가능)
        }
    }
}
