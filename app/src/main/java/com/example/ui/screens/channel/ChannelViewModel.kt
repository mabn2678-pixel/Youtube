package com.example.ui.screens.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChannelInfo
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ChannelUiState {
    object Loading : ChannelUiState
    data class Success(val channel: ChannelInfo) : ChannelUiState
    data class Error(val message: String) : ChannelUiState
}

class ChannelViewModel(
    private val channelId: String,
    private val videoRepository: VideoRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    init {
        loadChannel()
    }

    fun loadChannel() {
        viewModelScope.launch {
            _uiState.value = ChannelUiState.Loading
            subscriptionRepository.isSubscribed(channelId).collect { sub ->
                _isSubscribed.value = sub
            }
        }
        viewModelScope.launch {
            val result = videoRepository.getChannelDetails(channelId)
            result.fold(
                onSuccess = { channel ->
                    _uiState.value = ChannelUiState.Success(channel)
                },
                onFailure = { err ->
                    _uiState.value = ChannelUiState.Error(err.localizedMessage ?: "فشل تحميل تفاصيل القناة")
                }
            )
        }
    }

    fun toggleSubscription() {
        val state = _uiState.value
        if (state !is ChannelUiState.Success) return
        val channel = state.channel
        viewModelScope.launch {
            subscriptionRepository.toggleSubscription(
                channelId = channel.id,
                channelName = channel.name,
                avatarUrl = channel.avatarUrl,
                subscriberCount = channel.subscriberCountText,
                currentlySubscribed = _isSubscribed.value
            )
            _isSubscribed.value = !_isSubscribed.value
        }
    }
}
