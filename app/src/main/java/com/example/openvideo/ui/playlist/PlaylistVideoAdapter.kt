package com.example.openvideo.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.data.local.PlaylistVideoEntity

class PlaylistVideoAdapter(
    private val onClick: (PlaylistVideoEntity) -> Unit,
    private val onRemove: (PlaylistVideoEntity) -> Unit
) : ListAdapter<PlaylistVideoEntity, PlaylistVideoAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PlaylistVideoEntity>() {
            override fun areItemsTheSame(a: PlaylistVideoEntity, b: PlaylistVideoEntity) =
                a.playlistId == b.playlistId && a.videoId == b.videoId
            override fun areContentsTheSame(a: PlaylistVideoEntity, b: PlaylistVideoEntity) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val duration: TextView = view.findViewById(R.id.tv_duration)
        val removeBtn: ImageButton = view.findViewById(R.id.btn_remove)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
            removeBtn.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemove(getItem(pos))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.videoTitle
        holder.duration.text = formatDuration(item.videoDuration)
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
