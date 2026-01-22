package com.poc.search.ui.results

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.poc.search.UiState
import com.poc.search.model.PhotoItem
import com.poc.search.model.UNKNOWN_KEY

@Composable
fun ResultsScreen(
    uiState: UiState,
    onBackToSetup: () -> Unit,
    onExport: () -> Unit,
    onReassign: (photoUri: Uri, newDogIdOrNull: String?) -> Unit,
    onClearMessage: () -> Unit
) {
    val grouped = uiState.grouped ?: emptyMap()
    val keys = remember(grouped) {
        val normal = grouped.keys.filter { it != UNKNOWN_KEY }.sorted()
        listOf(UNKNOWN_KEY) + normal
    }

    var selectedKey by remember { mutableStateOf<String?>(null) }
    var dialogPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackToSetup) { Text("Reset") }
            Button(onClick = onExport) { Text("Export") }
            TextButton(onClick = onClearMessage) { Text("Clear Message") }
        }

        Spacer(Modifier.height(8.dp))

        uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(8.dp))

        if (selectedKey == null) {
            Text("Groups", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(keys.size) { idx ->
                    val k = keys[idx]
                    val count = grouped[k]?.size ?: 0
                    val title = if (k == UNKNOWN_KEY) "Unknown" else k
                    GroupRow(title = title, count = count) {
                        selectedKey = k
                    }
                }
            }
        } else {
            val k = selectedKey!!
            val title = if (k == UNKNOWN_KEY) "Unknown" else k
            val photos = grouped[k].orEmpty()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectedKey = null }) { Text("Back") }
                Text("$title (${photos.size})", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { item ->
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { dialogPhoto = item }
                    )
                }
            }
        }
    }

    if (dialogPhoto != null) {
        val photo = dialogPhoto!!
        val options = keys // UNKNOWN_KEY 포함

        AlertDialog(
            onDismissRequest = { dialogPhoto = null },
            title = { Text("Reassign") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("현재: ${photo.assignment.bestDogId ?: "Unknown"}")
                    Text("Top candidates:")
                    photo.assignment.top.take(5).forEach {
                        Text("- ${it.dogId}: ${"%.3f".format(it.score)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("다시 배정:")
                    options.forEach { k ->
                        val label = if (k == UNKNOWN_KEY) "Unknown" else k
                        Text(
                            label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newDogIdOrNull = if (k == UNKNOWN_KEY) null else k
                                    onReassign(photo.uri, newDogIdOrNull)
                                    dialogPhoto = null
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogPhoto = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun GroupRow(title: String, count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("$count", style = MaterialTheme.typography.titleMedium)
        }
    }
}
