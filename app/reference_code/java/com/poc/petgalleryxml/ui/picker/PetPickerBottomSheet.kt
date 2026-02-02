package com.poc.petgalleryxml.ui.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.poc.petgalleryxml.databinding.BottomSheetPetPickerBinding
import com.poc.petgalleryxml.domain.MockPets
import com.poc.petgalleryxml.domain.Pet

class PetPickerBottomSheet : BottomSheetDialogFragment() {

    var onPetSelected: ((Pet) -> Unit)? = null

    private var _binding: BottomSheetPetPickerBinding? = null
    private val binding get() = _binding!!

    private var selectedId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedId = arguments?.getString(ARG_SELECTED)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPetPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PetPickerAdapter(
            pets = MockPets.items,
            selectedId = selectedId,
            onClick = { pet ->
                onPetSelected?.invoke(pet)
                dismiss()
            }
        )
        binding.rvPets.adapter = adapter
        binding.rvPets.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SELECTED = "selected_pet"

        fun newInstance(selectedPetId: String?): PetPickerBottomSheet {
            val f = PetPickerBottomSheet()
            f.arguments = Bundle().apply {
                putString(ARG_SELECTED, selectedPetId)
            }
            return f
        }
    }
}
