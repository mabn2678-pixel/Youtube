package com.example.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.PlaylistEntity
import com.example.data.model.PlaybackSpeed
import com.example.data.model.VideoDetail
import com.example.data.model.VideoStream
import com.example.data.repository.DownloadRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.VideoRepository
import com.example.download.DownloadManagerHelper
import com.example.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    object Loading : PlayerUiState
    data class Success(val detail: VideoDetail) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

class PlayerViewModel(
    private val videoId: String,
    private val videoRepository: VideoRepository,
    private val historyRepository: HistoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val playlistRepository: PlaylistRepository,
    val playerManager: PlayerManager,
    private val downloadManagerHelper: DownloadManagerHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isPlaying = playerManager.isPlaying
    val isLoading = playerManager.isLoading
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration
    val selectedQuality = playerManager.selectedQuality
    val selectedSpeed = playerManager.selectedSpeed
    val subtitlesEnabled = playerManager.subtitlesEnabled
    val isBackgroundPlaying = playerManager.isBackgroundPlaying

    init {
        loadVideoDetails()
    }

    fun loadVideoDetails() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            val lastPosition = historyRepository.getPosition(videoId)
            val result = videoRepository.getVideoDetails(videoId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = PlayerUiState.Success(detail)

                    // Check subscription status
                    if (detail.item.uploaderId.isNotEmpty()) {
                        subscriptionRepository.isSubscribed(detail.item.uploaderId).collect { sub ->
                            _isSubscribed.value = sub
                        }
                    }

                    // Start playback
                    playerManager.playVideo(detail, startPositionMs = lastPosition)

                    // Record to history
                    historyRepository.recordWatch(
                        videoId = detail.item.id,
                        title = detail.item.title,
                        channelTitle = detail.item.uploaderName,
                        thumbnailUrl = detail.item.thumbnailUrl,
                        durationSeconds = detail.item.durationSeconds,
                        positionMs = lastPosition
                    )
                },
                onFailure = { err ->
                    _uiState.value = PlayerUiState.Error(err.localizedMessage ?: "فشل تحميل تفاصيل الفيديو")
                }
            )
        }
    }

    fun toggleSubscription() {
        val state = _uiState.value
        if (state !is PlayerUiState.Success) return
        val item = state.detail.item
        val channelId = if (item.uploaderId.isNotEmpty()) item.uploaderId else item.uploaderName

        viewModelScope.launch {
            subscriptionRepository.toggleSubscription(
                channelId = channelId,
                channelName = item.uploaderName,
                avatarUrl = item.uploaderAvatarUrl,
                subscriberCount = item.subscriberCount,
                currentlySubscribed = _isSubscribed.value
            )
            _isSubscribed.value = !_isSubscribed.value
        }
    }

    fun setQuality(stream: VideoStream) {
        playerManager.setQuality(stream)
    }

    fun setSpeed(speed: PlaybackSpeed) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun toggleSubtitles() {
        playerManager.toggleSubtitles()
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
        // Update history position
        val state = _uiState.value
        if (state is PlayerUiState.Success) {
            viewModelScope.launch {
                historyRepository.recordWatch(
                    videoId = state.detail.item.id,
                    title = state.detail.item.title,
                    channelTitle = state.detail.item.uploaderName,
                    thumbnailUrl = state.detail.item.thumbnailUrl,
                    durationSeconds = state.detail.item.durationSeconds,
                    positionMs = positionMs
                )
            }
        }
    }

    fun seekForward() = playerManager.seekForward()
    fun seekBackward() = playerManager.seekBackward()

    fun toggleBackgroundPlayback() {
        if (isBackgroundPlaying.value) {
            playerManager.stopBackgroundPlay()
        } else {
            playerManager.enableBackgroundPlay()
        }
    }

    fun downloadMedia(streamUrl: String, quality: String, isAudio: Boolean) {
        val state = _uiState.value
        if (state is PlayerUiState.Success) {
            downloadManagerHelper.enqueueDownload(
                video = state.detail.item,
                streamUrl = streamUrl,
                quality = quality,
                isAudioOnly = isAudio
            )
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(title)
        }
    }

    fun addToPlaylist(playlistId: Long) {
        val state = _uiState.value
        if (state is PlayerUiState.Success) {
            viewModelScope.launch {
                playlistRepository.addVideoToPlaylist(playlistId, state.detail.item)
            }
        }
    }
}
