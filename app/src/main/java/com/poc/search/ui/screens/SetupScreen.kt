package com.poc.search.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poc.search.MainViewModel

@Composable
fun SetupScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val ui = vm.ui.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Server 설정")

        OutlinedTextField(
            value = ui.baseUrl,
            onValueChange = vm::setBaseUrl,
            label = { Text("Base URL (예: http://10.0.2.2:8000)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = ui.daycareId,
            onValueChange = vm::setDaycareId,
            label = { Text("daycare_id") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = ui.trainerId,
            onValueChange = vm::setTrainerId,
            label = { Text("trainer_id (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("업로드 시 JPEG 옵션: maxSide=${ui.jpegMaxSidePx}px, quality=${ui.jpegQuality}")

        if (ui.isBusy) {
            CircularProgressIndicator()
        }

        Button(onClick = { vm.testHealth() }, modifier = Modifier.fillMaxWidth()) {
            Text("Health check")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
