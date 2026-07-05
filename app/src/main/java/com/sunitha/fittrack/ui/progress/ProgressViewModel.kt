package com.sunitha.fittrack.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayMacros(
    val dateLabel: String,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float
)

class ProgressViewModel(private val repo: FitTrackRepository) : ViewModel() {

    val goalWeight = 65f

    // ── Weight ────────────────────────────────────────────────────────────────

    val weightEntries: StateFlow<List<WeightEntry>> = repo.getWeightEntriesSince(
        System.currentTimeMillis() - 90 * 86_400_000L
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightEntry?> = repo.getLatestWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _newWeightInput = MutableStateFlow("")
    val newWeightInput: StateFlow<String> = _newWeightInput.asStateFlow()

    private val _showLogDialog = MutableStateFlow(false)
    val showLogDialog: StateFlow<Boolean> = _showLogDialog.asStateFlow()

    fun setWeightInput(v: String) { _newWeightInput.value = v }
    fun openLogDialog() { _showLogDialog.value = true }
    fun closeLogDialog() { _showLogDialog.value = false; _newWeightInput.value = "" }

    fun logWeight() {
        val kg = _newWeightInput.value.toFloatOrNull() ?: return
        viewModelScope.launch {
            repo.addWeightEntry(WeightEntry(weightKg = kg))
            closeLogDialog()
        }
    }

    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch { repo.deleteWeightEntry(entry) }
    }

    fun weeklyChange(): Float? {
        val entries = weightEntries.value
        if (entries.size < 2) return null
        val latest = entries.first().weightKg
        val weekAgo = entries.firstOrNull {
            it.dateMillis < System.currentTimeMillis() - 6 * 86_400_000L
        }?.weightKg ?: return null
        return latest - weekAgo
    }

    // ── Macro Trends (7-day) ──────────────────────────────────────────────────

    val macroTrends: StateFlow<List<DayMacros>> = repo.getFoodEntriesSinceFlow(
        System.currentTimeMillis() - 6 * 86_400_000L
    ).map { entries ->
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        (0..6).map { offset ->
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -offset)
            }.timeInMillis
        }.reversed().map { dayStart ->
            val dayEnd = dayStart + 86_400_000L
            val dayEntries = entries.filter { it.dateMillis in dayStart until dayEnd }
            DayMacros(
                dateLabel = sdf.format(Date(dayStart)),
                proteinG  = dayEntries.sumOf { it.proteinG.toDouble() }.toFloat(),
                carbsG    = dayEntries.sumOf { it.carbsG.toDouble() }.toFloat(),
                fatG      = dayEntries.sumOf { it.fatG.toDouble() }.toFloat()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class ProgressViewModelFactory(private val repo: FitTrackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressViewModel(repo) as T
}
