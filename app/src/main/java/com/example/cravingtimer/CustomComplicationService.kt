package com.example.cravingtimer

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class CustomComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val intent = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = Icon.createWithResource(this, android.R.drawable.btn_star)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Timer").build(),
                contentDescription = PlainComplicationText.Builder("Start Timer").build()
            )
            .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
            .setTapAction(pendingIntent)
            .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Craving Timer").build(),
                contentDescription = PlainComplicationText.Builder("Start Craving Timer").build()
            )
            .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
            .setTapAction(pendingIntent)
            .build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = androidx.wear.watchface.complications.data.SmallImage.Builder(
                    icon,
                    SmallImageType.ICON
                ).build(),
                contentDescription = PlainComplicationText.Builder("Start Timer").build()
            )
            .setTapAction(pendingIntent)
            .build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val icon = Icon.createWithResource(this, android.R.drawable.btn_star)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Timer").build(),
                contentDescription = PlainComplicationText.Builder("Preview").build()
            )
            .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
            .build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = androidx.wear.watchface.complications.data.SmallImage.Builder(
                    icon,
                    SmallImageType.ICON
                ).build(),
                contentDescription = PlainComplicationText.Builder("Preview").build()
            )
            .build()

            else -> null
        }
    }
}
