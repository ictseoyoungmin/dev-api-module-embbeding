package com.poc.search.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("처리 중…")
                }
            }

            Text("daycare_id: ${ui.daycareId}")
            Text("instances: ${instances.size}  selected: ${ui.selectedInstanceId ?: "-"}")
            Text("대표샷(exemplar): ${ui.exemplarInstanceIds.size}")

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
                    onClick = { vm.addExemplar(ui.selectedInstanceId!!) },
                    enabled = ui.selectedInstanceId != null
                ) {
                    Text("대표 추가")
                }
                OutlinedButton(
                    onClick = { vm.searchAndSortServer() },
                    enabled = ui.exemplarInstanceIds.isNotEmpty() || ui.selectedInstanceId != null
                ) {
                    Text("RRF 정렬(서버)")
                }
            }
        }
    }
}
