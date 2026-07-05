package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WeightEntry)

    @Delete
    suspend fun deleteEntry(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis DESC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries WHERE dateMillis >= :fromMillis ORDER BY dateMillis ASC")
    fun getEntriesSince(fromMillis: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestEntry(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis ASC LIMIT 1")
    fun getEarliestEntry(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis ASC")
    suspend fun getAllEntriesSuspend(): List<WeightEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntry>)
}
