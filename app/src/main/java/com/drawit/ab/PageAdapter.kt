package com.drawit.ab

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drawit.ab.databinding.ItemStoryboardBinding
import com.drawit.ab.models.Page

class PageAdapter(
    private val pages: List<Page>,
    private val onItemClicked: (Int) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    inner class PageViewHolder(private val binding: ItemStoryboardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.storyboardView.setPage(pages[position])
            binding.storyboardView.invalidate()
            binding.root.setOnClickListener {
                onItemClicked(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemStoryboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return pages.size
    }
}
