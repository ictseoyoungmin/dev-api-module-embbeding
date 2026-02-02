package com.poc.petgalleryxml

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.poc.petgalleryxml.databinding.ActivityMainBinding
import com.poc.petgalleryxml.ui.camera.CameraFragment
import com.poc.petgalleryxml.ui.settings.SettingsFragment
import com.poc.petgalleryxml.ui.work.WorkFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, WorkFragment.newInstance())
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_work -> {
                    binding.toolbar.title = "🐾 반자동 분류"
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, WorkFragment.newInstance())
                    }
                    true
                }
                R.id.nav_camera -> {
                    binding.toolbar.title = "📤 업로드"
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, CameraFragment.newInstance())
                    }
                    true
                }
                R.id.nav_settings -> {
                    binding.toolbar.title = "⚙️ 설정"
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, SettingsFragment.newInstance())
                    }
                    true
                }
                else -> false
            }
        }
    }
}
