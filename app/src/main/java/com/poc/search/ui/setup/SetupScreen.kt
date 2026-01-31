package com.poc.search.ui.setup


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poc.search.UiState
import com.poc.search.model.ResponseFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    uiState: UiState,
    onSetBaseUrl: (String) -> Unit,
    onSetTopK: (Int) -> Unit,
    onSetThreshold: (Float) -> Unit,
    onSetBatchSize: (Int) -> Unit,
    onSetFormat: (ResponseFormat) -> Unit,
    onPickIncoming: (Uri) -> Unit,
    onPickReference: (Uri) -> Unit,
    onPickOutput: (Uri) -> Unit,
    onStart: () -> Unit,
    onTestHealth: () -> Unit,
    onClearMessage: () -> Unit
) {
    // 초기 위치를 /sdcard/Download 폴더로 제안
    val initialUri = remember {
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
    }

    val incomingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onPickIncoming(it) }
    }

    val referenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onPickReference(uri) }

    val outputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onPickOutput(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dog Photo PoC (Server Embedding)", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = uiState.baseUrl,
            onValueChange = onSetBaseUrl,
            label = { Text("Server Base URL") },
            supportingText = { Text("예: http://<SERVER_IP>:8000") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTestHealth) { Text("Health Test") }
            TextButton(onClick = onClearMessage) { Text("Clear Message") }
        }

        FolderRow(
            label = "Incoming 폴더(약 2,000장)",
            uri = uiState.incomingRoot,
            onPick = { incomingLauncher.launch(initialUri) }
        )
        FolderRow(
            label = "Reference 폴더(dogId별 하위폴더)",
            uri = uiState.referenceRoot,
            onPick = { referenceLauncher.launch(initialUri) }
        )
        FolderRow(
            label = "Output 폴더(Export 대상)",
            uri = uiState.outputRoot,
            onPick = { outputLauncher.launch(initialUri) }
        )

        Divider()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uiState.topK.toString(),
                onValueChange = { it.toIntOrNull()?.let(onSetTopK) },
                label = { Text("TopK") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.batchSize.toString(),
                onValueChange = { it.toIntOrNull()?.let(onSetBatchSize) },
                label = { Text("BatchSize") },
                modifier = Modifier.weight(1f)
            )
        }

        Text("Unknown Threshold: ${"%.3f".format(uiState.threshold)}")
        Slider(
            value = uiState.threshold,
            onValueChange = onSetThreshold,
            valueRange = 0.1f..0.9f
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = uiState.format == ResponseFormat.F16,
                onClick = { onSetFormat(ResponseFormat.F16) },
                label = { Text("format=f16") }
            )
            FilterChip(
                selected = uiState.format == ResponseFormat.F32,
                onClick = { onSetFormat(ResponseFormat.F32) },
                label = { Text("format=f32") }
            )
        }

        Text(
            "Reference 폴더 규격 예:\n" +
                    "reference/\n" +
                    "  dog_001/  (이미지 여러 장)\n" +
                    "  dog_002/\n",
            style = MaterialTheme.typography.bodySmall
        )

        val ready = uiState.incomingRoot != null && uiState.referenceRoot != null && uiState.outputRoot != null

        Button(
            onClick = onStart,
            enabled = ready,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start (Scan → Embed → Classify)") }

        uiState.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FolderRow(label: String, uri: Uri?, onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) { Text("Select") }
            Text(
                text = uri?.toString() ?: "Not selected",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
