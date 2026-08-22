package com.example.openvideo.ui.home

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.openvideo.R
import com.example.openvideo.core.ui.GroupedListChrome
import com.example.openvideo.data.model.VideoItem

class VideoGridAdapter(
    private val onClick: (VideoItem) -> Unit,
    private val onMoreOptions: ((VideoItem, View) -> Unit)? = null,
    private val onSelectionChanged: ((List<VideoItem>) -> Unit)? = null,
    private val onLongClick: ((VideoItem) -> Unit)? = null,
    private val onFocusChanged: (VideoItem) -> Unit = {}
) : ListAdapter<VideoItem, VideoGridAdapter.ViewHolder>(DIFF) {

    var viewMode: ViewMode = ViewMode.LIST
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var continueWatchingBadges: Map<Long, ContinueWatchingBadge> = emptyMap()
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
        val thumbnailLoading: ProgressBar? = view.findViewById(R.id.thumbnail_loading)
        val moreBtn: View? = view.findViewById(R.id.btn_more)
        val checkBox: ImageView? = view.findViewById(R.id.cb_select)
        val hairline: View? = view.findViewById(R.id.row_hairline)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (isMultiSelectMode) {
                        toggleSelection(getItem(pos))
                    } else {
                        val item = getItem(pos)
                        val continueWatchingBadge = continueWatchingBadges[item.id]
                        if (continueWatchingBadge?.isAvailable != false) {
                            onClick(item)
                        }
                    }
                }
            }
            view.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (onLongClick != null && !isMultiSelectMode) {
                        startMultiSelectMode()
                        toggleSelection(getItem(pos))
                    }
                    onLongClick?.invoke(getItem(pos))
                    onLongClick != null
                } else {
                    false
                }
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
            view.setOnFocusChangeListener { _, hasFocus ->
                val pos = bindingAdapterPosition
                if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (viewMode == ViewMode.GRID) TYPE_GRID else TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_GRID) R.layout.item_video_grid else R.layout.item_video
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        (view.findViewById<View>(R.id.iv_thumbnail).parent as View).clipToOutline = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val continueWatchingBadge = continueWatchingBadges[item.id]

        holder.title.text = item.title
        holder.duration.text = if (viewMode == ViewMode.GRID) {
            continueWatchingBadge?.progressLabel ?: formatDuration(item.duration)
        } else {
            formatDuration(item.duration)
        }
        holder.size?.text = continueWatchingBadge?.progressLabel ?: formatSize(item.size)
        holder.resolution?.text = continueWatchingBadge?.watchedTimeLabel ?: "${item.width}x${item.height}"

        val isSelected = selectedItems.contains(item.id)
        bindSelectMark(holder.checkBox, isSelected)
        holder.moreBtn?.visibility = if (isMultiSelectMode) View.GONE else View.VISIBLE
        holder.itemView.setBackgroundColor(
            if (isSelected) {
                ContextCompat.getColor(holder.itemView.context, R.color.ov_accent_blue_soft)
            } else {
                Color.TRANSPARENT
            }
        )
        holder.itemView.alpha = if (continueWatchingBadge?.isAvailable == false) 0.6f else 1f
        if (viewMode == ViewMode.LIST) {
            GroupedListChrome.bindContained(holder.hairline, position, itemCount)
        }

        holder.onLoadStarted()
        Glide.with(holder.thumbnail)
            .load(item.thumbnailUri)
            .centerCrop()
            .placeholder(R.drawable.ic_movie)
            .fallback(R.drawable.ic_movie)
            .error(R.drawable.ic_movie)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.onLoadCleared()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.onLoadCleared()
                    return false
                }
            })
            .into(holder.thumbnail)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.onLoadCleared()
        Glide.with(holder.thumbnail.context.applicationContext).clear(holder.thumbnail)
        super.onViewRecycled(holder)
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

    private fun bindSelectMark(mark: ImageView?, selected: Boolean) {
        if (mark == null) return
        if (!isMultiSelectMode) {
            mark.visibility = View.GONE
            return
        }
        mark.visibility = View.VISIBLE
        if (selected) {
            mark.setBackgroundResource(R.drawable.bg_select_circle_on)
            mark.setImageResource(R.drawable.ic_check)
            mark.imageTintList = ContextCompat.getColorStateList(mark.context, android.R.color.white)
        } else {
            mark.setBackgroundResource(R.drawable.bg_select_circle_off)
            mark.setImageDrawable(null)
        }
    }

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

    private fun ViewHolder.onLoadStarted() {
        thumbnailLoading?.visibility = View.VISIBLE
    }

    private fun ViewHolder.onLoadCleared() {
        thumbnailLoading?.visibility = View.GONE
    }
}
