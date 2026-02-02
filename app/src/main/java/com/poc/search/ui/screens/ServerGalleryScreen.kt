package com.poc.search.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.poc.search.MainViewModel
import com.poc.search.ui.model.ServerImageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerGalleryScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenDetail: (imageId: String) -> Unit
) {
    val ui = vm.ui.collectAsState().value
    val items = vm.serverImages.collectAsState().value

    LaunchedEffect(ui.baseUrl, ui.daycareId) {
        if (items.isEmpty()) {
            vm.refreshServerGallery()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Server Gallery (${ui.daycareId})") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.refreshServerGallery() }) { Text("새로고침") }
                OutlinedButton(onClick = { vm.clearServerSort() }) { Text("정렬 초기화") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.searchAndSortServer() },
                    enabled = ui.exemplarInstanceIds.isNotEmpty()
                ) {
                    Text("RRF 정렬(대표샷 ${ui.exemplarInstanceIds.size})")
                }
                OutlinedButton(
                    onClick = { vm.clearExemplars() },
                    enabled = ui.exemplarInstanceIds.isNotEmpty()
                ) {
                    Text("대표 초기화")
                }
            }

            if (ui.isBusy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("처리 중…")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.imageId }) { item ->
                ServerGalleryItem(item = item, onClick = { onOpenDetail(item.imageId) })
            }
        }
    }
}

@Composable
private fun ServerGalleryItem(
    item: ServerImageItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbUrl,
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.imageId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("instances=${item.instanceCount}")
            if (!item.capturedAt.isNullOrBlank()) {
                Text("captured_at=${item.capturedAt}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
