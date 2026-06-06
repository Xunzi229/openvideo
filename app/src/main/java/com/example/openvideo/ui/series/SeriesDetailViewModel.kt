package com.example.openvideo.ui.series

import androidx.lifecycle.ViewModel
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    fun getEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodeUiState>> =
        repository.getPlayableEpisodesForSeries(seriesId)
            .map { episodes -> episodes.map(SeriesEpisodeUiState::from) }
}
