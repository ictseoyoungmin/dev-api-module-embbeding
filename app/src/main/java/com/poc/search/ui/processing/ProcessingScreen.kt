package com.poc.search.ui.processing


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poc.search.UiState
import com.poc.search.model.Progress

@Composable
fun ProcessingScreen(
    uiState: UiState,
    onCancel: () -> Unit,
    onClearMessage: () -> Unit
) {
    val p = uiState.progress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Processing...", style = MaterialTheme.typography.titleLarge)

        when (p) {
            is Progress.Scanning -> {
                Text("Scanning... found=${p.found}")
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is Progress.EmbeddingRef -> {
                Text("Embedding Reference: ${p.dogDone}/${p.dogTotal}")
                LinearProgressIndicator(
                    progress = (p.dogDone.toFloat() / p.dogTotal.coerceAtLeast(1)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is Progress.EmbeddingIncoming -> {
                Text("Embedding Incoming: ${p.done}/${p.total}")
                LinearProgressIndicator(
                    progress = if (p.total > 0) p.done.toFloat() / p.total else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is Progress.Classifying -> {
                Text("Classifying: ${p.done}/${p.total}")
                LinearProgressIndicator(
                    progress = if (p.total > 0) p.done.toFloat() / p.total else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is Progress.Exporting -> {
                Text("Exporting: ${p.done}/${p.total}")
                LinearProgressIndicator(
                    progress = if (p.total > 0) p.done.toFloat() / p.total else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Progress.Done -> {
                Text("Done")
            }
            null -> Text("...")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel) { Text("Cancel") }
            TextButton(onClick = onClearMessage) { Text("Clear Message") }
        }

        uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
