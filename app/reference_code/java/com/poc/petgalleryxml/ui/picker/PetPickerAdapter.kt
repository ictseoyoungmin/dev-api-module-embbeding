package com.poc.petgalleryxml.ui.picker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.poc.petgalleryxml.databinding.ItemPetBinding
import com.poc.petgalleryxml.domain.Pet

class PetPickerAdapter(
    private val pets: List<Pet>,
    private val selectedId: String?,
    private val onClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetPickerAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = pets.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(pets[position])
    }

    inner class VH(private val binding: ItemPetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: Pet) {
            binding.tvEmoji.text = pet.emoji
            binding.tvName.text = "${pet.name} (${pet.id})"
            binding.tvSelected.visibility = if (pet.id == selectedId) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onClick(pet) }
        }
    }
}
