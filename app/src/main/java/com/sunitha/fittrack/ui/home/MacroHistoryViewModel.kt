package com.sunitha.fittrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.datastore.MacroGoals
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.datastore.calculateMacros
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class DayMacros(
    val dateMillis: Long,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val entryCount: Int
)

class MacroHistoryViewModel(
    repo: FitTrackRepository,
    profileStore: UserProfileStore
) : ViewModel() {

    val goals: StateFlow<MacroGoals> = combine(
        profileStore.profile,
        repo.getLatestWeight()
    ) { profile, weight ->
        calculateMacros(profile, weight?.weightKg ?: 0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroGoals())

    val days: StateFlow<List<DayMacros>> = repo
        .getFoodEntriesSinceFlow(System.currentTimeMillis() - 30 * 86_400_000L)
        .map { entries ->
            entries
                .groupBy { startOfDay(it.dateMillis) }
                .map { (dayStart, dayEntries) ->
                    DayMacros(
                        dateMillis = dayStart,
                        calories   = dayEntries.sumOf { it.calories },
                        proteinG   = dayEntries.sumOf { it.proteinG.toDouble() }.toFloat(),
                        carbsG     = dayEntries.sumOf { it.carbsG.toDouble() }.toFloat(),
                        fatG       = dayEntries.sumOf { it.fatG.toDouble() }.toFloat(),
                        entryCount = dayEntries.size
                    )
                }
                .sortedByDescending { it.dateMillis }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class MacroHistoryViewModelFactory(
    private val repo: FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MacroHistoryViewModel(repo, profileStore) as T
}
