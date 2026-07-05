package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.PeriodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PeriodEntry)

    @Delete
    suspend fun delete(entry: PeriodEntry)

    @Query("SELECT * FROM period_entries WHERE dateMillis = :dayMillis LIMIT 1")
    fun getPeriodForDay(dayMillis: Long): Flow<PeriodEntry?>

    @Query("SELECT * FROM period_entries WHERE dateMillis = :dayMillis LIMIT 1")
    suspend fun getPeriodForDaySuspend(dayMillis: Long): PeriodEntry?

    @Query("SELECT * FROM period_entries ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentPeriodDays(limit: Int): Flow<List<PeriodEntry>>

    @Query("SELECT * FROM period_entries ORDER BY dateMillis ASC")
    suspend fun getAllPeriodDaysSuspend(): List<PeriodEntry>
}
