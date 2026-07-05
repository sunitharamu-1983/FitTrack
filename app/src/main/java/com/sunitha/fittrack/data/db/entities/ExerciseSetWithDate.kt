package com.sunitha.fittrack.data.db.entities

data class ExerciseSetWithDate(
    val exerciseName: String,
    val reps: Int,
    val weightKg: Float,
    val dateMillis: Long
)
