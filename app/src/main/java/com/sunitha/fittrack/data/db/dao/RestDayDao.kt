package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.RestDayEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RestDayDao {
    @Query("SELECT * FROM rest_day_entries WHERE dateMillis = :dayStartMillis LIMIT 1")
    fun getRestDayForDay(dayStartMillis: Long): Flow<RestDayEntry?>

    @Query("SELECT * FROM rest_day_entries WHERE dateMillis = :dayStartMillis LIMIT 1")
    suspend fun getRestDayForDaySuspend(dayStartMillis: Long): RestDayEntry?

    @Query("SELECT * FROM rest_day_entries ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentRestDays(limit: Int): Flow<List<RestDayEntry>>

    @Query("SELECT * FROM rest_day_entries ORDER BY dateMillis ASC")
    suspend fun getAllRestDaysSuspend(): List<RestDayEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: RestDayEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RestDayEntry>)

    @Delete
    suspend fun delete(entry: RestDayEntry)
}
