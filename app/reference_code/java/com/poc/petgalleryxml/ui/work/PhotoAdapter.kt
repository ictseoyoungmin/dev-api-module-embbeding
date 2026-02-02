package com.poc.petgalleryxml.ui.work

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.poc.petgalleryxml.data.api.dto.GalleryImageItem
import com.poc.petgalleryxml.databinding.ItemPhotoBinding
import com.poc.petgalleryxml.util.TimeFormat

class PhotoAdapter(
    private val onClick: (GalleryImageItem) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    private var items: List<GalleryImageItem> = emptyList()
    private var scores: Map<String, Double> = emptyMap()

    fun submit(newItems: List<GalleryImageItem>, newScores: Map<String, Double>) {
        items = newItems
        scores = newScores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], scores[items[position].imageId])
    }

    inner class VH(private val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GalleryImageItem, score: Double?) {
            // thumb_url는 상대경로일 수 있음. Coil이 절대 URL로만 확실하게 동작하므로 보정.
            val absThumb = absUrl(item.thumbUrl)
            binding.ivThumb.load(absThumb) {
                crossfade(true)
            }

            if (item.instanceCount > 1) {
                binding.badgeCount.text = "${item.instanceCount}마리"
                binding.badgeCount.visibility = android.view.View.VISIBLE
            } else {
                binding.badgeCount.visibility = android.view.View.GONE
            }

            binding.badgeTime.text = TimeFormat.relativeTimeKorean(item.capturedAt ?: item.uploadedAt)

            if (score != null) {
                binding.badgeScore.text = String.format("%.3f", score)
                binding.badgeScore.visibility = android.view.View.VISIBLE
            } else {
                binding.badgeScore.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(item) }
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

/**
 * Adapter에서 baseUrl을 참조하기 위한 최소 helper.
 * - Settings에서 baseUrl 변경 시, 새로고침하면 반영됩니다.
 */
object AppConfig {
    fun baseUrl(context: android.content.Context): String {
        return com.poc.petgalleryxml.data.prefs.AppPrefs(context).baseUrl
    }
}
