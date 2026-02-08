package com.example.cravingtimer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.wear.tiles.TileService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CravingTimer:Alarm")
        wl.acquire(15000)

        // 1. DATA CLEANUP & TILE REFRESH
        try {
            val prefs = context.getSharedPreferences("TimerData", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val list = prefs.getString("COMPLETIONS", "") ?: ""
            val timestamps = list.split(",").filter { it.isNotEmpty() }.takeLast(20).toMutableList()
            timestamps.add(now.toString())
            
            prefs.edit()
                .putString("COMPLETIONS", timestamps.joinToString(","))
                .remove("TIMER_END_TIME")
                .commit()

            TileService.getUpdater(context).requestUpdate(TimerTileService::class.java)
        } catch (e: Exception) {}

        // 2. ALERT
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 400, 1000, 400, 1000, 400, 1000)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 101, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer Finished!")
            .setContentText("Tap to dismiss.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(alarmSound)
            .setVibrate(pattern)
            .setFullScreenIntent(pendingIntent, true)
            .setDefaults(Notification.DEFAULT_ALL)

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notificationManager.notify(100, notification)
        
        if (wl.isHeld) wl.release()
    }
}