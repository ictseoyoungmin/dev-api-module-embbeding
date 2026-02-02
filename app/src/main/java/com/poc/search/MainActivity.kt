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
import com.poc.search.ui.PetGalleryApp

class MainActivity : ComponentActivity() {

    // 패치된 새로운 MainViewModel을 사용합니다.
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 모든 파일 접근 권한 확인
        checkAllFilesAccess()

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                // 패치된 통합 앱 UI를 실행합니다.
                PetGalleryApp(vm = vm)
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
