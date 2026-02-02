package com.poc.petgalleryxml.ui.work

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.poc.petgalleryxml.data.prefs.AppPrefs
import com.poc.petgalleryxml.databinding.FragmentWorkBinding
import com.poc.petgalleryxml.domain.MockPets
import com.poc.petgalleryxml.ui.detail.ServerDetailActivity
import com.poc.petgalleryxml.ui.picker.PetPickerBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkFragment : Fragment() {

    private var _binding: FragmentWorkBinding? = null
    private val binding get() = _binding!!

    private val vm: WorkViewModel by viewModels()

    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var exemplarAdapter: ExemplarAdapter

    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Default mode = SERVER
        binding.modeToggle.check(binding.btnServer.id)
        vm.setMode(WorkViewModel.Mode.SERVER)

        binding.btnSelectPet.setOnClickListener {
            PetPickerBottomSheet.newInstance(prefs.selectedPetId)
                .apply {
                    onPetSelected = { pet ->
                        prefs.selectedPetId = pet.id
                        renderSelectedPet()
                    }
                }
                .show(parentFragmentManager, "petPicker")
        }

        renderSelectedPet()

        binding.btnLocal.setOnClickListener { vm.setMode(WorkViewModel.Mode.LOCAL) }
        binding.btnServer.setOnClickListener { vm.setMode(WorkViewModel.Mode.SERVER) }

        // Grid gallery
        photoAdapter = PhotoAdapter(
            onClick = { item ->
                val intent = Intent(requireContext(), ServerDetailActivity::class.java).apply {
                    putExtra(ServerDetailActivity.EXTRA_IMAGE_ID, item.imageId)
                    putExtra(ServerDetailActivity.EXTRA_RAW_URL, item.rawUrl)
                    putExtra(ServerDetailActivity.EXTRA_THUMB_URL, item.thumbUrl)
                    putExtra(ServerDetailActivity.EXTRA_SELECTED_PET_ID, prefs.selectedPetId)
                }
                startActivity(intent)
            }
        )
        binding.rv.adapter = photoAdapter
        binding.rv.setHasFixedSize(true)
        binding.rv.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)

        // Swipe refresh
        binding.swipe.setOnRefreshListener { vm.refreshServerImages() }

        // Exemplars tray
        exemplarAdapter = ExemplarAdapter(
            onRemove = { instanceId -> ExemplarStore.remove(instanceId) }
        )
        binding.rvExemplars.adapter = exemplarAdapter
        binding.rvExemplars.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)

        binding.btnClearExemplars.setOnClickListener { ExemplarStore.clear() }

        binding.btnRrf.setOnClickListener {
            vm.rrfSearchWithExemplars(ExemplarStore.items.value)
        }

        // Observe VM
        vm.images.observe(viewLifecycleOwner) { items ->
            photoAdapter.submit(items, vm.scores.value.orEmpty())
        }
        vm.scores.observe(viewLifecycleOwner) { scores ->
            photoAdapter.submit(vm.images.value.orEmpty(), scores)
        }
        vm.sortHint.observe(viewLifecycleOwner) { hint ->
            binding.tvModeHint.text = hint
        }
        vm.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipe.isRefreshing = loading
        }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
            }
        }

        // ExemplarStore collector
        viewLifecycleOwner.lifecycleScope.launch {
            ExemplarStore.items.collectLatest { items ->
                binding.tvExemplarCount.text = "대표샷 ${items.size}개"
                exemplarAdapter.submit(items)

                // 대표샷이 하나도 없으면 tray를 살짝 줄여도 되지만, PoC에서는 항상 노출
            }
        }
    }

    private fun renderSelectedPet() {
        val pet = MockPets.find(prefs.selectedPetId)
        binding.tvPet.text = if (pet == null) {
            "선택된 펫: (없음)"
        } else {
            "선택된 펫: ${pet.emoji} ${pet.name} (${pet.id})"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = WorkFragment()
    }
}
