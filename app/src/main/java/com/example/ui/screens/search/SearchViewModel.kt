package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Results(val videos: List<VideoItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _searchHistory = MutableStateFlow(
        listOf("برمجة أندرويد كوتلن", "وثائقيات علمية", "شرح Compose", "بودكاست تقني")
    )
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _suggestions.value = emptyList()
            _uiState.value = SearchUiState.Idle
            return
        }

        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(300)
            val result = videoRepository.getSearchSuggestions(newQuery)
            _suggestions.value = result
        }
    }

    fun search(q: String = _query.value) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        _query.value = trimmed
        _suggestions.value = emptyList()

        // Update history
        val updatedHistory = (_searchHistory.value.toMutableList().apply {
            remove(trimmed)
            add(0, trimmed)
        }).take(10)
        _searchHistory.value = updatedHistory

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val res = videoRepository.searchVideos(trimmed)
            res.fold(
                onSuccess = { list ->
                    _uiState.value = SearchUiState.Results(list)
                },
                onFailure = { err ->
                    _uiState.value = SearchUiState.Error(err.localizedMessage ?: "حدث خطأ أثناء البحث")
                }
            )
        }
    }

    fun clearQuery() {
        _query.value = ""
        _suggestions.value = emptyList()
        _uiState.value = SearchUiState.Idle
    }

    fun removeHistoryItem(item: String) {
        _searchHistory.value = _searchHistory.value.filter { it != item }
    }
}
