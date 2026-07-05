package com.sunitha.fittrack.data.db.dao

import androidx.room.*
import com.sunitha.fittrack.data.db.entities.ExerciseSetWithDate
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseName, setNumber")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sessions WHERE dateMillis >= :fromMillis ORDER BY dateMillis DESC")
    suspend fun getSessionsSince(fromMillis: Long): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE dateMillis >= :start AND dateMillis < :end")
    suspend fun getSessionsForDaySuspend(start: Long, end: Long): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE dateMillis >= :start AND dateMillis < :end ORDER BY dateMillis DESC")
    fun getSessionsForDay(start: Long, end: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sets ORDER BY sessionId, exerciseName, setNumber")
    fun getAllSetsFlow(): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    suspend fun getAllSessionsSuspend(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sets ORDER BY sessionId, setNumber")
    suspend fun getAllSetsSuspend(): List<WorkoutSet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSets(sets: List<WorkoutSet>)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Update
    suspend fun updateSets(sets: List<WorkoutSet>)

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT COUNT(*) FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun getSetCountForSession(sessionId: Long): Int

    @Query("DELETE FROM workout_sessions WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<Long>)

    @Query("UPDATE workout_sessions SET muscleGroup = :name WHERE id IN (:ids)")
    suspend fun updateMuscleGroupByIds(ids: List<Long>, name: String)

    @Query("SELECT DISTINCT exerciseName FROM workout_sets ORDER BY exerciseName ASC")
    fun getAllExerciseNames(): Flow<List<String>>

    @Query("""
        SELECT ws.exerciseName, ws.reps, ws.weightKg, wss.dateMillis
        FROM workout_sets ws
        INNER JOIN workout_sessions wss ON ws.sessionId = wss.id
        WHERE ws.exerciseName = :exerciseName
        ORDER BY wss.dateMillis ASC
    """)
    fun getSetsForExerciseFlow(exerciseName: String): Flow<List<ExerciseSetWithDate>>
}
