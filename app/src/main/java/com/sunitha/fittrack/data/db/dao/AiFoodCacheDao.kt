package com.sunitha.fittrack.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunitha.fittrack.data.db.entities.AiFoodCacheEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AiFoodCacheDao {
    @Query("SELECT * FROM ai_food_cache WHERE foodNameKey = :key LIMIT 1")
    suspend fun getByKey(key: String): AiFoodCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AiFoodCacheEntry)

    @Query("SELECT * FROM ai_food_cache ORDER BY name ASC")
    fun getAllCachedFlow(): Flow<List<AiFoodCacheEntry>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: AiFoodCacheEntry)
}
