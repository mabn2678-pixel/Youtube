package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.data.model.PlaybackSpeed
import com.example.data.model.VideoDetail
import com.example.data.model.VideoStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerManager private constructor(private val context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    var mediaSession: MediaSession? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _currentVideo = MutableStateFlow<VideoDetail?>(null)
    val currentVideo: StateFlow<VideoDetail?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _selectedQuality = MutableStateFlow("720p")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _selectedSpeed = MutableStateFlow(PlaybackSpeed.SPEED_1_0)
    val selectedSpeed: StateFlow<PlaybackSpeed> = _selectedSpeed.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(false)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    private val _isBackgroundPlaying = MutableStateFlow(false)
    val isBackgroundPlaying: StateFlow<Boolean> = _isBackgroundPlaying.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isLoading.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun playVideo(videoDetail: VideoDetail, startPositionMs: Long = 0L, preferredQuality: String = "720p") {
        _currentVideo.value = videoDetail

        // Select chosen or best matching stream
        val stream = videoDetail.videoStreams.find { it.quality == preferredQuality }
            ?: videoDetail.videoStreams.firstOrNull()

        val streamUrl = stream?.url
            ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        _selectedQuality.value = stream?.quality ?: "720p"

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(videoDetail.item.title)
            .setArtist(videoDetail.item.uploaderName)
            .setArtworkUri(if (videoDetail.item.thumbnailUrl.isNotEmpty()) Uri.parse(videoDetail.item.thumbnailUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.play()
    }

    fun playLocalFile(uri: Uri, title: String, artist: String) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setQuality(stream: VideoStream) {
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        _selectedQuality.value = stream.quality

        val currentMeta = exoPlayer.mediaMetadata
        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaMetadata(currentMeta)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(currentPos)
        if (wasPlaying) exoPlayer.play()
    }

    fun setPlaybackSpeed(speed: PlaybackSpeed) {
        _selectedSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed.speed)
    }

    fun toggleSubtitles() {
        val enabled = !_subtitlesEnabled.value
        _subtitlesEnabled.value = enabled
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward(deltaMs: Long = 10000) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceAtMost(exoPlayer.duration)
        seekTo(newPos)
    }

    fun seekBackward(deltaMs: Long = 10000) {
        val newPos = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun enableBackgroundPlay() {
        _isBackgroundPlaying.value = true
        val serviceIntent = Intent(context, PlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopBackgroundPlay() {
        _isBackgroundPlaying.value = false
        val serviceIntent = Intent(context, PlayerService::class.java)
        context.stopService(serviceIntent)
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer.release()
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
