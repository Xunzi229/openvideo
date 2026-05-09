package com.example.openvideo.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.putSessionQueue
import com.example.openvideo.ui.player.toSessionVideoItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()

    private var historySnapshot: List<HistoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide category/filter tabs for history
        view.findViewById<View>(R.id.tab_layout)?.visibility = View.GONE
        view.findViewById<View>(R.id.category_scroll)?.visibility = View.GONE
        view.findViewById<View>(R.id.filter_scroll)?.visibility = View.GONE
        view.findViewById<View>(R.id.btn_refresh)?.visibility = View.GONE

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_videos)
        val emptyView = view.findViewById<TextView>(R.id.tv_empty)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Simple history list adapter
        val adapter = HistoryAdapter { entity ->
            val queue = historySnapshot.map { it.toSessionVideoItem() }
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putSessionQueue(queue)
                putExtra("video_uri", entity.path)
                putExtra("video_title", entity.title)
                putExtra("video_id", entity.videoId)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    historySnapshot = list
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }
}

private class HistoryAdapter(
    private val onClick: (HistoryEntity) -> Unit
) : androidx.recyclerview.widget.ListAdapter<HistoryEntity, HistoryAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<HistoryEntity>() {
        override fun areItemsTheSame(a: HistoryEntity, b: HistoryEntity) = a.videoId == b.videoId
        override fun areContentsTheSame(a: HistoryEntity, b: HistoryEntity) = a == b
    }
) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val duration: TextView = view.findViewById(R.id.tv_duration)
        val size: TextView = view.findViewById(R.id.tv_size)
        val resolution: TextView = view.findViewById(R.id.tv_resolution)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.duration.text = ""
        holder.size.text = ""
        holder.resolution.text = ""
    }
}
