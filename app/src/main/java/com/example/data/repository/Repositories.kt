package com.example.data.repository

import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.dao.WatchHistoryDao
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistItemEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.WatchHistoryEntity
import com.example.data.model.ChannelInfo
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoDetail
import com.example.data.model.VideoItem
import com.example.data.remote.VideoExtractorService
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val extractorService: VideoExtractorService
) {
    suspend fun getTrendingVideos(category: String = "all"): Result<List<VideoItem>> {
        return extractorService.getTrendingVideos(category)
    }

    suspend fun searchVideos(query: String, filter: String = "all"): Result<List<VideoItem>> {
        return extractorService.searchVideos(query, filter)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return extractorService.getSearchSuggestions(query)
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoDetail> {
        return extractorService.getVideoDetails(videoId)
    }

    suspend fun getChannelDetails(channelId: String): Result<ChannelInfo> {
        return extractorService.getChannelDetails(channelId)
    }

    suspend fun getShortsVideos(): Result<List<VideoItem>> {
        return extractorService.getShortsVideos()
    }
}

class HistoryRepository(
    private val watchHistoryDao: WatchHistoryDao
) {
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllHistory()

    suspend fun recordWatch(
        videoId: String,
        title: String,
        channelTitle: String,
        thumbnailUrl: String,
        durationSeconds: Long,
        positionMs: Long
    ) {
        watchHistoryDao.insertOrUpdate(
            WatchHistoryEntity(
                videoId = videoId,
                title = title,
                channelTitle = channelTitle,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                playbackPositionMs = positionMs,
                watchedAtTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeVideo(videoId: String) {
        watchHistoryDao.deleteById(videoId)
    }

    suspend fun clearHistory() {
        watchHistoryDao.clearAll()
    }

    suspend fun getPosition(videoId: String): Long {
        return watchHistoryDao.getByVideoId(videoId)?.playbackPositionMs ?: 0L
    }
}

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao
) {
    fun getSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    fun isSubscribed(channelId: String): Flow<Boolean> = subscriptionDao.isSubscribed(channelId)

    suspend fun toggleSubscription(
        channelId: String,
        channelName: String,
        avatarUrl: String,
        subscriberCount: String,
        currentlySubscribed: Boolean
    ) {
        if (currentlySubscribed) {
            subscriptionDao.unsubscribe(channelId)
        } else {
            subscriptionDao.subscribe(
                SubscriptionEntity(
                    channelId = channelId,
                    channelName = channelName,
                    avatarUrl = avatarUrl,
                    subscriberCount = subscriberCount
                )
            )
        }
    }
}

class PlaylistRepository(
    private val playlistDao: PlaylistDao
) {
    fun getPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> =
        playlistDao.getPlaylistItems(playlistId)

    suspend fun createPlaylist(title: String, description: String = ""): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(title = title, description = description))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addVideoToPlaylist(
        playlistId: Long,
        video: VideoItem
    ) {
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoId = video.id,
                title = video.title,
                channelTitle = video.uploaderName,
                thumbnailUrl = video.thumbnailUrl,
                durationSeconds = video.durationSeconds
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        playlistDao.removePlaylistItem(playlistId, videoId)
    }
}

class DownloadRepository(
    private val downloadDao: DownloadDao
) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun saveDownload(download: DownloadEntity) {
        downloadDao.insertOrUpdate(download)
    }

    suspend fun updateProgress(id: String, progress: Int, status: DownloadStatus, downloadedBytes: Long) {
        downloadDao.updateProgress(id, progress, status, downloadedBytes)
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteById(id)
    }

    suspend fun getDownload(id: String): DownloadEntity? {
        return downloadDao.getById(id)
    }
}
