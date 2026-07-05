package com.sunitha.fittrack.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.ai.ClaudeApiService
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.datastore.calculateMacros
import com.sunitha.fittrack.data.datastore.toAiContext
import com.sunitha.fittrack.data.datastore.MacroGoals
import com.sunitha.fittrack.data.db.entities.AiInsight
import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.db.entities.PeriodEntry
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AiInsightsViewModel(
    private val repo:         FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModel() {

    val insights: StateFlow<List<AiInsight>> = repo.getRecentInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Rest Day ──────────────────────────────────────────────────────────────

    private val _restDayResult = MutableStateFlow<String?>(null)
    val restDayResult: StateFlow<String?> = _restDayResult.asStateFlow()

    private val _symptoms = MutableStateFlow("")
    val symptoms: StateFlow<String> = _symptoms.asStateFlow()

    fun setSymptoms(v: String) { _symptoms.value = v }

    // ── Action chips & follow-up ──────────────────────────────────────────────

    private val _actionChips = MutableStateFlow<List<String>>(emptyList())
    val actionChips: StateFlow<List<String>> = _actionChips.asStateFlow()

    private val _selectedChip = MutableStateFlow<String?>(null)
    val selectedChip: StateFlow<String?> = _selectedChip.asStateFlow()

    private val _followUpAnswer = MutableStateFlow<String?>(null)
    val followUpAnswer: StateFlow<String?> = _followUpAnswer.asStateFlow()

    private val _isFollowUpLoading = MutableStateFlow(false)
    val isFollowUpLoading: StateFlow<Boolean> = _isFollowUpLoading.asStateFlow()

    fun askFollowUp(question: String) {
        val latestInsight = insights.value.firstOrNull { it.type == "WEEKLY_SUMMARY" } ?: return
        if (_selectedChip.value == question && _followUpAnswer.value != null) return
        _selectedChip.value = question
        _followUpAnswer.value = null
        _isFollowUpLoading.value = true
        viewModelScope.launch {
            val profile = profileStore.profile.first()
            val weight  = repo.getLatestWeight().firstOrNull()
            val ctx     = profile.toAiContext(weight?.weightKg)
            ClaudeApiService.answerFollowUp(question, latestInsight.content, ctx)
                .onSuccess { _followUpAnswer.value = it }
                .onFailure { _error.value = "Could not answer: ${it.message}" }
            _isFollowUpLoading.value = false
        }
    }

    fun clearFollowUp() { _followUpAnswer.value = null; _selectedChip.value = null }

    // ── Weekly summary ────────────────────────────────────────────────────────

    fun generateWeeklySummary() {
        _isGenerating.value = true
        _error.value = null
        _actionChips.value = emptyList()
        _followUpAnswer.value = null
        _selectedChip.value = null
        viewModelScope.launch {
            val now            = System.currentTimeMillis()
            val fourteenDaysAgo = now - 14 * 86_400_000L
            val sevenDaysAgo   = now - 7 * 86_400_000L

            val sessions    = repo.getWorkoutsSince(fourteenDaysAgo)
            val allSets     = repo.getAllSetsFlow().first()
            val sessionIds  = sessions.map { it.id }.toSet()
            val relevantSets = allSets.filter { it.sessionId in sessionIds }

            val foods          = repo.getFoodEntriesSince(sevenDaysAgo)
            val weightEntries  = repo.getRecentWeightEntries(5).first()
            val periodDays     = repo.getRecentPeriodDays(30).first()
            val profile        = profileStore.profile.first()
            val latestWeight   = weightEntries.firstOrNull()
            val macroGoals     = calculateMacros(profile, latestWeight?.weightKg ?: 0f)

            ClaudeApiService.getWeeklyInsights(
                userName       = profile.name.ifBlank { "there" },
                workoutSection = buildWorkoutSection(sessions, relevantSets),
                macroSection   = buildMacroSection(foods, macroGoals),
                weightSection  = buildWeightSection(weightEntries),
                periodSection  = buildPeriodSection(periodDays)
            ).onSuccess { content ->
                repo.saveInsight(AiInsight(type = "WEEKLY_SUMMARY", content = content))
                ClaudeApiService.getActionChips(content)
                    .onSuccess { _actionChips.value = it }
            }.onFailure {
                _error.value = "Could not generate insights: ${it.message}"
            }
            _isGenerating.value = false
        }
    }

    private fun buildWorkoutSection(sessions: List<WorkoutSession>, sets: List<WorkoutSet>): String {
        if (sessions.isEmpty()) return ""
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val sessionMap = sessions.associateBy { it.id }

        val sessionLines = sessions.sortedByDescending { it.dateMillis }.joinToString("\n") { s ->
            val date = sdf.format(Date(s.dateMillis))
            val sessionSets = sets.filter { it.sessionId == s.id }
            val exercises = sessionSets.groupBy { it.exerciseName }
                .entries.joinToString(", ") { (name, exSets) ->
                    val maxWt = exSets.maxOf { it.weightKg }
                    val setCount = exSets.map { it.setNumber }.distinct().size.coerceAtLeast(1)
                    val avgReps = exSets.groupBy { it.setNumber }
                        .values.mapNotNull { it.firstOrNull()?.reps }
                        .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
                    if (maxWt > 0f) "$name ${setCount}x${avgReps}@${maxWt.toInt()}kg"
                    else "$name ${setCount}x${avgReps}"
                }.ifBlank { s.muscleGroup }
            "$date: $exercises"
        }

        val overloadLines = sets.groupBy { it.exerciseName }.mapNotNull { (name, exSets) ->
            val bySession = exSets.groupBy { it.sessionId }
                .mapNotNull { (sid, sSets) ->
                    val session = sessionMap[sid] ?: return@mapNotNull null
                    session.dateMillis to sSets.maxOf { it.weightKg }
                }.sortedBy { it.first }
            if (bySession.size < 2) return@mapNotNull null
            val first = bySession.first().second
            val last  = bySession.last().second
            val diff  = last - first
            val arrow = when {
                diff > 1.9f  -> "↑${diff.toInt()}kg"
                diff < -1.9f -> "↓${(-diff).toInt()}kg"
                else         -> "plateau"
            }
            val pts = bySession.joinToString("→") { (_, wt) -> "${wt.toInt()}kg" }
            "$name: $pts ($arrow)"
        }

        return buildString {
            append("WORKOUTS (last 14 days):\n")
            append(sessionLines)
            if (overloadLines.isNotEmpty()) {
                append("\n\nPROGRESSIVE OVERLOAD:\n")
                append(overloadLines.joinToString("\n"))
            }
        }
    }

    private fun buildMacroSection(foods: List<FoodEntry>, goals: MacroGoals): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val days = (6 downTo 0).map { offset ->
            val dayStart = todayStart - offset * 86_400_000L
            val dayEnd   = dayStart + 86_400_000L
            val dayFoods = foods.filter { it.dateMillis in dayStart until dayEnd }
            val label    = sdf.format(Date(dayStart))
            if (dayFoods.isEmpty()) "$label: -"
            else {
                val cal  = dayFoods.sumOf { it.calories }
                val pro  = dayFoods.sumOf { it.proteinG.toDouble() }.toInt()
                val carb = dayFoods.sumOf { it.carbsG.toDouble() }.toInt()
                val fat  = dayFoods.sumOf { it.fatG.toDouble() }.toInt()
                "$label: ${cal}cal|${pro}gP|${carb}gC|${fat}gF"
            }
        }

        val goalLine = if (goals.calories > 0)
            "goal: ${goals.calories}cal|${goals.proteinG.toInt()}gP|${goals.carbsG.toInt()}gC|${goals.fatG.toInt()}gF"
        else "no goal set"

        return "NUTRITION ($goalLine):\n${days.joinToString("\n")}"
    }

    private fun buildWeightSection(entries: List<WeightEntry>): String {
        if (entries.isEmpty()) return ""
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val pts = entries.sortedByDescending { it.dateMillis }.take(5)
            .joinToString(" → ") { "${it.weightKg}kg(${sdf.format(Date(it.dateMillis))})" }
        return "WEIGHT (newest→oldest): $pts"
    }

    private fun buildPeriodSection(periods: List<PeriodEntry>): String {
        if (periods.isEmpty()) return ""
        val sdf    = SimpleDateFormat("MMM d", Locale.getDefault())
        val sorted = periods.sortedByDescending { it.dateMillis }.take(14)
        val dates  = sorted.joinToString(", ") { sdf.format(Date(it.dateMillis)) }
        return "PERIOD DAYS TRACKED (${sorted.size} days): $dates"
    }

    // ── Rest day ──────────────────────────────────────────────────────────────

    fun generateRestDayAdvice() {
        _isGenerating.value = true
        _error.value = null
        viewModelScope.launch {
            val weekAgo     = System.currentTimeMillis() - 7 * 86_400_000L
            val workouts    = repo.getWorkoutsSince(weekAgo)
            val weight      = repo.getLatestWeight().firstOrNull()
            val profile     = profileStore.profile.first()
            val periodToday = repo.getTodayPeriodDay().first() != null

            val userContext = profile.toAiContext(weight?.weightKg, isPeriodDay = periodToday)

            val recentStr = workouts.take(3).joinToString(", ") {
                val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                "${sdf.format(Date(it.dateMillis))} ${it.muscleGroup}"
            }.ifBlank { "None this week" }

            val workedDays = workouts.map { startOfDay(it.dateMillis) }.toSet()
            var streak = 0
            var day = startOfDay(System.currentTimeMillis())
            while (workedDays.contains(day)) { streak++; day = prevDay(day) }

            ClaudeApiService.getRestDayAdvice(
                recentWorkouts = recentStr,
                daysSinceRest  = streak,
                symptoms       = _symptoms.value,
                userContext    = userContext
            ).onSuccess { content ->
                _restDayResult.value = content
                repo.saveInsight(AiInsight(type = "REST_DAY", content = content))
            }.onFailure {
                _error.value = "Could not get rest day advice: ${it.message}"
            }
            _isGenerating.value = false
        }
    }

    fun clearError() { _error.value = null }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun prevDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
}

class AiInsightsViewModelFactory(
    private val repo:         FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AiInsightsViewModel(repo, profileStore) as T
}
