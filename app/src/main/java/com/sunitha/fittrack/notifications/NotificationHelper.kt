package com.sunitha.fittrack.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.sunitha.fittrack.MainActivity
import com.sunitha.fittrack.R

object NotificationHelper {

    const val CHANNEL_WORKOUT  = "fittrack_workout_reminder"
    const val CHANNEL_FOOD     = "fittrack_food_reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_WORKOUT, "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily workout logging reminder"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FOOD, "Food Reminders",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Meal logging reminder"
            }
        )
    }

    fun postReminder(context: Context, title: String, body: String, notifId: Int, channel: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra(NotificationDismissReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context, notifId + 10_000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    // Keep old methods so existing WorkManager jobs don't crash if still enqueued
    fun showWorkoutReminder(context: Context) =
        postReminder(context, "Morning check-in", "Log your workout and breakfast to start the day right.", 2001, CHANNEL_WORKOUT)

    fun showFoodReminder(context: Context, mealName: String = "meal") =
        postReminder(context, "Log your $mealName", "Keep your macros on track — tap to open FitTrack.", 2002, CHANNEL_FOOD)

    // Reminder notification IDs currently in use — cancelled whenever the app comes
    // to the foreground by any path (not just by tapping the notification itself).
    private val reminderNotifIds = listOf(2001, 2002)

    fun cancelAllReminders(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        reminderNotifIds.forEach { manager.cancel(it) }
    }
}
