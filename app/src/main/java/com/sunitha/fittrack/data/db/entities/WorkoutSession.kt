package com.sunitha.fittrack.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val muscleGroup: String,
    val durationMinutes: Int = 0,
    val totalSets: Int = 0,
    val estimatedCalories: Int = 0,
    val notes: String = ""
)
