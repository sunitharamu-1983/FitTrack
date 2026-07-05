package com.sunitha.fittrack.data

import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.db.entities.RestDayEntry
import com.sunitha.fittrack.data.db.entities.StepEntry
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import java.util.Calendar

object StreakCalculator {

    fun calculate(
        sessions: List<WorkoutSession>,
        restDays: List<RestDayEntry>,
        walkDays: List<StepEntry> = emptyList(),
        foodEntries: List<FoodEntry> = emptyList(),
        weightEntries: List<WeightEntry> = emptyList()
    ): Int {
        val activeDays = buildSet {
            sessions.forEach      { add(startOfDay(it.dateMillis)) }
            restDays.forEach      { add(startOfDay(it.dateMillis)) }
            walkDays.forEach      { add(startOfDay(it.dateMillis)) }
            foodEntries.forEach   { add(startOfDay(it.dateMillis)) }
            weightEntries.forEach { add(startOfDay(it.dateMillis)) }
        }
        if (activeDays.isEmpty()) return 0
        val today = startOfDay(System.currentTimeMillis())
        var current = if (activeDays.contains(today)) today else prevDay(today)
        var streak = 0
        while (activeDays.contains(current)) {
            streak++
            current = prevDay(current)
        }
        return streak
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun prevDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
}
