package com.poc.petgalleryxml.ui.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.poc.petgalleryxml.data.api.PetApiClient
import com.poc.petgalleryxml.data.api.dto.LabelAssignment
import com.poc.petgalleryxml.data.api.dto.LabelRequest
import com.poc.petgalleryxml.data.api.dto.InstanceOut
import com.poc.petgalleryxml.data.prefs.AppPrefs
import com.poc.petgalleryxml.databinding.ActivityServerDetailBinding
import com.poc.petgalleryxml.domain.MockPets
import com.poc.petgalleryxml.ui.picker.PetPickerBottomSheet
import com.poc.petgalleryxml.ui.work.ExemplarItem
import com.poc.petgalleryxml.ui.work.ExemplarStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerDetailBinding

    private lateinit var prefs: AppPrefs

    private var imageId: String = ""
    private var selectedPetId: String? = null

    private var instances: List<InstanceOut> = emptyList()
    private var selectedInstance: InstanceOut? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPrefs(this)

        imageId = intent.getStringExtra(EXTRA_IMAGE_ID) ?: ""
        if (imageId.isBlank()) {
            finish()
            return
        }

        selectedPetId = intent.getStringExtra(EXTRA_SELECTED_PET_ID) ?: prefs.selectedPetId

        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "상세"
        binding.toolbar.setNavigationOnClickListener { finish() }

        renderSelectedPet()

        binding.btnPickPet.setOnClickListener {
            PetPickerBottomSheet.newInstance(selectedPetId)
                .apply {
                    onPetSelected = { pet ->
                        selectedPetId = pet.id
                        prefs.selectedPetId = pet.id
                        renderSelectedPet()
                    }
                }
                .show(supportFragmentManager, "petPicker")
        }

        binding.btnAddExemplar.setOnClickListener {
            val ins = selectedInstance
            if (ins == null) {
                Snackbar.make(binding.root, "먼저 박스를 선택하세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ExemplarStore.add(
                ExemplarItem(
                    imageId = imageId,
                    instanceId = ins.instanceId,
                    thumbUrl = intent.getStringExtra(EXTRA_THUMB_URL),
                    bbox = ins.bbox
                )
            )
            Snackbar.make(binding.root, "대표샷에 추가됨", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnLabel.setOnClickListener {
            val ins = selectedInstance
            if (ins == null) {
                Snackbar.make(binding.root, "먼저 박스를 선택하세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedPetId.isNullOrBlank()) {
                Snackbar.make(binding.root, "펫을 먼저 선택하세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveLabel(ins.instanceId, selectedPetId!!)
        }

        binding.overlay.onInstanceClick = { ins ->
            selectedInstance = ins
            binding.tvSelected.text = "선택: ${ins.instanceId.takeLast(6)} | ${ins.species} | conf=${String.format("%.2f", ins.confidence)}"
        }

        fetchMetaAndRender()
    }

    private fun renderSelectedPet() {
        val pet = MockPets.find(selectedPetId)
        binding.tvSelectedPet.text = if (pet == null) {
            "현재 선택된 펫: (없음)"
        } else {
            "현재 선택된 펫: ${pet.emoji} ${pet.name} (${pet.id})"
        }
    }

    private fun fetchMetaAndRender() {
        val baseUrl = prefs.baseUrl

        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val api = PetApiClient(baseUrl, contentResolver)
                val meta = withContext(Dispatchers.IO) { api.getImageMeta(imageId) }

                // Load image
                val rawUrl = intent.getStringExtra(EXTRA_RAW_URL) ?: meta.image.rawUrl
                val absRaw = api.absoluteImageUrl(rawUrl)

                binding.ivImage.load(absRaw) {
                    listener(
                        onSuccess = { _, result ->
                            val d = result.drawable
                            binding.overlay.setImageIntrinsicSize(d.intrinsicWidth, d.intrinsicHeight)
                        }
                    )
                }

                instances = meta.instances
                binding.overlay.setInstances(instances)
                binding.overlay.setSelected(null)

                // Default selection: best confidence
                selectedInstance = instances.maxByOrNull { it.confidence }
                selectedInstance?.let {
                    binding.overlay.setSelected(it.instanceId)
                    binding.tvSelected.text = "선택: ${it.instanceId.takeLast(6)} | ${it.species} | conf=${String.format("%.2f", it.confidence)}"
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "불러오기 실패: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun saveLabel(instanceId: String, petId: String) {
        val baseUrl = prefs.baseUrl
        val daycareId = prefs.daycareId
        val trainerId = prefs.trainerId

        lifecycleScope.launch {
            try {
                binding.btnLabel.isEnabled = false
                val api = PetApiClient(baseUrl, contentResolver)
                val req = LabelRequest(
                    daycareId = daycareId,
                    assignments = listOf(LabelAssignment(instanceId = instanceId, petId = petId)),
                    labeledBy = trainerId
                )
                withContext(Dispatchers.IO) { api.setLabels(req) }

                // Update local instance list for overlay label display
                instances = instances.map { if (it.instanceId == instanceId) it.copy(petId = petId) else it }
                binding.overlay.setInstances(instances)
                Snackbar.make(binding.root, "라벨 저장됨: $petId", Snackbar.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Snackbar.make(binding.root, "라벨 저장 실패: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnLabel.isEnabled = true
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_ID = "image_id"
        const val EXTRA_RAW_URL = "raw_url"
        const val EXTRA_THUMB_URL = "thumb_url"
        const val EXTRA_SELECTED_PET_ID = "selected_pet_id"
    }
}
