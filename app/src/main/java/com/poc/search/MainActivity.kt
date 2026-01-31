package com.poc.search

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import com.poc.search.ui.processing.ProcessingScreen
import com.poc.search.ui.results.ResultsScreen
import com.poc.search.ui.setup.SetupScreen

class MainActivity : ComponentActivity() {

    private val vm: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAllFilesAccess()

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val ui = vm.state.collectAsState().value

                when {
                    ui.grouped != null -> ResultsScreen(
                        uiState = ui,
                        onBackToSetup = { vm.reset() },
                        onExport = { vm.export() },
                        onReassign = { photoUri, newDogIdOrNull -> vm.reassign(photoUri, newDogIdOrNull) },
                        onClearMessage = { vm.clearMessage() }
                    )

                    ui.progress != null -> ProcessingScreen(
                        uiState = ui,
                        onCancel = { vm.cancel() },
                        onClearMessage = { vm.clearMessage() }
                    )

                    else -> SetupScreen(
                        uiState = ui,
                        onSetBaseUrl = vm::setBaseUrl,
                        onSetTopK = vm::setTopK,
                        onSetThreshold = vm::setThreshold,
                        onSetBatchSize = vm::setBatchSize,
                        onSetFormat = vm::setFormat,
                        onPickIncoming = { uri -> vm.setIncomingRoot(uri) },
                        onPickReference = { uri -> vm.setReferenceRoot(uri) },
                        onPickOutput = { uri -> vm.setOutputRoot(uri) },
                        onStart = { vm.start() },
                        onTestHealth = { vm.testHealth() },
                        onClearMessage = { vm.clearMessage() }
                    )
                }
            }
        }
    }

    private fun checkAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            }
        }
    }
}
