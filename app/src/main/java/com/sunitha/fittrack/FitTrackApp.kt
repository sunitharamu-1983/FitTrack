package com.sunitha.fittrack

import android.app.Application
import androidx.work.WorkManager
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.db.FitTrackDatabase
import com.sunitha.fittrack.data.repository.FitTrackRepository
import com.sunitha.fittrack.notifications.AlarmScheduler

class FitTrackApp : Application() {
    val database     by lazy { FitTrackDatabase.getInstance(this) }
    val repository   by lazy { FitTrackRepository(database) }
    val profileStore by lazy { UserProfileStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Cancel old WorkManager reminders (replaced by exact AlarmManager alarms)
        WorkManager.getInstance(this).cancelUniqueWork("workout_reminder")
        WorkManager.getInstance(this).cancelUniqueWork("lunch_reminder")
        WorkManager.getInstance(this).cancelUniqueWork("dinner_reminder")
        AlarmScheduler.scheduleAll(this)
    }
}
