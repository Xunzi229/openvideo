package com.example.openvideo.ui.history

import androidx.lifecycle.ViewModel
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: VideoRepository
) : ViewModel() {

    val history: Flow<List<HistoryEntity>> = repository.getHistory()
}
