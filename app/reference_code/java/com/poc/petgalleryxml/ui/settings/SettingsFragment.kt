package com.poc.petgalleryxml.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.poc.petgalleryxml.data.api.PetApiClient
import com.poc.petgalleryxml.data.prefs.AppPrefs
import com.poc.petgalleryxml.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etBaseUrl.setText(prefs.baseUrl)
        binding.etDaycareId.setText(prefs.daycareId)
        binding.etTrainerId.setText(prefs.trainerId)

        binding.btnSave.setOnClickListener {
            prefs.baseUrl = binding.etBaseUrl.text?.toString().orEmpty().trim()
            prefs.daycareId = binding.etDaycareId.text?.toString().orEmpty().trim()
            prefs.trainerId = binding.etTrainerId.text?.toString().orEmpty().trim()
            Snackbar.make(binding.root, "저장되었습니다", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnHealth.setOnClickListener {
            val baseUrl = binding.etBaseUrl.text?.toString().orEmpty().trim()
            lifecycleScope.launch {
                try {
                    binding.tvResult.text = "요청 중..."
                    val api = PetApiClient(baseUrl, requireContext().contentResolver)
                    val resp = api.health()
                    binding.tvResult.text = "OK: $resp"
                } catch (e: Exception) {
                    binding.tvResult.text = "ERROR: ${e.message}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
