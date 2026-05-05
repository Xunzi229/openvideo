package com.example.openvideo.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.openvideo.R
import com.example.openvideo.data.model.VideoItem

class VideoGridAdapter(
    private val onClick: (VideoItem) -> Unit,
    private val onMoreOptions: ((VideoItem, View) -> Unit)? = null,
    private val onSelectionChanged: ((List<VideoItem>) -> Unit)? = null,
    private val onLongClick: ((VideoItem) -> Unit)? = null
) : ListAdapter<VideoItem, VideoGridAdapter.ViewHolder>(DIFF) {

    var viewMode: ViewMode = ViewMode.LIST
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    private val selectedItems = mutableSetOf<Long>()
    var isMultiSelectMode = false
        private set

    companion object {
        private const val TYPE_LIST = 0
        private const val TYPE_GRID = 1
        private val DIFF = object : DiffUtil.ItemCallback<VideoItem>() {
            override fun areItemsTheSame(a: VideoItem, b: VideoItem) = a.id == b.id
            override fun areContentsTheSame(a: VideoItem, b: VideoItem) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val title: TextView = view.findViewById(R.id.tv_title)
        val duration: TextView = view.findViewById(R.id.tv_duration)
        val size: TextView? = view.findViewById(R.id.tv_size)
        val resolution: TextView? = view.findViewById(R.id.tv_resolution)
        val moreBtn: View? = view.findViewById(R.id.btn_more)
        val checkBox: CheckBox? = view.findViewById(R.id.cb_select)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (isMultiSelectMode) {
                        toggleSelection(getItem(pos))
                    } else {
                        onClick(getItem(pos))
                    }
                }
            }
            view.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (!isMultiSelectMode) {
                        startMultiSelectMode()
                        toggleSelection(getItem(pos))
                    }
                    onLongClick?.invoke(getItem(pos))
                }
                true
            }
            moreBtn?.setOnClickListener { btn ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMoreOptions?.invoke(getItem(pos), btn)
                }
            }
            checkBox?.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    toggleSelection(getItem(pos))
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (viewMode == ViewMode.GRID) TYPE_GRID else TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_GRID) R.layout.item_video_grid else R.layout.item_video
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.title.text = item.title
        holder.duration.text = formatDuration(item.duration)
        holder.size?.text = formatSize(item.size)
        holder.resolution?.text = "${item.width}x${item.height}"

        // Multi-select UI
        val isSelected = selectedItems.contains(item.id)
        holder.checkBox?.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
        holder.checkBox?.isChecked = isSelected
        holder.itemView.setBackgroundColor(
            if (isSelected) Color.argb(40, 33, 150, 243) else Color.TRANSPARENT
        )

        Glide.with(holder.thumbnail)
            .load(item.thumbnailUri)
            .centerCrop()
            .placeholder(R.drawable.ic_movie)
            .into(holder.thumbnail)
    }

    fun startMultiSelectMode() {
        isMultiSelectMode = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(emptyList())
    }

    fun toggleSelection(item: VideoItem) {
        if (selectedItems.contains(item.id)) {
            selectedItems.remove(item.id)
        } else {
            selectedItems.add(item.id)
        }
        val index = currentList.indexOf(item)
        if (index >= 0) notifyItemChanged(index)
        onSelectionChanged?.invoke(getSelectedItems())
    }

    fun selectAll() {
        selectedItems.clear()
        currentList.forEach { selectedItems.add(it.id) }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(getSelectedItems())
    }

    fun getSelectedItems(): List<VideoItem> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedItems.size

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }
}
