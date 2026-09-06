package com.openvideo.app.ui.history

import androidx.lifecycle.ViewModel
import com.openvideo.app.data.local.HistoryEntity
import com.openvideo.app.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: VideoRepository
) : ViewModel() {

    val history: Flow<List<HistoryEntity>> = repository.getHistory()
}
