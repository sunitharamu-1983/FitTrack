package com.sunitha.fittrack.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_entries")
data class PeriodEntry(
    @PrimaryKey val dateMillis: Long,
    val note: String = ""
)
