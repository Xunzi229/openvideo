package com.example.openvideo.ui.playlist

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.openvideo.R
import com.example.openvideo.data.local.PlaylistVideoEntity
import java.io.File

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
        val thumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val thumbnailLoading: ProgressBar = view.findViewById(R.id.thumbnail_loading)
        val durationBadge: TextView = view.findViewById(R.id.tv_duration)
        val title: TextView = view.findViewById(R.id.tv_title)
        val rowMeta: View = view.findViewById(R.id.row_meta)
        val resolution: TextView = view.findViewById(R.id.tv_resolution)
        val metaSpacer: TextView = view.findViewById(R.id.tv_meta_spacer)
        val size: TextView = view.findViewById(R.id.tv_size)
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
        PlaylistMarqueeTextPolicy.apply(holder.title, item.videoTitle)
        val durationText = formatDuration(item.videoDuration)
        holder.durationBadge.text = durationText

        val path = item.videoPath.trim()
        when {
            path.startsWith("content://", ignoreCase = true) -> {
                holder.rowMeta.visibility = View.VISIBLE
                holder.metaSpacer.visibility = View.VISIBLE
                val sourceLabel =
                    holder.itemView.context.getString(R.string.playlist_item_source_media_library)
                PlaylistMarqueeTextPolicy.apply(holder.resolution, sourceLabel)
                PlaylistMarqueeTextPolicy.apply(holder.size, durationText)
            }
            else -> {
                val fileLabel = secondaryLabelFromPath(path)
                if (fileLabel.isEmpty()) {
                    holder.rowMeta.visibility = View.GONE
                    holder.resolution.isSelected = false
                    holder.size.isSelected = false
                } else {
                    holder.rowMeta.visibility = View.VISIBLE
                    holder.metaSpacer.visibility = View.VISIBLE
                    PlaylistMarqueeTextPolicy.apply(holder.resolution, fileLabel)
                    PlaylistMarqueeTextPolicy.apply(holder.size, durationText)
                }
            }
        }

        holder.thumbnailLoading.visibility = View.VISIBLE
        Glide.with(holder.thumbnail)
            .load(thumbnailModel(path))
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
                    holder.thumbnailLoading.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.thumbnailLoading.visibility = View.GONE
                    return false
                }
            })
            .into(holder.thumbnail)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.title.isSelected = false
        holder.resolution.isSelected = false
        holder.size.isSelected = false
        holder.thumbnailLoading.visibility = View.GONE
        Glide.with(holder.thumbnail.context.applicationContext).clear(holder.thumbnail)
        super.onViewRecycled(holder)
    }

    private fun thumbnailModel(path: String): Any =
        when {
            path.startsWith("content://", ignoreCase = true) -> Uri.parse(path)
            path.startsWith("file://", ignoreCase = true) -> Uri.parse(path)
            else -> File(path)
        }

    private fun secondaryLabelFromPath(path: String): String {
        if (path.isEmpty()) return ""
        if (path.startsWith("content://", ignoreCase = true)) return ""
        if (path.startsWith("file://", ignoreCase = true)) {
            val p = runCatching { Uri.parse(path).path }.getOrNull() ?: return ""
            return File(p).name
        }
        return File(path).name
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
