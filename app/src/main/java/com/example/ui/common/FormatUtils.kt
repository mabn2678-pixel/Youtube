package com.example.ui.common

import java.util.Locale

object FormatUtils {
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }

    fun formatDurationMs(millis: Long): String {
        return formatDuration(millis / 1000)
    }

    fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", views / 1_000_000.0)
            views >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", views / 1_000.0)
            else -> views.toString()
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
