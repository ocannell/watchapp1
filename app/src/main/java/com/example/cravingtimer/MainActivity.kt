package com.example.cravingtimer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var timerText: TextView
    private lateinit var minLabel: TextView
    private lateinit var mainButton: Button
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var selectedDurationMin = 15

    companion object {
        const val CHANNEL_ID = "craving_timer_channel_v11"
        const val REQUEST_CODE_PERMISSIONS = 101
        const val PREFS_FILE = "TimerData"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerText = findViewById(R.id.timerText)
        minLabel = findViewById(R.id.minLabel)
        mainButton = findViewById(R.id.mainButton)
        btnPlus = findViewById(R.id.btnPlus)
        btnMinus = findViewById(R.id.btnMinus)

        createNotificationChannel()

        btnPlus.setOnClickListener {
            if (!isTimerRunning && selectedDurationMin < 60) {
                selectedDurationMin += 1
                getSharedPreferences(PREFS_FILE, MODE_PRIVATE).edit().putInt("DURATION", selectedDurationMin).commit()
                updateTimeDisplay()
                androidx.wear.tiles.TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)
            }
        }

        btnMinus.setOnClickListener {
            if (!isTimerRunning && selectedDurationMin > 1) {
                selectedDurationMin -= 1
                getSharedPreferences(PREFS_FILE, MODE_PRIVATE).edit().putInt("DURATION", selectedDurationMin).commit()
                updateTimeDisplay()
                androidx.wear.tiles.TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)
            }
        }

        mainButton.setOnClickListener {
            if (isTimerRunning) {
                cancelTimer()
            } else {
                checkPermissionsAndStartTimer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncWithTimerState()
        androidx.wear.tiles.TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)      
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        syncWithTimerState()
    }

    override fun onPause() {
        super.onPause()
        stopUiTimer()
    }

    private fun syncWithTimerState() {
        val prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        selectedDurationMin = prefs.getInt("DURATION", 15)
        val endTime = prefs.getLong("TIMER_END_TIME", 0L)
        val now = System.currentTimeMillis()

        if (endTime > now) {
            startUiTimer(endTime - now)
        } else {
            setSetupMode()
        }
    }

    private fun updateTimeDisplay() {
        timerText.text = selectedDurationMin.toString()
    }

    private fun setSetupMode() {
        isTimerRunning = false
        stopUiTimer()
        mainButton.text = "Start"
        mainButton.backgroundTintList = getColorStateList(android.R.color.holo_green_dark)
        btnPlus.visibility = View.VISIBLE
        btnMinus.visibility = View.VISIBLE
        minLabel.visibility = View.VISIBLE
        updateTimeDisplay()
    }

    private fun setRunningMode() {
        isTimerRunning = true
        mainButton.text = "Cancel"
        mainButton.backgroundTintList = getColorStateList(android.R.color.holo_red_dark)
        btnPlus.visibility = View.INVISIBLE
        btnMinus.visibility = View.INVISIBLE
        minLabel.visibility = View.INVISIBLE
    }

    private fun checkPermissionsAndStartTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_PERMISSIONS)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
             val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
             if (!alarmManager.canScheduleExactAlarms()) {
                 val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                 startActivity(intent)
                 return
             }
        }

        val triggerTime = System.currentTimeMillis() + (selectedDurationMin * 60 * 1000L)
        getSharedPreferences(PREFS_FILE, MODE_PRIVATE).edit().putLong("TIMER_END_TIME", triggerTime).commit()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val showIntent = Intent(this, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(this, 102, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        vibrateConfirm(200)
        startUiTimer(selectedDurationMin * 60 * 1000L)
        androidx.wear.tiles.TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)      
    }

    private fun startUiTimer(durationMs: Long) {
        setRunningMode()
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                val intent = Intent(this@MainActivity, AlarmReceiver::class.java)
                sendBroadcast(intent)
                timerText.text = "00:00"
                setSetupMode()
            }
        }.start()
    }

    private fun stopUiTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun cancelTimer() {
        vibrateConfirm(100)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)

        getSharedPreferences(PREFS_FILE, MODE_PRIVATE).edit().remove("TIMER_END_TIME").commit()
        androidx.wear.tiles.TileService.getUpdater(this).requestUpdate(TimerTileService::class.java)      
        setSetupMode()
    }

    private fun vibrateConfirm(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))  
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
