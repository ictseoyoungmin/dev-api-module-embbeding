package com.poc.search.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poc.search.MainViewModel
import com.poc.search.ui.components.ImageWithBoxes
import com.poc.search.ui.model.BoxInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    vm: MainViewModel,
    imageId: String,
    onBack: () -> Unit
) {
    val ui = vm.ui.collectAsState().value
    val meta = vm.serverSelectedMeta.collectAsState().value

    LaunchedEffect(imageId) {
        vm.selectServerImage(imageId)
    }

    // ✅ 정렬용 ID 입력 다이얼로그 상태
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchIdInput by remember { mutableStateOf("") }

    val instances = meta?.instances?.map { inst ->
        BoxInstance(
            instanceId = inst.instanceId,
            confidence = inst.confidence,
            x1 = inst.bbox.x1.toDouble(),
            y1 = inst.bbox.y1.toDouble(),
            x2 = inst.bbox.x2.toDouble(),
            y2 = inst.bbox.y2.toDouble(),
            species = inst.species,
            petId = inst.petId
        )
    } ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(imageId) },
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
            if (ui.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("daycare_id: ${ui.daycareId} | 대표샷: ${ui.exemplarInstanceIds.size}개")

            val rawUrl = meta?.image?.rawUrl
            if (!rawUrl.isNullOrBlank()) {
                ImageWithBoxes(
                    imageModel = rawUrl,
                    imageWidth = meta?.image?.width,
                    imageHeight = meta?.image?.height,
                    instances = instances,
                    selectedInstanceId = ui.selectedInstanceId,
                    exemplarInstanceIds = ui.exemplarInstanceIds.toSet(),
                    onSelectInstance = { vm.selectInstance(it) }
                )
            } else {
                Text("이미지 메타 로딩 중…")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { ui.selectedInstanceId?.let { vm.addExemplar(it) } },
                    enabled = ui.selectedInstanceId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("대표 추가")
                }
                Button(
                    onClick = { showSearchDialog = true },
                    enabled = ui.exemplarInstanceIds.isNotEmpty() || ui.selectedInstanceId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("RRF 정렬(서버)")
                }
            }
        }
    }

    // ✅ RRF 정렬용 ID 입력 다이얼로그 (Subbox)
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("정렬할 이름을 입력하세요") },
            text = {
                OutlinedTextField(
                    value = searchIdInput,
                    onValueChange = { searchIdInput = it },
                    label = { Text("예: 뽀미, 윌터") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (searchIdInput.isNotBlank()) {
                        vm.searchAndSortServer(searchIdInput.trim())
                        showSearchDialog = false
                        onBack()
                    }
                }) { Text("정렬 시작") }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("취소") }
            }
        )
    }
}
