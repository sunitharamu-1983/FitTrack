package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.StepEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM step_entries WHERE dateMillis = :dayStartMillis LIMIT 1")
    fun getStepsForDay(dayStartMillis: Long): Flow<StepEntry?>

    @Query("SELECT * FROM step_entries WHERE dateMillis = :dayStartMillis LIMIT 1")
    suspend fun getStepsForDaySuspend(dayStartMillis: Long): StepEntry?

    @Query("SELECT * FROM step_entries WHERE dateMillis = :dayStartMillis AND isWalkDay = 1 LIMIT 1")
    fun getWalkDayForDay(dayStartMillis: Long): Flow<StepEntry?>

    @Query("SELECT * FROM step_entries WHERE isWalkDay = 1 ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentWalkDays(limit: Int): Flow<List<StepEntry>>

    @Query("SELECT * FROM step_entries ORDER BY dateMillis ASC")
    suspend fun getAllStepsSuspend(): List<StepEntry>

    @Query("SELECT * FROM step_entries")
    fun getAllStepsFlow(): Flow<List<StepEntry>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: StepEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StepEntry>)

    @Query("UPDATE step_entries SET steps = :steps WHERE dateMillis = :dayStartMillis")
    suspend fun updateStepsForDay(dayStartMillis: Long, steps: Int): Int

    @Update
    suspend fun update(entry: StepEntry)
}
