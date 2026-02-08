package com.example.cravingtimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences(MainActivity.PREFS_FILE, Context.MODE_PRIVATE)
            val endTime = prefs.getLong("TIMER_END_TIME", 0L)
            val now = System.currentTimeMillis()

            if (endTime > now) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    101,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val showIntent = Intent(context, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(context, 102, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarmClockInfo = AlarmManager.AlarmClockInfo(endTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
        }
    }
}
