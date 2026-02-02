package com.poc.search.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.poc.search.MainViewModel
import com.poc.search.SelectionMode
import com.poc.search.data.db.LocalImageEntity
import com.poc.search.data.db.UploadState
import kotlinx.coroutines.launch

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
    
    // ✅ 스크롤 상태 관리 및 코루틴 스코프
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // ✅ 정렬 데이터(images)가 변경될 때 스크롤을 최상단으로 이동
    // 특히 sortRank가 있는 이미지가 위로 올라왔을 때 유용함
    LaunchedEffect(images) {
        if (images.any { it.sortRank != null }) {
            gridState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("PetID PoC", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { vm.testHealth() }) { Icon(Icons.Default.Refresh, "Health") }
                IconButton(onClick = onOpenSetup) { Icon(Icons.Default.Settings, "Setup") }
            }
        )

        // ✅ 검색 히스토리 리스트 (클릭 시 스크롤 최상단 이동)
        if (ui.searchHistory.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lazyItems(ui.searchHistory) { history ->
                    FilterChip(
                        selected = false,
                        onClick = { 
                            vm.searchByHistory(history)
                            scope.launch { gridState.animateScrollToItem(0) }
                        },
                        label = { Text(history.petId) },
                        leadingIcon = { Text("🐶", fontSize = 12.sp) }
                    )
                }
            }
        }

        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("그리드:", style = MaterialTheme.typography.labelMedium)
                    listOf(4, 6, 8).forEach { cols ->
                        FilterChip(
                            selected = ui.gridColumns == cols,
                            onClick = { vm.setGridColumns(cols) },
                            label = { Text("${cols}x${cols}") }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("선택 모드:", style = MaterialTheme.typography.labelMedium)
                    SelectionMode.values().forEach { mode ->
                        FilterChip(
                            selected = ui.selectionMode == mode,
                            onClick = { vm.setSelectionMode(mode) },
                            label = { Text(if (mode == SelectionMode.MANUAL) "수동" else "가장큰객체") }
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.scanDefaultFolder() }, modifier = Modifier.weight(1f)) {
                        Text("폴더 스캔 & 즉시전송")
                    }
                    OutlinedButton(onClick = { 
                        vm.clearAllData()
                        scope.launch { gridState.scrollToItem(0) }
                    }) {
                        Text("초기화")
                    }
                }
            }
        }

        if (ui.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        // ✅ gridState를 바인딩하여 스크롤 제어
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(ui.gridColumns),
            modifier = Modifier.fillMaxSize().padding(2.dp),
            contentPadding = PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(images, key = { it.localUri }) { item ->
                Box(modifier = Modifier.aspectRatio(1f).clickable { onOpenDetail(item.localUri) }) {
                    AsyncImage(
                        model = item.localUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (item.uploadState == UploadState.UPLOADED) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp).align(Alignment.TopEnd).padding(2.dp)
                        )
                    }
                    
                    item.sortRank?.let { rank ->
                        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f)).padding(2.dp).align(Alignment.BottomStart)) {
                            Text("#${rank + 1}", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
