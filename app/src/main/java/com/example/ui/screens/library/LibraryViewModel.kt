package com.example.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistItemEntity
import com.example.data.local.entities.WatchHistoryEntity
import com.example.data.repository.DownloadRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.PlaylistRepository
import com.example.download.DownloadManagerHelper
import com.example.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(
    private val historyRepository: HistoryRepository,
    private val downloadRepository: DownloadRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadManagerHelper: DownloadManagerHelper,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: History, 1: Downloads, 2: Playlists
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val historyList: StateFlow<List<WatchHistoryEntity>> = historyRepository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadList: StateFlow<List<DownloadEntity>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistList: StateFlow<List<PlaylistEntity>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedPlaylistItems = MutableStateFlow<List<PlaylistItemEntity>>(emptyList())
    val selectedPlaylistItems: StateFlow<List<PlaylistItemEntity>> = _selectedPlaylistItems.asStateFlow()

    private val _viewingPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val viewingPlaylist: StateFlow<PlaylistEntity?> = _viewingPlaylist.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        _viewingPlaylist.value = null
    }

    fun removeHistoryItem(videoId: String) {
        viewModelScope.launch {
            historyRepository.removeVideo(videoId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun deleteDownload(entity: DownloadEntity) {
        viewModelScope.launch {
            downloadManagerHelper.removeDownloadedFile(entity)
        }
    }

    fun playOfflineDownload(entity: DownloadEntity) {
        val file = File(entity.localFilePath)
        if (file.exists()) {
            playerManager.playLocalFile(
                uri = Uri.fromFile(file),
                title = entity.title,
                artist = entity.channelTitle
            )
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(title)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            if (_viewingPlaylist.value?.id == playlistId) {
                _viewingPlaylist.value = null
            }
        }
    }

    fun openPlaylist(playlist: PlaylistEntity) {
        _viewingPlaylist.value = playlist
        viewModelScope.launch {
            playlistRepository.getPlaylistItems(playlist.id).collect { items ->
                _selectedPlaylistItems.value = items
            }
        }
    }

    fun closePlaylistView() {
        _viewingPlaylist.value = null
    }

    fun removePlaylistItem(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            playlistRepository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }
}
