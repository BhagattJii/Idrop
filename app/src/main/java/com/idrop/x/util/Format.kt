package com.idrop.x.util

import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unit])
}

fun formatRelativeTime(timestamp: Long): String {
    val seconds = ((System.currentTimeMillis() - timestamp) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "just now"
        seconds < 3600L -> "${seconds / 60L}m ago"
        seconds < 86_400L -> "${seconds / 3600L}h ago"
        else -> "${seconds / 86_400L}d ago"
    }
}
