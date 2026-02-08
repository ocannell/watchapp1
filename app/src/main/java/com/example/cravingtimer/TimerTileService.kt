package com.example.cravingtimer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.Calendar

class TimerTileService : TileService() {
    private val RESOURCES_VERSION = "1"

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val futures = SettableFuture.create<TileBuilders.Tile>()
        val prefs = getSharedPreferences("TimerData", Context.MODE_PRIVATE)

        val lastClickId = requestParams.state?.lastClickableId ?: ""
        if (lastClickId == "start_btn") {
            val durationMin = prefs.getInt("DURATION", 15)
            val triggerTime = System.currentTimeMillis() + (durationMin * 60 * 1000L)
            prefs.edit().putLong("TIMER_END_TIME", triggerTime).commit()

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 101, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val showIntent = Intent(this, MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(this, 102, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        } else if (lastClickId == "cancel_btn") {
            prefs.edit().remove("TIMER_END_TIME").commit()
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 101, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)
        }

        val endTime = prefs.getLong("TIMER_END_TIME", 0L)
        val durationMin = prefs.getInt("DURATION", 15)
        val currentTime = System.currentTimeMillis()
        val isRunning = endTime > currentTime
        val countToday = getCompletionCount(prefs)

        val openAppIntent = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(ActionBuilders.AndroidActivity.Builder()
                .setPackageName(this.packageName)
                .setClassName(MainActivity::class.java.name)
                .build()).build()

        val startAction = ActionBuilders.LoadAction.Builder().build()
        val cancelAction = ActionBuilders.LoadAction.Builder().build()

        val timelineBuilder = TimelineBuilders.Timeline.Builder()

        if (isRunning) {
            val totalRemainingMs = endTime - currentTime
            val minutesToBuffer = (totalRemainingMs / 60000).toInt()

            for (i in 0..minutesToBuffer) {
                val displayMins = minutesToBuffer - i
                val entryStart = currentTime + (i * 60000)
                val timeLabel = if (displayMins == 0) "<1 min left" else "$displayMins min left"

                val layout = buildRunningLayout(timeLabel, countToday, cancelAction, openAppIntent)

                timelineBuilder.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder()
                    .setValidity(TimelineBuilders.TimeInterval.Builder()
                        .setStartMillis(entryStart)
                        .setEndMillis(if (i == minutesToBuffer) endTime else entryStart + 60000)
                        .build())
                    .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
                    .build())
            }

            val finishedLayout = buildIdleLayout(durationMin, countToday + 1, startAction, openAppIntent)
            timelineBuilder.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder()
                .setValidity(TimelineBuilders.TimeInterval.Builder()
                    .setStartMillis(endTime)
                    .build())
                .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(finishedLayout).build())
                .build())

        } else {
            val layout = buildIdleLayout(durationMin, countToday, startAction, openAppIntent)
            timelineBuilder.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder()
                .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
                .build())
        }

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion((currentTime / 1000).toString())
            .setTimeline(timelineBuilder.build())
            .setFreshnessIntervalMillis(60000)
            .build()

        futures.set(tile)
        return futures
    }

    private fun buildRunningLayout(timeLabel: String, count: Int, cancelAction: ActionBuilders.LoadAction, openAppAction: ActionBuilders.LaunchAction): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(timeLabel)
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(20f)).setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD).build())
                    .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(ModifiersBuilders.Clickable.Builder().setId("open_app").setOnClick(openAppAction).build()).build())
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("($count done today)")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(12f)).build())
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f)).build())
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("CANCEL")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(14f)).build())
                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                        .setClickable(ModifiersBuilders.Clickable.Builder().setId("cancel_btn").setOnClick(cancelAction).build())
                        .setBackground(ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(0xFFD32F2F.toInt())).setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(10f)).build()).build())
                        .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(8f)).build())
                        .build())
                    .build()
            )
            .build()
    }

    private fun buildIdleLayout(durationMin: Int, count: Int, startAction: ActionBuilders.LoadAction, openAppAction: ActionBuilders.LaunchAction): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Start ${durationMin}m")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(22f)).build())
                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                        .setClickable(ModifiersBuilders.Clickable.Builder().setId("start_btn").setOnClick(startAction).build())
                        .setBackground(ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(0xFF4CAF50.toInt())).setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(20f)).build()).build())
                        .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(16f)).build())
                        .build())
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("$count completed today")
                    .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(12f)).build())
                    .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(ModifiersBuilders.Clickable.Builder().setId("open_app_idle").setOnClick(openAppAction).build()).build())
                    .build()
            )
            .build()
    }

    private fun getCompletionCount(prefs: android.content.SharedPreferences): Int {
        val list = prefs.getString("COMPLETIONS", "") ?: ""
        if (list.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val midnight = calendar.timeInMillis
        val timestamps = list.split(",").mapNotNull { it.toLongOrNull() }
        return timestamps.filter { it >= midnight }.size
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val futures = SettableFuture.create<ResourceBuilders.Resources>()
        futures.set(ResourceBuilders.Resources.Builder().setVersion(requestParams.version).build())       
        return futures
    }
}
