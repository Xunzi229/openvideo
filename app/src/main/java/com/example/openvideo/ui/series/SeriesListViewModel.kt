package com.example.openvideo.ui.series

import androidx.lifecycle.ViewModel
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SeriesListViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    val series: Flow<List<SeriesUiState>> = repository.getAllSeries()
        .map { items -> items.map(SeriesUiState::from) }
}
