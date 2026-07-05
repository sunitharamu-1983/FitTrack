package com.sunitha.fittrack

import android.app.Application
import androidx.work.WorkManager
import com.sunitha.fittrack.data.datastore.ThemePreferenceStore
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.db.FitTrackDatabase
import com.sunitha.fittrack.data.repository.FitTrackRepository
import com.sunitha.fittrack.notifications.AlarmScheduler
import androidx.glance.appwidget.updateAll
import com.sunitha.fittrack.widget.FitTrackWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FitTrackApp : Application() {
    val database     by lazy { FitTrackDatabase.getInstance(this) }
    val repository   by lazy { FitTrackRepository(database) }
    val profileStore by lazy { UserProfileStore(this) }
    val themeStore   by lazy { ThemePreferenceStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Cancel old WorkManager reminders (replaced by exact AlarmManager alarms)
        WorkManager.getInstance(this).cancelUniqueWork("workout_reminder")
        WorkManager.getInstance(this).cancelUniqueWork("lunch_reminder")
        WorkManager.getInstance(this).cancelUniqueWork("dinner_reminder")
        AlarmScheduler.scheduleAll(this)

        // Refresh the home screen widget whenever any data it displays changes,
        // regardless of which screen made the change.
        appScope.launch {
            combine(
                repository.getAllWorkoutSessions(),
                repository.getRecentRestDays(1),
                repository.getRecentWalkDays(1),
                repository.getTodayCalories(),
                repository.getTodaySteps()
            ) { _, _, _, _, _ -> Unit }
                .collect { FitTrackWidget().updateAll(this@FitTrackApp) }
        }
    }
}
