package com.example.cravingtimer

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.wear.tiles.TileService

class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("TimerData", Context.MODE_PRIVATE)
        val durationMin = prefs.getInt("DURATION", 15)
        val durationMs = durationMin * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + durationMs
        
        // 1. Save State
        prefs.edit().putLong("TIMER_END_TIME", triggerTime).commit()
        
        // 2. Set Alarm (use AlarmClock to bypass standby quotas)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 101, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(this, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            this, 102, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        
        // 3. Refresh Tile
        TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)
        
        // 4. Finish
        finish()
    }
}