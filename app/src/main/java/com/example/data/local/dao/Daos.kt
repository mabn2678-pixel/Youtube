package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistItemEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.WatchHistoryEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watchedAtTimestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getByVideoId(videoId: String): WatchHistoryEntity?
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribe(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun unsubscribe(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun isSubscribed(channelId: String): Flow<Boolean>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removePlaylistItem(playlistId: Long, videoId: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, status = :status, downloadedBytes = :downloadedBytes WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, status: DownloadStatus, downloadedBytes: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?
}
