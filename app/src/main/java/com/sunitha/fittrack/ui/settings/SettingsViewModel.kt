package com.sunitha.fittrack.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.datastore.ThemeMode
import com.sunitha.fittrack.data.datastore.ThemePreferenceStore
import com.sunitha.fittrack.data.db.entities.*
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed class BackupRestoreState {
    object Idle    : BackupRestoreState()
    object Loading : BackupRestoreState()
    data class Success(val message: String) : BackupRestoreState()
    data class Error(val message: String)   : BackupRestoreState()
}

class SettingsViewModel(
    private val repo: FitTrackRepository,
    private val themeStore: ThemePreferenceStore
) : ViewModel() {

    private val _state = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val state: StateFlow<BackupRestoreState> = _state.asStateFlow()

    private val _backupFiles = MutableStateFlow<List<File>>(emptyList())
    val backupFiles: StateFlow<List<File>> = _backupFiles.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themeStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { themeStore.setThemeMode(mode) } }

    fun clearState() { _state.value = BackupRestoreState.Idle }

    private fun backupDir(context: Context): File {
        val dir = context.getExternalFilesDir("Backups") ?: File(context.filesDir, "Backups")
        dir.mkdirs()
        return dir
    }

    fun loadBackupFiles(context: Context) {
        val dir = backupDir(context)
        _backupFiles.value = dir.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteBackupFile(file: File, context: Context) {
        file.delete()
        loadBackupFiles(context)
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    fun backup(context: Context) {
        _state.value = BackupRestoreState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sessions = repo.getAllSessionsSuspend()
                val sets     = repo.getAllSetsSuspend()
                val food     = repo.getAllFoodSuspend()
                val weight   = repo.getAllWeightSuspend()
                val restDays = repo.getAllRestDaysSuspend()
                val steps    = repo.getAllStepsSuspend()

                val json = JSONObject().apply {
                    put("version",    1)
                    put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                    put("workoutSessions", JSONArray(sessions.map { s ->
                        JSONObject().apply {
                            put("id",                s.id)
                            put("muscleGroup",       s.muscleGroup)
                            put("dateMillis",        s.dateMillis)
                            put("durationMinutes",   s.durationMinutes)
                            put("totalSets",         s.totalSets)
                            put("estimatedCalories", s.estimatedCalories)
                            put("notes",             s.notes)
                        }
                    }))
                    put("workoutSets", JSONArray(sets.map { s ->
                        JSONObject().apply {
                            put("id",           s.id)
                            put("sessionId",    s.sessionId)
                            put("exerciseName", s.exerciseName)
                            put("setNumber",    s.setNumber)
                            put("reps",         s.reps)
                            put("weightKg",     s.weightKg)
                            put("isCompleted",  s.isCompleted)
                        }
                    }))
                    put("foodEntries", JSONArray(food.map { f ->
                        JSONObject().apply {
                            put("id",                 f.id)
                            put("dateMillis",         f.dateMillis)
                            put("mealType",           f.mealType)
                            put("foodName",           f.foodName)
                            put("servingDescription", f.servingDescription)
                            put("calories",           f.calories)
                            put("proteinG",           f.proteinG)
                            put("carbsG",             f.carbsG)
                            put("fatG",               f.fatG)
                        }
                    }))
                    put("weightEntries", JSONArray(weight.map { w ->
                        JSONObject().apply {
                            put("id",         w.id)
                            put("dateMillis", w.dateMillis)
                            put("weightKg",   w.weightKg)
                        }
                    }))
                    put("restDays", JSONArray(restDays.map { r ->
                        JSONObject().apply {
                            put("id",         r.id)
                            put("dateMillis", r.dateMillis)
                            put("note",       r.note)
                        }
                    }))
                    put("stepEntries", JSONArray(steps.map { s ->
                        JSONObject().apply {
                            put("id",         s.id)
                            put("dateMillis", s.dateMillis)
                            put("steps",      s.steps)
                        }
                    }))
                }.toString(2)

                val filename = "fittrack_backup_${SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())}.json"
                File(backupDir(context), filename).writeText(json)
                loadBackupFiles(context)

                val total = sessions.size + food.size + weight.size
                "Backup saved ($total records)"
            }.onSuccess { msg ->
                _state.value = BackupRestoreState.Success(msg)
            }.onFailure { e ->
                _state.value = BackupRestoreState.Error("Backup failed: ${e.message}")
            }
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    fun restoreFromFile(file: File) {
        _state.value = BackupRestoreState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                parseAndRestore(file.readText())
            }.onSuccess { msg ->
                _state.value = BackupRestoreState.Success(msg)
            }.onFailure { e ->
                _state.value = BackupRestoreState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun restoreFromUri(uri: android.net.Uri, context: Context) {
        _state.value = BackupRestoreState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: throw Exception("Could not read file")
                parseAndRestore(text)
            }.onSuccess { msg ->
                _state.value = BackupRestoreState.Success(msg)
            }.onFailure { e ->
                _state.value = BackupRestoreState.Error("Restore failed: ${e.message}")
            }
        }
    }

    private suspend fun parseAndRestore(jsonString: String): String {
        val root     = JSONObject(jsonString)
        val sessions = parseSessions(root.optJSONArray("workoutSessions"))
        val sets     = parseSets(root.optJSONArray("workoutSets"))
        val food     = parseFood(root.optJSONArray("foodEntries"))
        val weight   = parseWeight(root.optJSONArray("weightEntries"))
        val restDays = parseRestDays(root.optJSONArray("restDays"))
        val steps    = parseSteps(root.optJSONArray("stepEntries"))

        if (sessions.isNotEmpty()) repo.restoreSessions(sessions)
        if (sets.isNotEmpty())     repo.restoreSets(sets)
        if (food.isNotEmpty())     repo.restoreFood(food)
        if (weight.isNotEmpty())   repo.restoreWeight(weight)
        if (restDays.isNotEmpty()) repo.restoreRestDays(restDays)
        if (steps.isNotEmpty())    repo.restoreSteps(steps)

        return "Restored ${sessions.size} workouts · ${food.size} food entries · ${weight.size} weight entries"
    }

    private fun parseSessions(arr: JSONArray?): List<WorkoutSession> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkoutSession(
                id                = o.getLong("id"),
                muscleGroup       = o.getString("muscleGroup"),
                dateMillis        = o.getLong("dateMillis"),
                durationMinutes   = o.getInt("durationMinutes"),
                totalSets         = o.getInt("totalSets"),
                estimatedCalories = o.getInt("estimatedCalories"),
                notes             = o.optString("notes", "")
            )
        }
    }

    private fun parseSets(arr: JSONArray?): List<WorkoutSet> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkoutSet(
                id           = o.getLong("id"),
                sessionId    = o.getLong("sessionId"),
                exerciseName = o.getString("exerciseName"),
                setNumber    = o.getInt("setNumber"),
                reps         = o.getInt("reps"),
                weightKg     = o.getDouble("weightKg").toFloat(),
                isCompleted  = o.optBoolean("isCompleted", true)
            )
        }
    }

    private fun parseFood(arr: JSONArray?): List<FoodEntry> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FoodEntry(
                id                 = o.getLong("id"),
                dateMillis         = o.getLong("dateMillis"),
                mealType           = o.getString("mealType"),
                foodName           = o.getString("foodName"),
                servingDescription = o.getString("servingDescription"),
                calories           = o.getInt("calories"),
                proteinG           = o.getDouble("proteinG").toFloat(),
                carbsG             = o.getDouble("carbsG").toFloat(),
                fatG               = o.getDouble("fatG").toFloat()
            )
        }
    }

    private fun parseWeight(arr: JSONArray?): List<WeightEntry> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WeightEntry(id = o.getLong("id"), dateMillis = o.getLong("dateMillis"), weightKg = o.getDouble("weightKg").toFloat())
        }
    }

    private fun parseRestDays(arr: JSONArray?): List<RestDayEntry> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RestDayEntry(id = o.getLong("id"), dateMillis = o.getLong("dateMillis"), note = o.optString("note", ""))
        }
    }

    private fun parseSteps(arr: JSONArray?): List<StepEntry> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            StepEntry(id = o.getLong("id"), dateMillis = o.getLong("dateMillis"), steps = o.getInt("steps"))
        }
    }
}

class SettingsViewModelFactory(
    private val repo: FitTrackRepository,
    private val themeStore: ThemePreferenceStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repo, themeStore) as T
}
