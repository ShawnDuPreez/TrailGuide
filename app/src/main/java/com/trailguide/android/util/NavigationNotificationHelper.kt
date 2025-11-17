package com.trailguide.android.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.trailguide.android.R
import com.trailguide.android.data.model.NavigationStats
import kotlin.math.roundToInt

/**
 * Helper for building the persistent navigation notification (Google Maps style).
 */
object NavigationNotificationHelper {

    const val ACTION_PAUSE = "com.trailguide.android.navigation.ACTION_PAUSE"
    const val ACTION_RESUME = "com.trailguide.android.navigation.ACTION_RESUME"
    const val ACTION_STOP = "com.trailguide.android.navigation.ACTION_STOP"

    fun buildNavigationNotification(
        context: Context,
        stats: NavigationStats?,
        isPaused: Boolean,
        contentIntent: PendingIntent,
        pauseIntent: PendingIntent,
        resumeIntent: PendingIntent,
        stopIntent: PendingIntent
    ): Notification {
        // Ensure navigation channel is created
        NotificationUtil.createNotificationChannels(context)

        val title = if (stats != null) {
            "${stats.trailName} • ${stats.progressPercent.roundToInt()}%"
        } else {
            context.getString(R.string.app_name)
        }

        val statusText = when {
            stats == null -> "Calculating route..."
            isPaused -> "Navigation paused"
            else -> formatStatusLine(context, stats)
        }

        val builder = NotificationCompat.Builder(context, NotificationUtil.CHANNEL_NAVIGATION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Trail Navigation")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$statusText"))
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        val progress = stats?.progressPercent?.roundToInt() ?: 0
        builder.setProgress(100, progress.coerceIn(0, 100), stats == null)

        if (isPaused) {
            builder.addAction(0, "Resume", resumeIntent)
        } else {
            builder.addAction(0, "Pause", pauseIntent)
        }

        builder.addAction(0, "Stop", stopIntent)

        return builder.build()
    }

    private fun formatStatusLine(context: Context, stats: NavigationStats): String {
        val distanceRemainingKm = stats.distanceRemainingMeters / 1000.0
        val pace = stats.currentPaceMinPerKm ?: stats.averagePaceMinPerKm
        val paceText = pace?.let { formatPace(it) } ?: "—"
        val etaText = stats.etaMillis?.let { android.text.format.DateFormat.getTimeFormat(context).format(it) } ?: "—"
        val elevation = if (stats.currentElevationMeters != null) {
            "${stats.currentElevationMeters} m"
        } else {
            "—"
        }

        return buildString {
            append("Remain: ${"%.2f".format(distanceRemainingKm)} km • ")
            append("ETA: $etaText • Pace: $paceText • Elev: $elevation")
        }
    }

    private fun formatPace(paceMinPerKm: Double): String {
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).roundToInt()
        return "%d:%02d /km".format(minutes, seconds)
    }
}

