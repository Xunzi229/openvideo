package com.example.openvideo.ui.series

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.openvideo.R
import com.example.openvideo.core.ui.ScreenBreakpoint
import com.example.openvideo.ui.BrowseAdaptiveLayoutPolicy
import com.example.openvideo.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SeriesListFragment : Fragment() {

    private val viewModel: SeriesListViewModel by viewModels()
    private lateinit var adapter: SeriesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var lastFocusedSeriesId: Long? = null
    private var pendingSeriesFocusRestoreId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_series_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tv_title).setText(R.string.series_list_title)
        recyclerView = view.findViewById(R.id.recycler_series)
        emptyView = view.findViewById(R.id.tv_empty)
        emptyView.isFocusable = true
        adapter = SeriesAdapter(
            onClick = { series ->
                lastFocusedSeriesId = series.seriesId
                pendingSeriesFocusRestoreId = lastFocusedSeriesId
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,
                        SeriesDetailFragment.newInstance(series.seriesId, series.title)
                    )
                    .addToBackStack("series:${series.seriesId}")
                    .commit()
            },
            onFocusChanged = { series -> lastFocusedSeriesId = series.seriesId }
        )

        recyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            BrowseAdaptiveLayoutPolicy.contentSpanCount(currentBreakpoint())
        )
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.series.collect { series ->
                    adapter.submitList(series) { restoreSeriesFocusIfNeeded(series) }
                    emptyView.visibility = if (series.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (series.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun restoreSeriesFocusIfNeeded(series: List<SeriesUiState>) {
        val seriesId = pendingSeriesFocusRestoreId ?: return
        val position = series.indexOfFirst { it.seriesId == seriesId }
        if (position == -1) return
        pendingSeriesFocusRestoreId = null
        recyclerView.post {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
    }

    private fun currentBreakpoint(): ScreenBreakpoint =
        (activity as? MainActivity)?.breakpoint ?: ScreenBreakpoint.COMPACT
}
