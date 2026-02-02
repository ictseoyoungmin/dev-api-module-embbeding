package com.poc.petgalleryxml.ui.work

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.poc.petgalleryxml.databinding.ItemExemplarBinding

class ExemplarAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<ExemplarAdapter.VH>() {

    private var items: List<ExemplarItem> = emptyList()

    fun submit(newItems: List<ExemplarItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExemplarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(private val binding: ItemExemplarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExemplarItem) {
            val url = item.thumbUrl
            if (!url.isNullOrBlank()) {
                binding.iv.load(absUrl(url))
            } else {
                // fallback
                binding.iv.setImageDrawable(null)
            }

            binding.btnRemove.setOnClickListener { onRemove(item.instanceId) }
        }

        private fun absUrl(relativeOrAbs: String): String {
            val s = relativeOrAbs.trim()
            return if (s.startsWith("http://") || s.startsWith("https://")) s
            else {
                val base = AppConfig.baseUrl(binding.root.context)
                base.trimEnd('/') + s
            }
        }
    }
}
