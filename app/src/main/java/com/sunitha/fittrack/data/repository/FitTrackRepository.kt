package com.sunitha.fittrack.data.repository

import com.sunitha.fittrack.ai.ClaudeApiService
import com.sunitha.fittrack.data.db.FitTrackDatabase
import com.sunitha.fittrack.data.db.entities.*
import com.sunitha.fittrack.data.local.IndianFoodData
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FitTrackRepository(db: FitTrackDatabase) {

    private val workoutDao      = db.workoutDao()
    private val foodDao         = db.foodDao()
    private val weightDao       = db.weightDao()
    private val insightDao      = db.aiInsightDao()
    private val stepDao         = db.stepDao()
    private val restDayDao      = db.restDayDao()
    private val aiFoodCacheDao  = db.aiFoodCacheDao()
    private val periodDao       = db.periodDao()

    // ---- Workout ----
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>  = workoutDao.getAllSessions()
    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSession>> = workoutDao.getRecentSessions(limit)
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>> = workoutDao.getSetsForSession(sessionId)
    fun getAllExerciseNames(): Flow<List<String>> = workoutDao.getAllExerciseNames()
    fun getExerciseHistory(name: String): Flow<List<ExerciseSetWithDate>> = workoutDao.getSetsForExerciseFlow(name)
    fun getAllSetsFlow(): Flow<List<WorkoutSet>> = workoutDao.getAllSetsFlow()
    fun getTodaySessions(): Flow<List<WorkoutSession>> {
        val (start, end) = todayRange()
        return workoutDao.getSessionsForDay(start, end)
    }

    suspend fun saveWorkout(session: WorkoutSession, sets: List<WorkoutSet>) {
        val (start, end) = todayRange()
        val existing = workoutDao.getSessionsForDaySuspend(start, end)
            .firstOrNull { it.muscleGroup == session.muscleGroup }
        if (existing != null) {
            val existingCount = workoutDao.getSetCountForSession(existing.id)
            workoutDao.insertSets(sets.map { s ->
                s.copy(sessionId = existing.id, setNumber = existingCount + s.setNumber)
            })
            workoutDao.updateSession(existing.copy(totalSets = existing.totalSets + sets.size))
        } else {
            val sessionId = workoutDao.insertSession(session)
            workoutDao.insertSets(sets.map { it.copy(sessionId = sessionId) })
        }
    }

    suspend fun deleteWorkoutSession(session: WorkoutSession) = workoutDao.deleteSession(session)
    suspend fun deleteWorkoutSessions(ids: List<Long>) = workoutDao.deleteSessionsByIds(ids)
    suspend fun updateSessionsMuscleGroup(ids: List<Long>, name: String) = workoutDao.updateMuscleGroupByIds(ids, name)
    suspend fun updateWorkoutSets(sets: List<WorkoutSet>) = workoutDao.updateSets(sets)
    suspend fun insertWorkoutSets(sets: List<WorkoutSet>) = workoutDao.insertSets(sets)

    suspend fun getWorkoutsSince(fromMillis: Long): List<WorkoutSession> =
        workoutDao.getSessionsSince(fromMillis)

    // ---- Food ----
    fun getFoodEntriesForToday(): Flow<List<FoodEntry>> {
        val (start, end) = todayRange()
        return foodDao.getEntriesForDay(start, end)
    }

    fun getFoodEntriesForDay(dayMillis: Long): Flow<List<FoodEntry>> {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        val end   = start + 86_400_000L
        return foodDao.getEntriesForDay(start, end)
    }

    fun getTodayCalories(): Flow<Int?> {
        val (start, end) = todayRange()
        return foodDao.getTotalCaloriesForDay(start, end)
    }

    fun getTodayProtein(): Flow<Float?> {
        val (start, end) = todayRange()
        return foodDao.getTotalProteinForDay(start, end)
    }

    fun getTodayCarbs(): Flow<Float?> {
        val (start, end) = todayRange()
        return foodDao.getTotalCarbsForDay(start, end)
    }

    fun getTodayFat(): Flow<Float?> {
        val (start, end) = todayRange()
        return foodDao.getTotalFatForDay(start, end)
    }

    fun getTodayFiber(): Flow<Float?> {
        val (start, end) = todayRange()
        return foodDao.getTotalFiberForDay(start, end)
    }

    suspend fun addFoodEntry(entry: FoodEntry)    = foodDao.insertEntry(entry)
    suspend fun deleteFoodEntry(entry: FoodEntry) = foodDao.deleteEntry(entry)
    suspend fun updateFoodEntry(entry: FoodEntry) = foodDao.updateEntry(entry)

    suspend fun getFoodEntriesSince(fromMillis: Long): List<FoodEntry> =
        foodDao.getEntriesSince(fromMillis)

    fun getFoodEntriesSinceFlow(fromMillis: Long): Flow<List<FoodEntry>> =
        foodDao.getEntriesSinceFlow(fromMillis)

    // ---- Weight ----
    fun getAllWeightEntries(): Flow<List<WeightEntry>>  = weightDao.getAllEntries()
    fun getLatestWeight(): Flow<WeightEntry?>           = weightDao.getLatestEntry()
    fun getEarliestWeight(): Flow<WeightEntry?>         = weightDao.getEarliestEntry()
    fun getWeightEntriesSince(fromMillis: Long): Flow<List<WeightEntry>> = weightDao.getEntriesSince(fromMillis)
    fun getRecentWeightEntries(limit: Int): Flow<List<WeightEntry>> = weightDao.getRecentEntries(limit)

    suspend fun addWeightEntry(entry: WeightEntry) = weightDao.insertEntry(entry)
    suspend fun deleteWeightEntry(entry: WeightEntry) = weightDao.deleteEntry(entry)

    // ---- AI Insights ----
    fun getRecentInsights(): Flow<List<AiInsight>> = insightDao.getRecentInsights()
    suspend fun saveInsight(insight: AiInsight) = insightDao.insertInsight(insight)
    suspend fun getLatestInsightByType(type: String): AiInsight? = insightDao.getLatestByType(type)
    suspend fun pruneOldInsights(beforeMillis: Long) = insightDao.deleteOlderThan(beforeMillis)

    // ---- Historical Import duplicate checks ----
    suspend fun hasWorkoutOrRestForDay(start: Long, end: Long): Boolean =
        workoutDao.getSessionsForDaySuspend(start, end).isNotEmpty() ||
        restDayDao.getRestDayForDaySuspend(start) != null

    suspend fun hasFoodForDay(start: Long, end: Long): Boolean =
        foodDao.getEntriesForDaySuspend(start, end).isNotEmpty()

    suspend fun hasStepsForDay(start: Long): Boolean =
        stepDao.getStepsForDaySuspend(start) != null

    // ---- Backup / Restore ----
    suspend fun getAllSessionsSuspend()  = workoutDao.getAllSessionsSuspend()
    suspend fun getAllSetsSuspend()      = workoutDao.getAllSetsSuspend()
    suspend fun getAllFoodSuspend()      = foodDao.getAllEntriesSuspend()
    suspend fun getAllWeightSuspend()    = weightDao.getAllEntriesSuspend()
    suspend fun getAllRestDaysSuspend()  = restDayDao.getAllRestDaysSuspend()
    suspend fun getAllStepsSuspend()     = stepDao.getAllStepsSuspend()

    suspend fun restoreSessions(list: List<WorkoutSession>) = workoutDao.insertSessions(list)
    suspend fun restoreSets(list: List<WorkoutSet>)         = workoutDao.insertAllSets(list)
    suspend fun restoreFood(list: List<FoodEntry>)          = foodDao.insertAll(list)
    suspend fun restoreWeight(list: List<WeightEntry>)      = weightDao.insertAll(list)
    suspend fun restoreRestDays(list: List<RestDayEntry>)   = restDayDao.insertAll(list)
    suspend fun restoreSteps(list: List<StepEntry>)         = stepDao.insertAll(list)

    // ---- Period Days ----
    fun getTodayPeriodDay(): Flow<PeriodEntry?> = periodDao.getPeriodForDay(todayRange().first)
    fun getRecentPeriodDays(limit: Int): Flow<List<PeriodEntry>> = periodDao.getRecentPeriodDays(limit)

    suspend fun markPeriodDay(note: String = "") {
        val dayMillis = todayRange().first
        periodDao.insert(PeriodEntry(dateMillis = dayMillis, note = note))
    }

    suspend fun removePeriodDay() {
        val dayMillis = todayRange().first
        val existing = periodDao.getPeriodForDaySuspend(dayMillis)
        if (existing != null) periodDao.delete(existing)
    }

    // ---- AI Food Cache ----
    suspend fun getCachedFood(query: String): AiFoodCacheEntry? =
        aiFoodCacheDao.getByKey(query.trim().lowercase())

    suspend fun saveFoodToCache(query: String, result: ClaudeApiService.AiFoodResult) =
        aiFoodCacheDao.insert(
            AiFoodCacheEntry(
                foodNameKey = query.trim().lowercase(),
                name        = result.name,
                serving     = result.serving,
                calories    = result.calories,
                proteinG    = result.proteinG,
                carbsG      = result.carbsG,
                fatG        = result.fatG,
                fiberG      = result.fiberG,
                category    = result.category
            )
        )

    fun getAllCachedFoodsFlow(): Flow<List<AiFoodCacheEntry>> = aiFoodCacheDao.getAllCachedFlow()

    suspend fun syncFoodEntriesToCache() {
        val localNames = IndianFoodData.foods.map { it.name.lowercase() }.toSet()
        val allEntries = foodDao.getAllEntriesSuspend()
        val seen = mutableSetOf<String>()
        for (entry in allEntries) {
            // Skip multi-serving logged entries and simple aggregates
            if (entry.servingDescription.contains("×")) continue
            val key = entry.foodName.trim().lowercase()
            if (key in localNames) continue
            if (!seen.add(key)) continue
            aiFoodCacheDao.insertIgnore(
                AiFoodCacheEntry(
                    foodNameKey = key,
                    name        = entry.foodName.trim(),
                    serving     = entry.servingDescription.trim(),
                    calories    = entry.calories,
                    proteinG    = entry.proteinG,
                    carbsG      = entry.carbsG,
                    fatG        = entry.fatG,
                    category    = "My Foods"
                )
            )
        }
    }

    // ---- Rest Days ----
    fun getTodayRestDay(): Flow<RestDayEntry?> = restDayDao.getRestDayForDay(todayRange().first)
    fun getRecentRestDays(limit: Int): Flow<List<RestDayEntry>> = restDayDao.getRecentRestDays(limit)

    suspend fun markRestDay(note: String = "") {
        val dayMillis = todayRange().first
        val existing = restDayDao.getRestDayForDaySuspend(dayMillis)
        if (existing == null) restDayDao.insert(RestDayEntry(dateMillis = dayMillis, note = note))
    }

    suspend fun removeRestDay() {
        val dayMillis = todayRange().first
        val existing = restDayDao.getRestDayForDaySuspend(dayMillis)
        if (existing != null) restDayDao.delete(existing)
    }

    // ---- Steps ----
    fun getTodaySteps(): Flow<StepEntry?> = stepDao.getStepsForDay(todayRange().first)
    fun getAllStepsFlow(): Flow<List<StepEntry>> = stepDao.getAllStepsFlow()

    suspend fun logSteps(steps: Int) {
        val dayMillis = todayRange().first
        val updated = stepDao.updateStepsForDay(dayMillis, steps)
        if (updated == 0) stepDao.insert(StepEntry(dateMillis = dayMillis, steps = steps))
    }

    // ---- Walk Days ----
    fun getTodayWalkDay(): Flow<StepEntry?> = stepDao.getWalkDayForDay(todayRange().first)
    fun getRecentWalkDays(limit: Int): Flow<List<StepEntry>> = stepDao.getRecentWalkDays(limit)

    suspend fun markWalkDay() {
        val dayMillis = todayRange().first
        val existing = stepDao.getStepsForDaySuspend(dayMillis)
        if (existing != null) stepDao.update(existing.copy(isWalkDay = true))
        else stepDao.insert(StepEntry(dateMillis = dayMillis, steps = 0, isWalkDay = true))
    }

    suspend fun removeWalkDay() {
        val dayMillis = todayRange().first
        val existing = stepDao.getStepsForDaySuspend(dayMillis)
        if (existing != null) stepDao.update(existing.copy(isWalkDay = false))
    }

    // ---- Helpers ----
    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return Pair(start, start + 86_400_000L)
    }
}
