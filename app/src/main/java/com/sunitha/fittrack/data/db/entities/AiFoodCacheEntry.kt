package com.sunitha.fittrack.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_food_cache",
    indices = [Index(value = ["foodNameKey"], unique = true)]
)
data class AiFoodCacheEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodNameKey: String,
    val name: String,
    val serving: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val category: String
)
