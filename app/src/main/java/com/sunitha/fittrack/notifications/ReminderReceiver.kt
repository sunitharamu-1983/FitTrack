package com.sunitha.fittrack.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId     = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val channel     = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationHelper.CHANNEL_WORKOUT
        val title       = intent.getStringExtra(EXTRA_TITLE) ?: "FitTrack Reminder"
        val body        = intent.getStringExtra(EXTRA_BODY) ?: ""
        val hour        = intent.getIntExtra(EXTRA_HOUR, 9)
        val minute      = intent.getIntExtra(EXTRA_MINUTE, 0)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 301)

        NotificationHelper.postReminder(context, title, body, notifId, channel)

        // Reschedule for the same time tomorrow
        val reminder = AlarmScheduler.Reminder(
            hour        = hour,
            minute      = minute,
            requestCode = requestCode,
            notifId     = notifId,
            channel     = channel,
            title       = title,
            body        = body
        )
        AlarmScheduler.schedule(context, reminder)
    }

    companion object {
        const val EXTRA_NOTIF_ID     = "notif_id"
        const val EXTRA_CHANNEL      = "channel"
        const val EXTRA_TITLE        = "title"
        const val EXTRA_BODY         = "body"
        const val EXTRA_HOUR         = "hour"
        const val EXTRA_MINUTE       = "minute"
        const val EXTRA_REQUEST_CODE = "request_code"
    }
}
