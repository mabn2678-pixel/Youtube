package com.example.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.model.VideoItem
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionsViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val videoRepository: VideoRepository
) : ViewModel() {

    val subscriptions: StateFlow<List<SubscriptionEntity>> = subscriptionRepository.getSubscriptions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _feedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val feedVideos: StateFlow<List<VideoItem>> = _feedVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSubscriptionFeed()
    }

    fun loadSubscriptionFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = videoRepository.getTrendingVideos("all")
            result.onSuccess { videos ->
                _feedVideos.value = videos
            }
            _isLoading.value = false
        }
    }

    fun unsubscribe(channelId: String) {
        viewModelScope.launch {
            subscriptionRepository.toggleSubscription(
                channelId = channelId,
                channelName = "",
                avatarUrl = "",
                subscriberCount = "",
                currentlySubscribed = true
            )
        }
    }
}
