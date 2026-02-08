package com.example.cravingtimer

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.wear.tiles.TileService

class CancelActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val prefs = getSharedPreferences("TimerData", Context.MODE_PRIVATE)
        prefs.edit().remove("TIMER_END_TIME").apply()

        TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)
        finish()
    }
}
