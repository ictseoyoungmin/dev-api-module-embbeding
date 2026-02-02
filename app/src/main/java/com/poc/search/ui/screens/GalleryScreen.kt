package com.poc.search.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.poc.search.MainViewModel
import com.poc.search.data.db.LocalImageEntity
import com.poc.search.data.db.UploadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    vm: MainViewModel,
    onOpenSetup: () -> Unit,
    onOpenDetail: (localUri: String) -> Unit,
    onOpenServerGallery: () -> Unit
) {
    val ui = vm.ui.collectAsState().value
    val images = vm.images.collectAsState().value

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.addPickedUris(uris)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Gallery (${ui.daycareId})") },
            actions = {
                // ✅ Health Test 버튼 추가
                IconButton(onClick = { vm.testHealth() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Health Test")
                }
                IconButton(onClick = onOpenSetup) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // ✅ 권한 문제를 피하기 위한 직접 스캔 버튼
                Button(
                    onClick = { vm.scanDefaultFolder() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("폴더 자동 스캔")
                }
                
                OutlinedButton(
                    onClick = { pickLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("사진 개별 선택")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onOpenServerGallery() }, modifier = Modifier.weight(1f)) {
                    Text("서버 갤러리")
                }
                OutlinedButton(
                    onClick = { vm.clearExemplars() },
                    enabled = ui.exemplarInstanceIds.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("대표 초기화(${ui.exemplarInstanceIds.size})")
                }
            }

            if (ui.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            ui.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(images, key = { it.localUri }) { item ->
                GalleryItem(
                    item = item,
                    onClick = { onOpenDetail(item.localUri) }
                )
            }
        }
    }
}

@Composable
private fun GalleryItem(
    item: LocalImageEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.localUri,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.localUri.substringAfterLast("/"),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "상태: ${item.uploadState} | ID: ${item.serverImageId ?: "-"}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (item.uploadState == UploadState.UPLOADED) {
            Text("✅", color = Color.Green)
        }
    }
}
