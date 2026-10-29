package com.example.download

import android.content.Context
import android.util.Log
import com.example.data.local.entities.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoItem
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadManagerHelper(
    private val context: Context,
    private val downloadRepository: DownloadRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient()

    fun enqueueDownload(
        video: VideoItem,
        streamUrl: String,
        quality: String,
        isAudioOnly: Boolean
    ) {
        val extension = if (isAudioOnly) "m4a" else "mp4"
        val downloadId = "${video.id}_${quality.replace(" ", "_")}_${if (isAudioOnly) "audio" else "video"}"
        val filename = "newplayer_${video.id}_${System.currentTimeMillis()}.$extension"

        val downloadsDir = File(context.filesDir, "downloads")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val targetFile = File(downloadsDir, filename)

        scope.launch {
            val initialEntity = DownloadEntity(
                id = downloadId,
                videoId = video.id,
                title = video.title,
                channelTitle = video.uploaderName,
                thumbnailUrl = video.thumbnailUrl,
                localFilePath = targetFile.absolutePath,
                isAudioOnly = isAudioOnly,
                quality = quality,
                progress = 0,
                status = DownloadStatus.DOWNLOADING,
                totalBytes = 0,
                downloadedBytes = 0
            )
            downloadRepository.saveDownload(initialEntity)

            try {
                val request = Request.Builder().url(streamUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful || response.body == null) {
                    downloadRepository.updateProgress(downloadId, 0, DownloadStatus.FAILED, 0)
                    return@launch
                }

                val body = response.body!!
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressUpdate = 0

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val currentProgress = ((totalBytesRead * 100) / contentLength).toInt()
                                if (currentProgress >= lastProgressUpdate + 2 || currentProgress == 100) {
                                    lastProgressUpdate = currentProgress
                                    downloadRepository.updateProgress(
                                        downloadId,
                                        currentProgress,
                                        DownloadStatus.DOWNLOADING,
                                        totalBytesRead
                                    )
                                }
                            }
                        }
                    }
                }

                downloadRepository.updateProgress(
                    downloadId,
                    100,
                    DownloadStatus.COMPLETED,
                    totalBytesRead
                )
            } catch (e: Exception) {
                Log.e("Downloader", "Download failed: ${e.message}", e)
                downloadRepository.updateProgress(downloadId, 0, DownloadStatus.FAILED, 0)
            }
        }
    }

    suspend fun removeDownloadedFile(downloadEntity: DownloadEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(downloadEntity.localFilePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.w("Downloader", "Failed to delete file: ${e.message}")
        }
        downloadRepository.deleteDownload(downloadEntity.id)
    }
}
