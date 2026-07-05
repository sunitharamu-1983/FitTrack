package com.sunitha.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.ai.ClaudeApiService
import com.sunitha.fittrack.data.db.entities.AiInsight
import com.sunitha.fittrack.data.db.entities.PeriodEntry
import com.sunitha.fittrack.data.db.entities.RestDayEntry
import com.sunitha.fittrack.data.db.entities.StepEntry
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import com.sunitha.fittrack.data.local.ExerciseData
import com.sunitha.fittrack.data.local.ExerciseTemplate
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// ── History item (workout or rest day) ──────────────────────────────────────
sealed class HistoryItem {
    data class Workout(
        val session: WorkoutSession,
        val allSessionIds: List<Long>,
        val allSessions: List<WorkoutSession> = emptyList()
    ) : HistoryItem()
    data class RestDay(val entry: RestDayEntry)      : HistoryItem()
    data class PeriodDay(val entry: PeriodEntry)     : HistoryItem()
    data class WalkDay(val entry: StepEntry)         : HistoryItem()
}

internal fun dayKey(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
}

// ── Set log (in-memory while logging) ───────────────────────────────────────
data class SetLog(
    val setNumber: Int,
    var reps: String = "",
    var weightKg: String = ""
)

// ── Exercise being logged ────────────────────────────────────────────────────
data class ActiveExercise(
    val template: ExerciseTemplate,
    val sets: MutableList<SetLog> = mutableListOf(SetLog(1))
)

enum class WorkoutStep { HISTORY, SELECT_MUSCLE, SELECT_EXERCISES, LOG_WORKOUT }

class WorkoutViewModel(private val repo: FitTrackRepository) : ViewModel() {

    val sessions = repo.getRecentSessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<WorkoutSession>> = repo.getTodaySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSets: StateFlow<List<WorkoutSet>> = repo.getAllSetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stepsByDay: StateFlow<Map<Long, Int>> = repo.getAllStepsFlow()
        .map { list ->
            list.associate { entry ->
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = entry.dateMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0);      set(java.util.Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis to entry.steps
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val historyItems: StateFlow<List<HistoryItem>> = combine(
        repo.getAllWorkoutSessions(),
        repo.getRecentRestDays(365),
        repo.getRecentPeriodDays(365),
        repo.getRecentWalkDays(365)
    ) { sessions, restDays, periodDays, walkDays ->
        val items = mutableListOf<HistoryItem>()
        sessions
            .groupBy { dayKey(it.dateMillis) }
            .values
            .forEach { group ->
                val muscleGroups = group.map { it.muscleGroup }.distinct()
                val rep = group.minByOrNull { it.dateMillis } ?: group.first()
                items.add(HistoryItem.Workout(
                    session       = rep.copy(
                        muscleGroup = muscleGroups.joinToString(" · "),
                        totalSets   = group.sumOf { it.totalSets }
                    ),
                    allSessionIds = group.map { it.id },
                    allSessions   = group
                ))
            }
        restDays.forEach   { items.add(HistoryItem.RestDay(it)) }
        periodDays.forEach { items.add(HistoryItem.PeriodDay(it)) }
        walkDays.forEach   { items.add(HistoryItem.WalkDay(it)) }
        items.sortedByDescending {
            when (it) {
                is HistoryItem.Workout   -> it.session.dateMillis
                is HistoryItem.RestDay   -> it.entry.dateMillis
                is HistoryItem.PeriodDay -> it.entry.dateMillis
                is HistoryItem.WalkDay   -> it.entry.dateMillis
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastDayItems: StateFlow<List<HistoryItem>> = historyItems
        .map { items ->
            if (items.isEmpty()) return@map emptyList()
            val targetKey = dayKey(when (val first = items.first()) {
                is HistoryItem.Workout   -> first.session.dateMillis
                is HistoryItem.RestDay   -> first.entry.dateMillis
                is HistoryItem.PeriodDay -> first.entry.dateMillis
                is HistoryItem.WalkDay   -> first.entry.dateMillis
            })
            items.filter { item ->
                dayKey(when (item) {
                    is HistoryItem.Workout   -> item.session.dateMillis
                    is HistoryItem.RestDay   -> item.entry.dateMillis
                    is HistoryItem.PeriodDay -> item.entry.dateMillis
                    is HistoryItem.WalkDay   -> item.entry.dateMillis
                }) == targetKey
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayRestDay: StateFlow<RestDayEntry?> = repo.getTodayRestDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayPeriodDay: StateFlow<PeriodEntry?> = repo.getTodayPeriodDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayWalkDay: StateFlow<StepEntry?> = repo.getTodayWalkDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todaySteps: StateFlow<Int> = repo.getTodaySteps()
        .map { it?.steps ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun logSteps(steps: Int) {
        viewModelScope.launch { repo.logSteps(steps) }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch { repo.deleteWorkoutSession(session) }
    }

    fun deleteSessions(sessionIds: List<Long>) {
        viewModelScope.launch { repo.deleteWorkoutSessions(sessionIds) }
    }

    fun updateSessionsMuscleGroup(sessionIds: List<Long>, name: String) {
        viewModelScope.launch { repo.updateSessionsMuscleGroup(sessionIds, name) }
    }

    fun updateWorkoutSets(sets: List<WorkoutSet>) {
        viewModelScope.launch { repo.updateWorkoutSets(sets) }
    }

    fun upsertWorkoutSets(sets: List<WorkoutSet>) {
        val toUpdate = sets.filter { it.id != 0L }
        val toInsert = sets.filter { it.id == 0L }
        viewModelScope.launch {
            if (toUpdate.isNotEmpty()) repo.updateWorkoutSets(toUpdate)
            if (toInsert.isNotEmpty()) repo.insertWorkoutSets(toInsert)
        }
    }

    fun markRestDay(note: String = "") {
        viewModelScope.launch { repo.markRestDay(note) }
    }

    fun removeRestDay() {
        viewModelScope.launch { repo.removeRestDay() }
    }

    fun markPeriodDay(note: String = "") {
        viewModelScope.launch { repo.markPeriodDay(note) }
    }

    fun removePeriodDay() {
        viewModelScope.launch { repo.removePeriodDay() }
    }

    fun markWalkDay() {
        viewModelScope.launch { repo.markWalkDay() }
    }

    fun removeWalkDay() {
        viewModelScope.launch { repo.removeWalkDay() }
    }

    private val _step = MutableStateFlow(WorkoutStep.HISTORY)
    val step: StateFlow<WorkoutStep> = _step.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup.asStateFlow()

    private val _availableExercises = MutableStateFlow<List<ExerciseTemplate>>(emptyList())
    val availableExercises: StateFlow<List<ExerciseTemplate>> = _availableExercises.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<ExerciseTemplate>>(emptyList())
    val selectedExercises: StateFlow<List<ExerciseTemplate>> = _selectedExercises.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ActiveExercise>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExercise>> = _activeExercises.asStateFlow()

    private val _aiSuggestions = MutableStateFlow<List<ClaudeApiService.AiExercise>>(emptyList())
    val aiSuggestions: StateFlow<List<ClaudeApiService.AiExercise>> = _aiSuggestions.asStateFlow()

    private val _isLoadingAi   = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _aiError       = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _isSaving      = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Navigation ────────────────────────────────────────────────────────────

    fun selectMuscleGroup(group: String) {
        _selectedMuscleGroup.value = group
        _availableExercises.value  = ExerciseData.forMuscleGroup(group)
        _selectedExercises.value   = emptyList()
        _aiSuggestions.value       = emptyList()
        _step.value = WorkoutStep.SELECT_EXERCISES
    }

    fun toggleExercise(template: ExerciseTemplate) {
        val current = _selectedExercises.value.toMutableList()
        if (current.any { it.name == template.name }) current.removeAll { it.name == template.name }
        else current.add(template)
        _selectedExercises.value = current
    }

    fun startWorkout() {
        if (_selectedExercises.value.isEmpty()) return
        _activeExercises.value = _selectedExercises.value.map { t ->
            ActiveExercise(template = t, sets = mutableListOf(SetLog(1, reps = "", weightKg = "")))
        }
        _step.value = WorkoutStep.LOG_WORKOUT
    }

    fun goBack() {
        _step.value = when (_step.value) {
            WorkoutStep.SELECT_EXERCISES -> WorkoutStep.SELECT_MUSCLE
            WorkoutStep.LOG_WORKOUT      -> WorkoutStep.SELECT_EXERCISES
            else                         -> WorkoutStep.HISTORY
        }
    }

    fun showMuscleGroupSelector() {
        _step.value = WorkoutStep.SELECT_MUSCLE
    }

    // ── Set management ────────────────────────────────────────────────────────

    fun addSet(exerciseName: String) {
        _activeExercises.value = _activeExercises.value.map { ae ->
            if (ae.template.name == exerciseName) {
                val newSets = ae.sets.toMutableList().also {
                    it.add(SetLog(it.size + 1))
                }
                ae.copy(sets = newSets)
            } else ae
        }
    }

    fun updateReps(exerciseName: String, setIndex: Int, reps: String) {
        _activeExercises.value = _activeExercises.value.map { ae ->
            if (ae.template.name == exerciseName) {
                val newSets = ae.sets.toMutableList().also {
                    if (setIndex < it.size) it[setIndex] = it[setIndex].copy(reps = reps)
                }
                ae.copy(sets = newSets)
            } else ae
        }
    }

    fun updateWeight(exerciseName: String, setIndex: Int, weight: String) {
        _activeExercises.value = _activeExercises.value.map { ae ->
            if (ae.template.name == exerciseName) {
                val newSets = ae.sets.toMutableList().also {
                    if (setIndex < it.size) it[setIndex] = it[setIndex].copy(weightKg = weight)
                }
                ae.copy(sets = newSets)
            } else ae
        }
    }

    fun removeSet(exerciseName: String, setIndex: Int) {
        _activeExercises.value = _activeExercises.value.map { ae ->
            if (ae.template.name == exerciseName && ae.sets.size > 1) {
                ae.copy(sets = ae.sets.toMutableList().also { it.removeAt(setIndex) })
            } else ae
        }
    }

    // ── AI suggestions ────────────────────────────────────────────────────────

    fun loadAiSuggestions() {
        val group = _selectedMuscleGroup.value ?: return
        _isLoadingAi.value = true
        _aiError.value = null
        viewModelScope.launch {
            val cacheType  = "exercises_$group"
            val oneDayAgo  = System.currentTimeMillis() - 86_400_000L
            val cached     = repo.getLatestInsightByType(cacheType)
            if (cached != null && cached.generatedAtMillis > oneDayAgo) {
                runCatching { ClaudeApiService.deserializeExercises(cached.content) }
                    .onSuccess { exercises ->
                        _aiSuggestions.value = exercises
                        _isLoadingAi.value   = false
                        return@launch
                    }
                // parse failed — fall through to live call
            }
            ClaudeApiService.getExerciseSuggestions(group)
                .onSuccess { exercises ->
                    _aiSuggestions.value = exercises
                    repo.saveInsight(AiInsight(
                        type    = cacheType,
                        content = ClaudeApiService.serializeExercises(exercises)
                    ))
                }
                .onFailure { _aiError.value = "AI unavailable: ${it.message}" }
            _isLoadingAi.value = false
        }
    }

    fun addCustomExercise(name: String, sets: Int, reps: String, weight: String = "") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val template = ExerciseTemplate(
            name            = trimmed,
            muscleGroup     = _selectedMuscleGroup.value ?: "",
            defaultSets     = sets,
            defaultReps     = reps,
            suggestedWeight = weight.trim()
        )
        val current = _selectedExercises.value.toMutableList()
        if (current.none { it.name == template.name }) current.add(template)
        _selectedExercises.value = current
    }

    fun addAiExercise(ai: ClaudeApiService.AiExercise) {
        val template = ExerciseTemplate(
            name         = ai.name,
            muscleGroup  = _selectedMuscleGroup.value ?: "",
            defaultSets  = ai.sets,
            defaultReps  = ai.reps,
            suggestedWeight = ai.weight
        )
        val current = _selectedExercises.value.toMutableList()
        if (current.none { it.name == template.name }) current.add(template)
        _selectedExercises.value = current
    }

    // ── Save workout ──────────────────────────────────────────────────────────

    fun finishWorkout() {
        val active = _activeExercises.value
        if (active.isEmpty()) return
        _isSaving.value = true
        viewModelScope.launch {
            val sets = active.flatMap { ae ->
                ae.sets.mapIndexed { idx, s ->
                    WorkoutSet(
                        sessionId    = 0,
                        exerciseName = ae.template.name,
                        setNumber    = idx + 1,
                        reps         = s.reps.toIntOrNull() ?: 0,
                        weightKg     = s.weightKg.toFloatOrNull() ?: 0f
                    )
                }
            }
            val session = WorkoutSession(
                muscleGroup     = _selectedMuscleGroup.value ?: "",
                durationMinutes = 0,
                totalSets       = sets.size,
                estimatedCalories = 0
            )
            repo.saveWorkout(session, sets)
            _isSaving.value = false
            reset()
        }
    }

    private fun reset() {
        _step.value                = WorkoutStep.HISTORY
        _selectedMuscleGroup.value = null
        _selectedExercises.value   = emptyList()
        _activeExercises.value     = emptyList()
        _aiSuggestions.value       = emptyList()
    }
}

class WorkoutViewModelFactory(private val repo: FitTrackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WorkoutViewModel(repo) as T
}
