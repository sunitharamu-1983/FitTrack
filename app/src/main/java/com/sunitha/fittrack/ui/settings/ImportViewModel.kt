package com.sunitha.fittrack.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.db.entities.*
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val PREFS_FILE = "fittrack_prefs"
private const val PREFS_KEY_IMPORT_DONE = "historical_import_done"
private const val CSV_URL =
    "https://docs.google.com/spreadsheets/d/19U5tmm_VlzWztLfjxdH5c4WXyPyhuAoa2ew81P_PRTM/export?format=csv"

private val REST_DAY_TYPES = setOf(
    "rest", "recovery", "walk", "sick leave", "leave", "rest day"
)

data class ImportPreview(
    val totalCsvRows: Int,
    val skippedInvalid: Int,
    val skippedDuplicates: Int,
    val workoutsToImport: Int,
    val restDaysToImport: Int,
    val cardioToImport: Int,
    val foodToImport: Int,
    val stepsToImport: Int
) {
    val totalToImport get() = workoutsToImport + restDaysToImport + cardioToImport + foodToImport + stepsToImport
}

data class ImportBatch(
    val sessions: List<WorkoutSession>,
    val restDays: List<RestDayEntry>,
    val foodEntries: List<FoodEntry>,
    val stepEntries: List<StepEntry>
)

sealed class ImportState {
    object Idle      : ImportState()
    object Fetching  : ImportState()
    data class Preview(val summary: ImportPreview, val batch: ImportBatch) : ImportState()
    object Importing : ImportState()
    data class Done(val message: String)  : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(private val repo: FitTrackRepository) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun fetchAndPreview() {
        _state.value = ImportState.Fetching
        viewModelScope.launch {
            runCatching {
                val csv = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(CSV_URL).build()
                    http.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                        resp.body!!.string()
                    }
                }
                buildPreviewAndBatch(csv)
            }.onSuccess { (preview, batch) ->
                _state.value = ImportState.Preview(preview, batch)
            }.onFailure { e ->
                _state.value = ImportState.Error("Could not fetch sheet: ${e.message}")
            }
        }
    }

    fun confirmImport(batch: ImportBatch, context: Context) {
        _state.value = ImportState.Importing
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (batch.sessions.isNotEmpty())   repo.restoreSessions(batch.sessions)
                if (batch.restDays.isNotEmpty())   repo.restoreRestDays(batch.restDays)
                if (batch.foodEntries.isNotEmpty()) repo.restoreFood(batch.foodEntries)
                if (batch.stepEntries.isNotEmpty()) repo.restoreSteps(batch.stepEntries)

                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                    .edit().putBoolean(PREFS_KEY_IMPORT_DONE, true).apply()

                val total = batch.sessions.size + batch.restDays.size +
                            batch.foodEntries.size + batch.stepEntries.size
                "Imported $total records — ${batch.sessions.size} workouts, " +
                "${batch.restDays.size} rest days, ${batch.foodEntries.size} food entries, " +
                "${batch.stepEntries.size} step logs"
            }.onSuccess { msg ->
                _state.value = ImportState.Done(msg)
            }.onFailure { e ->
                _state.value = ImportState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun reset() { _state.value = ImportState.Idle }

    // ── CSV parsing + dedup ───────────────────────────────────────────────────

    private suspend fun buildPreviewAndBatch(csv: String): Pair<ImportPreview, ImportBatch> {
        val lines = csv.lines()
        if (lines.size < 2) throw Exception("Sheet appears empty")

        val dateFmt = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).apply { isLenient = false }

        val sessions   = mutableListOf<WorkoutSession>()
        val restDays   = mutableListOf<RestDayEntry>()
        val foodList   = mutableListOf<FoodEntry>()
        val stepList   = mutableListOf<StepEntry>()

        var skippedInvalid    = 0
        var skippedDuplicates = 0
        var totalRows         = 0

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            totalRows++
            val cols = parseCsvLine(line)

            val dateStr      = cols.getOrNull(0)?.trim() ?: ""
            val workoutType  = cols.getOrNull(2)?.trim() ?: ""
            val caloriesStr  = cols.getOrNull(3)?.trim() ?: ""
            val proteinStr   = cols.getOrNull(4)?.trim() ?: ""
            val cardioType   = cols.getOrNull(5)?.trim() ?: ""
            val cardioTimeStr = cols.getOrNull(6)?.trim() ?: ""
            val stepsStr     = cols.getOrNull(7)?.trim() ?: ""

            if (dateStr.isBlank() || workoutType.isBlank()) { skippedInvalid++; continue }

            val date = runCatching { dateFmt.parse(dateStr) }.getOrNull()
            if (date == null) { skippedInvalid++; continue }

            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            val dayEnd   = dayStart + 86_400_000L

            val isRest = workoutType.lowercase().trim() in REST_DAY_TYPES

            // ── Workout / Rest day ────────────────────────────────────────
            val alreadyHasActivity = repo.hasWorkoutOrRestForDay(dayStart, dayEnd)
            if (alreadyHasActivity) {
                skippedDuplicates++
            } else {
                if (isRest) {
                    restDays.add(RestDayEntry(dateMillis = dayStart, note = workoutType))
                } else {
                    sessions.add(WorkoutSession(
                        dateMillis       = dayStart,
                        muscleGroup      = workoutType,
                        durationMinutes  = 0,
                        totalSets        = 0,
                        estimatedCalories = 0
                    ))
                }
            }

            // ── Cardio session (separate, always added if time > 0) ───────
            val cardioMins = cardioTimeStr.toIntOrNull() ?: 0
            if (cardioMins > 0 && !alreadyHasActivity) {
                val cType = cardioType.ifBlank { "Cardio" }
                sessions.add(WorkoutSession(
                    dateMillis        = dayStart + 60_000L,
                    muscleGroup       = cType,
                    durationMinutes   = cardioMins,
                    totalSets         = 1,
                    estimatedCalories = (cardioMins * 6.5f).toInt()
                ))
            }

            // ── Food entry ────────────────────────────────────────────────
            val calories = caloriesStr.toIntOrNull() ?: 0
            val protein  = proteinStr.toFloatOrNull() ?: 0f
            if (calories > 0 && !repo.hasFoodForDay(dayStart, dayEnd)) {
                foodList.add(FoodEntry(
                    dateMillis         = dayStart,
                    mealType           = "Breakfast",
                    foodName           = "Daily Total",
                    servingDescription = "Full day (historical)",
                    calories           = calories,
                    proteinG           = protein,
                    carbsG             = 0f,
                    fatG               = 0f
                ))
            }

            // ── Steps ─────────────────────────────────────────────────────
            val steps = stepsStr.toIntOrNull() ?: 0
            if (steps > 0 && !repo.hasStepsForDay(dayStart)) {
                stepList.add(StepEntry(dateMillis = dayStart, steps = steps))
            }
        }

        val cardioCount = sessions.count { it.durationMinutes > 0 }
        val workoutCount = sessions.size - cardioCount

        val preview = ImportPreview(
            totalCsvRows      = totalRows,
            skippedInvalid    = skippedInvalid,
            skippedDuplicates = skippedDuplicates,
            workoutsToImport  = workoutCount,
            restDaysToImport  = restDays.size,
            cardioToImport    = cardioCount,
            foodToImport      = foodList.size,
            stepsToImport     = stepList.size
        )
        val batch = ImportBatch(sessions, restDays, foodList, stepList)
        return Pair(preview, batch)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"'            -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString().trim()); current.clear() }
                else                 -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}

class ImportViewModelFactory(private val repo: FitTrackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ImportViewModel(repo) as T
}

fun isImportDone(context: Context): Boolean =
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .getBoolean(PREFS_KEY_IMPORT_DONE, false)
