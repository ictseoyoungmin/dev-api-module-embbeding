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
fun DetailScreen(
    vm: MainViewModel,
    localUri: String,
    onBack: () -> Unit
) {
    val ui = vm.ui.collectAsState().value
    val images = vm.images.collectAsState().value
    val instances = vm.selectedInstances.collectAsState().value

    val image = images.firstOrNull { it.localUri == localUri }

    LaunchedEffect(localUri) {
        vm.selectLocalUri(localUri)
    }

    // ✅ 정렬용 ID 입력 다이얼로그 상태
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchIdInput by remember { mutableStateOf("") }

    val (showLabelDialog, setShowLabelDialog) = remember { mutableStateOf(false) }
    val (petIdInput, setPetIdInput) = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(image?.serverImageId ?: "Detail") },
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

            Text("Daycare: ${ui.daycareId} | 대표샷: ${ui.exemplarInstanceIds.size}개")

            ImageWithBoxes(
                imageModel = android.net.Uri.parse(localUri),
                imageWidth = image?.width,
                imageHeight = image?.height,
                instances = instances.map {
                    BoxInstance(
                        instanceId = it.instanceId,
                        confidence = it.confidence,
                        x1 = it.x1,
                        y1 = it.y1,
                        x2 = it.x2,
                        y2 = it.y2,
                        species = it.species,
                        petId = it.petId
                    )
                },
                selectedInstanceId = ui.selectedInstanceId,
                exemplarInstanceIds = ui.exemplarInstanceIds.toSet(),
                onSelectInstance = { vm.selectInstance(it) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.ingest(localUri) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upload & Detect")
                }
                
                Button(
                    onClick = { 
                        // ✅ 바로 정렬하지 않고 ID 입력창을 띄움
                        showSearchDialog = true
                    },
                    enabled = ui.selectedInstanceId != null || ui.exemplarInstanceIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("RRF 정렬")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { ui.selectedInstanceId?.let { vm.addExemplar(it) } },
                    enabled = ui.selectedInstanceId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("대표 추가")
                }
                OutlinedButton(
                    onClick = { setShowLabelDialog(true) },
                    enabled = ui.selectedInstanceId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ID 지정")
                }
            }

            if (ui.selectedInstanceId == null && ui.exemplarInstanceIds.isEmpty()) {
                Text(
                    "💡 정렬하려면 사진의 개체(박스)를 먼저 탭하세요.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // ✅ RRF 정렬용 ID 입력 Subbox (다이얼로그)
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
                        vm.searchAndSort(searchIdInput.trim())
                        showSearchDialog = false
                        onBack() // 정렬 시작 후 리스트로 이동
                    }
                }) { Text("정렬 시작") }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("취소") }
            }
        )
    }

    if (showLabelDialog) {
        AlertDialog(
            onDismissRequest = { setShowLabelDialog(false) },
            title = { Text("Pet ID 지정") },
            text = {
                OutlinedTextField(
                    value = petIdInput,
                    onValueChange = setPetIdInput,
                    label = { Text("ID 입력") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (petIdInput.isNotBlank()) vm.labelSelectedInstance(petIdInput.trim())
                    setShowLabelDialog(false)
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { setShowLabelDialog(false) }) { Text("취소") }
            }
        )
    }
}
