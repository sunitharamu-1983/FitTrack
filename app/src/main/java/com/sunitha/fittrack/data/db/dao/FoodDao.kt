package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FoodEntry)

    @Delete
    suspend fun deleteEntry(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY mealType, dateMillis")
    fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE dateMillis >= :fromMillis ORDER BY dateMillis DESC")
    suspend fun getEntriesSince(fromMillis: Long): List<FoodEntry>

    @Query("SELECT * FROM food_entries WHERE dateMillis >= :fromMillis ORDER BY dateMillis DESC")
    fun getEntriesSinceFlow(fromMillis: Long): Flow<List<FoodEntry>>

    @Query("SELECT SUM(calories) FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    fun getTotalCaloriesForDay(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(proteinG) FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    fun getTotalProteinForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("SELECT SUM(carbsG) FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    fun getTotalCarbsForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("SELECT SUM(fatG) FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    fun getTotalFatForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("SELECT SUM(fiberG) FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    fun getTotalFiberForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("SELECT * FROM food_entries WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay LIMIT 1")
    suspend fun getEntriesForDaySuspend(startOfDay: Long, endOfDay: Long): List<FoodEntry>

    @Query("SELECT * FROM food_entries ORDER BY dateMillis ASC")
    suspend fun getAllEntriesSuspend(): List<FoodEntry>

    @Update
    suspend fun updateEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodEntry>)
}
