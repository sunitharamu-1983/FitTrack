package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.AiInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AiInsight)

    @Query("SELECT * FROM ai_insights ORDER BY generatedAtMillis DESC LIMIT 20")
    fun getRecentInsights(): Flow<List<AiInsight>>

    @Query("SELECT * FROM ai_insights WHERE type = :type ORDER BY generatedAtMillis DESC LIMIT 1")
    suspend fun getLatestByType(type: String): AiInsight?

    @Query("DELETE FROM ai_insights WHERE generatedAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
