package com.example.openvideo.ui.favorite

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
import com.example.openvideo.data.local.FavoriteEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private val viewModel: FavoriteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.tab_layout)?.visibility = View.GONE
        view.findViewById<View>(R.id.category_scroll)?.visibility = View.GONE
        view.findViewById<View>(R.id.filter_scroll)?.visibility = View.GONE
        view.findViewById<View>(R.id.btn_refresh)?.visibility = View.GONE

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_videos)
        val emptyView = view.findViewById<TextView>(R.id.tv_empty)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = FavoriteAdapter()
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collect { list ->
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }
}

private class FavoriteAdapter :
    androidx.recyclerview.widget.ListAdapter<FavoriteEntity, FavoriteAdapter.VH>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<FavoriteEntity>() {
            override fun areItemsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a.videoId == b.videoId
            override fun areContentsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a == b
        }
    ) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val duration: TextView = view.findViewById(R.id.tv_duration)
        val size: TextView = view.findViewById(R.id.tv_size)
        val resolution: TextView = view.findViewById(R.id.tv_resolution)
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
