package com.sunitha.fittrack.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    data class Reminder(
        val hour: Int,
        val minute: Int,
        val requestCode: Int,
        val notifId: Int,
        val channel: String,
        val title: String,
        val body: String
    )

    val reminders = listOf(
        Reminder(
            hour        = 9,
            minute      = 0,
            requestCode = 301,
            notifId     = 2001,
            channel     = NotificationHelper.CHANNEL_WORKOUT,
            title       = "Morning check-in",
            body        = "Log your workout and breakfast to start the day right."
        ),
        Reminder(
            hour        = 13,
            minute      = 30,
            requestCode = 302,
            notifId     = 2002,
            channel     = NotificationHelper.CHANNEL_FOOD,
            title       = "Lunch reminder",
            body        = "Have you logged your lunch? Keep your macros on track."
        ),
        Reminder(
            hour        = 19,
            minute      = 0,
            requestCode = 303,
            notifId     = 2003,
            channel     = NotificationHelper.CHANNEL_WORKOUT,
            title       = "Evening log",
            body        = "Time to wrap up — log dinner and any workouts from today."
        )
    )

    fun scheduleAll(context: Context) {
        NotificationHelper.createChannels(context)
        reminders.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, reminder: Reminder) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE,      reminder.minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID,     reminder.notifId)
            putExtra(ReminderReceiver.EXTRA_CHANNEL,      reminder.channel)
            putExtra(ReminderReceiver.EXTRA_TITLE,        reminder.title)
            putExtra(ReminderReceiver.EXTRA_BODY,         reminder.body)
            putExtra(ReminderReceiver.EXTRA_HOUR,         reminder.hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE,       reminder.minute)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, reminder.requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context, reminder.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }
}
