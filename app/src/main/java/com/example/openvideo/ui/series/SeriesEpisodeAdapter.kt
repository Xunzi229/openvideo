package com.example.openvideo.ui.series

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R

class SeriesEpisodeAdapter(
    private val onClick: (SeriesEpisodeUiState) -> Unit
) : ListAdapter<SeriesEpisodeUiState, SeriesEpisodeAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SeriesEpisodeUiState>() {
            override fun areItemsTheSame(a: SeriesEpisodeUiState, b: SeriesEpisodeUiState): Boolean =
                a.episodeId == b.episodeId

            override fun areContentsTheSame(a: SeriesEpisodeUiState, b: SeriesEpisodeUiState): Boolean =
                a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.tv_episode_number)
        val title: TextView = view.findViewById(R.id.tv_episode_title)
        val meta: TextView = view.findViewById(R.id.tv_episode_meta)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (getItem(pos).isAvailable) onClick(getItem(pos))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series_episode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.number.text = item.numberLabel
        holder.title.text = item.displayTitle
        holder.itemView.isEnabled = item.isAvailable
        holder.itemView.alpha = if (item.isAvailable) 1f else 0.6f
        val watchStatusLabel = SeriesEpisodeWatchStatusPolicy.label(
            status = item.watchStatus,
            labels = SeriesEpisodeWatchStatusLabels.from(holder.itemView.context),
            isAvailable = item.isAvailable
        )
        holder.meta.text = holder.itemView.context.getString(
            R.string.series_episode_meta,
            watchStatusLabel,
            item.confidence,
            item.rule
        )
    }
}
