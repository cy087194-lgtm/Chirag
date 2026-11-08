package com.Lia.assistant.agent.gaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Lia.assistant.MainActivity
import com.Lia.assistant.R

class AllRounderCoachService : Service() {

    companion object {
        private const val TAG = "CoachForegroundService"
        const val ACTION_START = "com.Lia.assistant.action.START_COACH"
        const val ACTION_STOP = "com.Lia.assistant.action.STOP_COACH"
        private const val CHANNEL_ID = "lia_coach_watch_channel"
        private const val NOTIFICATION_ID = 2002

        fun start(context: Context) {
            try {
                val intent = Intent(context, AllRounderCoachService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start AllRounderCoachService: ${e.message}", e)
            }
        }

        fun startWithTokens(context: Context, resultCode: Int, data: Intent) {
            try {
                val intent = Intent(context, AllRounderCoachService::class.java).apply {
                    action = ACTION_START
                    putExtra("EXTRA_RESULT_CODE", resultCode)
                    putExtra("EXTRA_RESULT_DATA", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start AllRounderCoachService with tokens: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AllRounderCoachService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to stop AllRounderCoachService: ${e.message}", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AllRounderCoachEngine.stopCoaching()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("EXTRA_RESULT_CODE", -1) ?: -1
        @Suppress("DEPRECATION")
        val resultData = intent?.getParcelableExtra<Intent>("EXTRA_RESULT_DATA")
        if (resultCode != -1 && resultData != null && !ScreenCaptureManager.hasActiveMediaProjection()) {
            ScreenCaptureManager.initMediaProjection(resultCode, resultData, this)
        }

        createNotificationChannel()
        val notification = buildNotification("Coach watching screen (Real-time)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (ScreenCaptureManager.hasActiveMediaProjection()) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "startForeground error: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {}
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Coach Watching Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Permanent notification indicating Coach is actively monitoring screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AllRounderCoachService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LiaAI Real-Time Screen Coach")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop Coach", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        AllRounderCoachEngine.stopCoaching()
        super.onDestroy()
    }
}
