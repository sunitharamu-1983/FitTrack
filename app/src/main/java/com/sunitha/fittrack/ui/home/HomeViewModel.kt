package com.sunitha.fittrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.datastore.MacroGoals
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.datastore.calculateMacros
import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.db.entities.PeriodEntry
import com.sunitha.fittrack.data.db.entities.RestDayEntry
import com.sunitha.fittrack.data.db.entities.StepEntry
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.data.db.entities.WorkoutSession
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

class HomeViewModel(
    private val repository:   FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val consumedCalories: StateFlow<Int> = repository.getTodayCalories()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val consumedProtein: StateFlow<Float> = repository.getTodayProtein()
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val consumedCarbs: StateFlow<Float> = repository.getTodayCarbs()
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val consumedFat: StateFlow<Float> = repository.getTodayFat()
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val consumedFiber: StateFlow<Float> = repository.getTodayFiber()
        .map { it ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val latestWeight: StateFlow<Float?> = repository.getLatestWeight()
        .map { it?.weightKg }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastWorkout: StateFlow<WorkoutSession?> = repository.getRecentSessions(1)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentWorkoutSessions: StateFlow<List<WorkoutSession>> = repository.getRecentSessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastRestDay: StateFlow<RestDayEntry?> = repository.getRecentRestDays(1)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastWalkDay: StateFlow<StepEntry?> = repository.getRecentWalkDays(1)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastPeriodDay: StateFlow<PeriodEntry?> = repository.getRecentPeriodDays(1)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workoutStreak: StateFlow<Int> = combine(
        repository.getAllWorkoutSessions(),
        repository.getRecentRestDays(365),
        repository.getRecentWalkDays(365),
        repository.getFoodEntriesSinceFlow(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000),
        repository.getAllWeightEntries()
    ) { sessions, restDays, walkDays, foodEntries, weightEntries ->
        calculateStreak(sessions, restDays, walkDays, foodEntries, weightEntries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySteps: StateFlow<Int> = repository.getTodaySteps()
        .map { it?.steps ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val macros: StateFlow<MacroGoals> = combine(
        profileStore.profile,
        repository.getLatestWeight()
    ) { profile, weightEntry ->
        val weight = weightEntry?.weightKg ?: 0f
        if (profile.isSetUp && weight > 0f) calculateMacros(profile, weight)
        else MacroGoals()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroGoals())

    val userName: StateFlow<String> = profileStore.profile
        .map { it.name.ifBlank { "there" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "there")

    val todayPeriodDay: StateFlow<PeriodEntry?> = repository.getTodayPeriodDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayRestDay: StateFlow<RestDayEntry?> = repository.getTodayRestDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayWalkDay: StateFlow<StepEntry?> = repository.getTodayWalkDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightGoal = 65.0f

    val startWeight: StateFlow<Float> = repository.getEarliestWeight()
        .map { it?.weightKg ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private fun calculateStreak(
        sessions: List<WorkoutSession>,
        restDays: List<RestDayEntry>,
        walkDays: List<StepEntry> = emptyList(),
        foodEntries: List<FoodEntry> = emptyList(),
        weightEntries: List<WeightEntry> = emptyList()
    ): Int {
        val activeDays = buildSet {
            sessions.forEach      { add(startOfDay(it.dateMillis)) }
            restDays.forEach      { add(startOfDay(it.dateMillis)) }
            walkDays.forEach      { add(startOfDay(it.dateMillis)) }
            foodEntries.forEach   { add(startOfDay(it.dateMillis)) }
            weightEntries.forEach { add(startOfDay(it.dateMillis)) }
        }
        if (activeDays.isEmpty()) return 0
        val today = startOfDay(System.currentTimeMillis())
        var current = if (activeDays.contains(today)) today else prevDay(today)
        var streak = 0
        while (activeDays.contains(current)) {
            streak++
            current = prevDay(current)
        }
        return streak
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun prevDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

    fun logSteps(steps: Int)            { viewModelScope.launch { repository.logSteps(steps) } }
    fun markRestDay(note: String = "")  { viewModelScope.launch { repository.markRestDay(note) } }
    fun removeRestDay()                 { viewModelScope.launch { repository.removeRestDay() } }
    fun markWalkDay()                   { viewModelScope.launch { repository.markWalkDay() } }
    fun removeWalkDay()                 { viewModelScope.launch { repository.removeWalkDay() } }
    fun markPeriodDay(note: String = "") { viewModelScope.launch { repository.markPeriodDay(note) } }
    fun removePeriodDay()               { viewModelScope.launch { repository.removePeriodDay() } }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }
}

class HomeViewModelFactory(
    private val repository:   FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository, profileStore) as T
}
