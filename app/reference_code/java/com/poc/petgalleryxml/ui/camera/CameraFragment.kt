package com.poc.petgalleryxml.ui.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.poc.petgalleryxml.data.api.PetApiClient
import com.poc.petgalleryxml.data.prefs.AppPrefs
import com.poc.petgalleryxml.databinding.FragmentCameraBinding
import com.poc.petgalleryxml.ui.detail.ServerDetailActivity
import com.poc.petgalleryxml.util.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: AppPrefs

    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult
        uploadUris(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickAndUpload.setOnClickListener {
            picker.launch(arrayOf("image/*"))
        }
    }

    private fun uploadUris(uris: List<Uri>) {
        val baseUrl = prefs.baseUrl
        val daycareId = prefs.daycareId
        val trainerId = prefs.trainerId

        binding.progress.visibility = View.VISIBLE
        binding.progress.max = uris.size
        binding.progress.progress = 0
        binding.tvLog.text = "업로드 시작: ${uris.size}장\n"

        lifecycleScope.launch {
            val api = PetApiClient(baseUrl, requireContext().contentResolver)

            var lastImageId: String? = null
            for ((idx, uri) in uris.withIndex()) {
                try {
                    appendLog("[${idx + 1}/${uris.size}] 업로드 중...")
                    val resp = withContext(Dispatchers.IO) {
                        api.ingest(
                            uri = uri,
                            daycareId = daycareId,
                            trainerId = trainerId,
                            capturedAtIso = TimeFormat.isoNowUtc(),
                            includeEmbedding = false
                        )
                    }
                    lastImageId = resp.image.imageId
                    appendLog("  - OK image_id=${resp.image.imageId} instances=${resp.instances.size}")
                } catch (e: Exception) {
                    appendLog("  - ERROR: ${e.message}")
                }
                binding.progress.progress = idx + 1
            }

            binding.progress.visibility = View.GONE

            if (lastImageId != null) {
                Snackbar.make(binding.root, "업로드 완료. 마지막 이미지 상세로 이동합니다.", Snackbar.LENGTH_SHORT).show()
                // 상세로 이동 (서버에 저장된 이미지이므로 ServerDetailActivity 사용)
                val intent = Intent(requireContext(), ServerDetailActivity::class.java).apply {
                    putExtra(ServerDetailActivity.EXTRA_IMAGE_ID, lastImageId)
                }
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, "업로드 실패", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun appendLog(line: String) {
        binding.tvLog.text = binding.tvLog.text.toString() + line + "\n"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CameraFragment()
    }
}
