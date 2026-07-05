package com.sunitha.fittrack.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_entries")
data class StepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val steps: Int,
    val isWalkDay: Boolean = false
)
