package com.example.data.model

data class VideoItem(
    val id: String,
    val title: String,
    val uploaderName: String,
    val uploaderId: String = "",
    val uploaderAvatarUrl: String = "",
    val durationSeconds: Long = 0,
    val views: Long = 0,
    val uploadedDate: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val subscriberCount: String = ""
)

data class VideoStream(
    val url: String,
    val quality: String, // e.g. "1080p", "720p", "480p", "360p"
    val format: String = "mp4",
    val bitrate: Long = 0,
    val width: Int = 1280,
    val height: Int = 720
)

data class AudioStream(
    val url: String,
    val quality: String = "128kbps",
    val format: String = "m4a",
    val bitrate: Long = 128000
)

data class SubtitleTrack(
    val languageCode: String,
    val languageName: String,
    val url: String
)

data class VideoDetail(
    val item: VideoItem,
    val videoStreams: List<VideoStream> = emptyList(),
    val audioStreams: List<AudioStream> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val relatedVideos: List<VideoItem> = emptyList()
)

data class ChannelInfo(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bannerUrl: String = "",
    val subscriberCountText: String = "",
    val description: String = "",
    val videos: List<VideoItem> = emptyList()
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class PlaybackSpeed(val speed: Float, val label: String) {
    SPEED_0_5(0.5f, "0.5x"),
    SPEED_0_75(0.75f, "0.75x"),
    SPEED_1_0(1.0f, "1.0x (عادي)"),
    SPEED_1_25(1.25f, "1.25x"),
    SPEED_1_5(1.5f, "1.5x"),
    SPEED_2_0(2.0f, "2.0x")
}
