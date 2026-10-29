package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val videos: List<VideoItem>,
        val shorts: List<VideoItem> = emptyList()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        loadHomeData("all")
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        loadHomeData(category)
    }

    fun loadHomeData(category: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val videoResult = videoRepository.getTrendingVideos(category)
            val shortsResult = videoRepository.getShortsVideos()

            val shortsList = shortsResult.getOrDefault(emptyList())

            videoResult.fold(
                onSuccess = { list ->
                    _uiState.value = HomeUiState.Success(videos = list, shorts = shortsList)
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(error.localizedMessage ?: "فشل في الاتصال بيوتيوب")
                }
            )
        }
    }
}
