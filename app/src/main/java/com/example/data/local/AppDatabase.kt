package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.dao.WatchHistoryDao
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistItemEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.WatchHistoryEntity
import com.example.data.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = runCatching {
        DownloadStatus.valueOf(value)
    }.getOrDefault(DownloadStatus.QUEUED)
}

@Database(
    entities = [
        WatchHistoryEntity::class,
        SubscriptionEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "newplayer_local.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
