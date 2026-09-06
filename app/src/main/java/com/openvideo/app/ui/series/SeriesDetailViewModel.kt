package com.openvideo.app.ui.series

import androidx.lifecycle.ViewModel
import com.openvideo.app.data.repository.VideoRepository
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
