package com.sunitha.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutHistoryViewModel(private val repo: FitTrackRepository) : ViewModel() {

    val allSets: StateFlow<List<WorkoutSet>> = repo.getAllSetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of day-start-millis → step count, for showing steps on each workout day
    val stepsByDay: StateFlow<Map<Long, Int>> = repo.getAllStepsFlow()
        .map { list -> list.associate { it.dateMillis to it.steps } }
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
}

class WorkoutHistoryViewModelFactory(private val repo: FitTrackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WorkoutHistoryViewModel(repo) as T
}
