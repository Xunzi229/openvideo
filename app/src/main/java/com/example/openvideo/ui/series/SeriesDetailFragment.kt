package com.example.openvideo.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.putSessionQueue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SeriesDetailFragment : Fragment() {

    private val viewModel: SeriesDetailViewModel by viewModels()
    private var seriesId: Long = 0L
    private var seriesTitle: String = ""
    private lateinit var adapter: SeriesEpisodeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var episodeSnapshot: List<SeriesEpisodeUiState> = emptyList()

    companion object {
        fun newInstance(seriesId: Long, seriesTitle: String): SeriesDetailFragment {
            return SeriesDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("series_id", seriesId)
                    putString("series_title", seriesTitle)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seriesId = arguments?.getLong("series_id") ?: 0L
        seriesTitle = arguments?.getString("series_title") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_series_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tv_title).text = seriesTitle
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recycler_episodes)
        emptyView = view.findViewById(R.id.tv_empty)
        adapter = SeriesEpisodeAdapter { episode ->
            openEpisode(episode)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getEpisodesForSeries(seriesId).collect { episodes ->
                    episodeSnapshot = episodes
                    adapter.submitList(episodes)
                    emptyView.visibility = if (episodes.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (episodes.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun openEpisode(episode: SeriesEpisodeUiState) {
        val selectedVideo = episode.toVideoItem()
        val queue = episodeSnapshot.filter { it.isAvailable }.map { it.toVideoItem() }
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putSessionQueue(queue.ifEmpty { listOf(selectedVideo) })
            putExtra("video_uri", selectedVideo.uri.toString())
            putExtra("video_title", selectedVideo.title)
            putExtra("video_id", selectedVideo.id)
            putExtra("video_path", selectedVideo.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, selectedVideo.width)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, selectedVideo.height)
        }
        startActivity(intent)
    }
}
