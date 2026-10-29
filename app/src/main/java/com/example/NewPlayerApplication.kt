package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.VideoExtractorServiceImpl
import com.example.data.repository.DownloadRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.VideoRepository
import com.example.download.DownloadManagerHelper
import com.example.player.PlayerManager

class NewPlayerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var videoRepository: VideoRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    lateinit var subscriptionRepository: SubscriptionRepository
        private set

    lateinit var playlistRepository: PlaylistRepository
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var downloadManagerHelper: DownloadManagerHelper
        private set

    lateinit var playerManager: PlayerManager
        private set

    lateinit var authManager: com.example.data.auth.AuthManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        val extractorService = VideoExtractorServiceImpl()

        videoRepository = VideoRepository(extractorService)
        historyRepository = HistoryRepository(database.watchHistoryDao())
        subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
        playlistRepository = PlaylistRepository(database.playlistDao())
        downloadRepository = DownloadRepository(database.downloadDao())

        downloadManagerHelper = DownloadManagerHelper(this, downloadRepository)
        playerManager = PlayerManager.getInstance(this)
        authManager = com.example.data.auth.AuthManager.getInstance(
            this,
            database.subscriptionDao(),
            database.playlistDao()
        )
    }

    companion object {
        lateinit var instance: NewPlayerApplication
            private set
    }
}
