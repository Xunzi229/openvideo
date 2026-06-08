package com.example.openvideo.ui.local

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R

class VideoFolderAdapter(
    private val onClick: (VideoFolder) -> Unit,
    private val onLongClick: (VideoFolder) -> Unit,
    private val onFocusChanged: (VideoFolder) -> Unit = {}
) : ListAdapter<VideoFolder, VideoFolderAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VideoFolder>() {
            override fun areItemsTheSame(oldItem: VideoFolder, newItem: VideoFolder): Boolean =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: VideoFolder, newItem: VideoFolder): Boolean =
                oldItem == newItem
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.tv_folder_name)
        private val count: TextView = view.findViewById(R.id.tv_folder_count)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
            }
            view.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
            view.setOnFocusChangeListener { _, hasFocus ->
                val position = bindingAdapterPosition
                if (hasFocus && position != RecyclerView.NO_POSITION) onFocusChanged(getItem(position))
            }
        }

        fun bind(folder: VideoFolder) {
            val displayName = if (folder.isPinned) {
                itemView.resources.getString(R.string.home_filter_folder_pinned_prefix, folder.name)
            } else {
                folder.name
            }
            name.text = displayName
            count.text = itemView.resources.getQuantityString(
                R.plurals.video_count,
                folder.videoCount,
                folder.videoCount
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
