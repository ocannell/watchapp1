package com.example.cravingtimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.wear.tiles.TileService

class TileActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.cravingtimer.ACTION_CANCEL") {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)

            val prefs = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
            prefs.edit().remove("TIMER_END_TIME").apply()

            TileService.getUpdater(context).requestUpdate(TimerTileService::class.java)
        }
    }
}
