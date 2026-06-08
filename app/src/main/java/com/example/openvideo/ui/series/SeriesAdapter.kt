package com.example.openvideo.ui.series

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.openvideo.R
import java.io.File

class SeriesAdapter(
    private val onClick: (SeriesUiState) -> Unit,
    private val onFocusChanged: (SeriesUiState) -> Unit = {}
) : ListAdapter<SeriesUiState, SeriesAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SeriesUiState>() {
            override fun areItemsTheSame(a: SeriesUiState, b: SeriesUiState): Boolean =
                a.seriesId == b.seriesId

            override fun areContentsTheSame(a: SeriesUiState, b: SeriesUiState): Boolean =
                a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.iv_poster)
        val title: TextView = view.findViewById(R.id.tv_series_title)
        val folder: TextView = view.findViewById(R.id.tv_series_folder)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
            view.setOnFocusChangeListener { _, hasFocus ->
                val pos = bindingAdapterPosition
                if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Glide.with(holder.poster)
            .load(posterModel(item.posterPath))
            .centerCrop()
            .placeholder(R.drawable.ic_movie)
            .fallback(R.drawable.ic_movie)
            .error(R.drawable.ic_movie)
            .into(holder.poster)
        holder.title.text = item.title
        holder.folder.text = item.folderPath
    }

    override fun onViewRecycled(holder: ViewHolder) {
        Glide.with(holder.poster.context.applicationContext).clear(holder.poster)
        super.onViewRecycled(holder)
    }

    private fun posterModel(path: String?): Any? {
        val trimmed = path?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> null
            trimmed.startsWith("content://", ignoreCase = true) -> Uri.parse(trimmed)
            trimmed.startsWith("file://", ignoreCase = true) -> Uri.parse(trimmed)
            else -> File(trimmed)
        }
    }
}
