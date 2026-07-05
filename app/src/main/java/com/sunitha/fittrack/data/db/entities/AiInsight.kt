package com.sunitha.fittrack.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AiInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val type: String,     // WEEKLY_SUMMARY, REST_DAY, EXERCISE_TIP
    val content: String
)
