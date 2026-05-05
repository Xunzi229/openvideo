package com.example.openvideo.ui.favorite

import androidx.lifecycle.ViewModel
import com.example.openvideo.data.local.FavoriteEntity
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    repository: VideoRepository
) : ViewModel() {

    val favorites: Flow<List<FavoriteEntity>> = repository.getFavorites()
}
