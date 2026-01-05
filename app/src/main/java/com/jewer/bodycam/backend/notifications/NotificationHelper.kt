package com.jewer.bodycam.backend.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.jewer.bodycam.MainActivity
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.functions.getBodycamBrand

object NotificationHelper {

    private const val CHANNEL_ID = "screen_recording_channel"

    // 更新前台服務通知
    fun createNotification(context: Context): Notification {
        // 建立前台服務通知 pendingIntent
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notifyTitle = "Recording..."
        val notifyText = when(getBodycamBrand(context)) {
            "AXON" -> "Tap top right “AXON” icon to stop recording"
            "MOTOROLA" -> "Tap top left “MOTOROLA” icon to stop recording"
            "TRANSCEND" -> "Tap bottom left “TRANSCEND” icon to stop recording"
            else -> "Tap the icon to stop recording"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(notifyTitle)
            .setContentText(notifyText)
            .setSmallIcon(R.mipmap.ic_water_mark_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 防止被滑掉
            .build()
    }

    // 建立前台服務通知頻道
    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recording Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager?.createNotificationChannel(serviceChannel)
    }
}