package com.sunitha.fittrack.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        when (type) {
            TYPE_WORKOUT -> NotificationHelper.showWorkoutReminder(applicationContext)
            TYPE_FOOD    -> {
                val meal = inputData.getString(KEY_MEAL) ?: "meal"
                NotificationHelper.showFoodReminder(applicationContext, meal)
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "reminder_type"
        const val KEY_MEAL = "meal_name"
        const val TYPE_WORKOUT = "workout"
        const val TYPE_FOOD    = "food"

        fun scheduleDaily(context: Context) {
            NotificationHelper.createChannels(context)
            val wm = WorkManager.getInstance(context)

            // Workout reminder at ~7 AM (delay from now to simulate — real app uses exact time)
            val workoutWork = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(KEY_TYPE to TYPE_WORKOUT))
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            // Lunch reminder
            val lunchWork = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(KEY_TYPE to TYPE_FOOD, KEY_MEAL to "lunch"))
                .setInitialDelay(6, TimeUnit.HOURS)
                .build()

            // Dinner reminder
            val dinnerWork = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(KEY_TYPE to TYPE_FOOD, KEY_MEAL to "dinner"))
                .setInitialDelay(12, TimeUnit.HOURS)
                .build()

            wm.enqueueUniquePeriodicWork("workout_reminder", ExistingPeriodicWorkPolicy.KEEP, workoutWork)
            wm.enqueueUniquePeriodicWork("lunch_reminder",   ExistingPeriodicWorkPolicy.KEEP, lunchWork)
            wm.enqueueUniquePeriodicWork("dinner_reminder",  ExistingPeriodicWorkPolicy.KEEP, dinnerWork)
        }
    }
}
