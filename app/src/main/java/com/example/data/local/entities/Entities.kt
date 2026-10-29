package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val playbackPositionMs: Long,
    val watchedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscriberCount: String,
    val subscribedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "videoId"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String, // typically videoId + "_" + quality/audio
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val localFilePath: String,
    val isAudioOnly: Boolean,
    val quality: String,
    val progress: Int, // 0 - 100
    val status: DownloadStatus,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
