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
import com.example.openvideo.data.local.PlaylistEntity

class PlaylistAdapter(
    private val onClick: (PlaylistEntity) -> Unit,
    private val onMoreOptions: (PlaylistEntity, View) -> Unit
) : ListAdapter<PlaylistEntity, PlaylistAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PlaylistEntity>() {
            override fun areItemsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a.id == b.id
            override fun areContentsTheSame(a: PlaylistEntity, b: PlaylistEntity) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_playlist_name)
        val count: TextView = view.findViewById(R.id.tv_video_count)
        val moreBtn: ImageButton = view.findViewById(R.id.btn_more)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
            moreBtn.setOnClickListener { btn ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onMoreOptions(getItem(pos), btn)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.name.text = item.name
        holder.count.text = "${item.createdAt}"
    }
}
