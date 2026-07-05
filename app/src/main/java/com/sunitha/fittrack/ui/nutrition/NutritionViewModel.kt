package com.sunitha.fittrack.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.ai.ClaudeApiService
import com.sunitha.fittrack.data.datastore.MacroGoals
import com.sunitha.fittrack.data.datastore.UserProfileStore
import com.sunitha.fittrack.data.datastore.calculateMacros
import com.sunitha.fittrack.data.db.entities.AiFoodCacheEntry
import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.local.IndianFood
import com.sunitha.fittrack.data.local.IndianFoodData
import com.sunitha.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class AiFoodState {
    object Idle    : AiFoodState()
    object Loading : AiFoodState()
    data class Found(val result: ClaudeApiService.AiFoodResult) : AiFoodState()
    data class Error(val message: String) : AiFoodState()
}

class NutritionViewModel(
    private val repo: FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModel() {

    private val _selectedDayMillis = MutableStateFlow(todayStartMillis())
    val selectedDayMillis: StateFlow<Long> = _selectedDayMillis.asStateFlow()

    fun selectDay(dayMillis: Long) { _selectedDayMillis.value = dayMillis }
    fun resetToToday() { _selectedDayMillis.value = todayStartMillis() }

    val todayEntries: StateFlow<List<FoodEntry>> = _selectedDayMillis
        .flatMapLatest { repo.getFoodEntriesForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCalories: StateFlow<Int> = todayEntries
        .map { it.sumOf { e -> e.calories } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProtein: StateFlow<Float> = todayEntries
        .map { it.sumOf { e -> e.proteinG.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalCarbs: StateFlow<Float> = todayEntries
        .map { it.sumOf { e -> e.carbsG.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalFat: StateFlow<Float> = todayEntries
        .map { it.sumOf { e -> e.fatG.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalFiber: StateFlow<Float> = todayEntries
        .map { it.sumOf { e -> e.fiberG.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<IndianFood>> = combine(
        _searchQuery,
        repo.getAllCachedFoodsFlow()
    ) { query, cached ->
        val localResults = IndianFoodData.search(query)
        val localNames = localResults.map { it.name.lowercase() }.toSet()
        val cachedResults = if (query.isBlank()) {
            cached.map { it.toIndianFood() }
        } else {
            val q = query.trim().lowercase()
            cached.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
                  .map { it.toIndianFood() }
        }
        val merged = localResults.toMutableList()
        cachedResults.forEach { if (it.name.lowercase() !in localNames) merged.add(it) }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IndianFoodData.foods)

    private val _selectedMealType = MutableStateFlow("Lunch")
    val selectedMealType: StateFlow<String> = _selectedMealType.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _pendingFood = MutableStateFlow<IndianFood?>(null)
    val pendingFood: StateFlow<IndianFood?> = _pendingFood.asStateFlow()

    val macros: StateFlow<MacroGoals> = combine(
        profileStore.profile,
        repo.getLatestWeight()
    ) { profile, weightEntry ->
        val weight = weightEntry?.weightKg ?: 0f
        if (profile.isSetUp && weight > 0f) calculateMacros(profile, weight)
        else MacroGoals()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroGoals())

    val mealTypes = listOf("Pre Breakfast", "Breakfast", "Lunch", "Dinner", "Post Dinner", "Snack")

    private val _aiLookupState = MutableStateFlow<AiFoodState>(AiFoodState.Idle)
    val aiLookupState: StateFlow<AiFoodState> = _aiLookupState.asStateFlow()

    init {
        viewModelScope.launch { repo.syncFoodEntriesToCache() }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q; clearAiLookup() }
    fun setMealType(type: String) { _selectedMealType.value = type }
    fun openAddSheet(food: IndianFood? = null) { _pendingFood.value = food; _showAddSheet.value = true }
    fun closeAddSheet() { _showAddSheet.value = false; _pendingFood.value = null; _searchQuery.value = ""; clearAiLookup() }

    fun lookupFoodWithAi() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        _aiLookupState.value = AiFoodState.Loading
        viewModelScope.launch {
            val cached = repo.getCachedFood(query)
            if (cached != null) {
                _aiLookupState.value = AiFoodState.Found(
                    ClaudeApiService.AiFoodResult(
                        name     = cached.name,
                        serving  = cached.serving,
                        calories = cached.calories,
                        proteinG = cached.proteinG,
                        carbsG   = cached.carbsG,
                        fatG     = cached.fatG,
                        fiberG   = cached.fiberG,
                        category = cached.category
                    )
                )
                return@launch
            }
            ClaudeApiService.getFoodMacros(query)
                .onSuccess { result ->
                    _aiLookupState.value = AiFoodState.Found(result)
                    repo.saveFoodToCache(query, result)
                }
                .onFailure { _aiLookupState.value = AiFoodState.Error("AI unavailable: ${it.message}") }
        }
    }

    fun clearAiLookup() { _aiLookupState.value = AiFoodState.Idle }

    fun addAiFood(result: ClaudeApiService.AiFoodResult) {
        val food = IndianFood(
            name     = result.name,
            serving  = result.serving,
            calories = result.calories,
            proteinG = result.proteinG,
            carbsG   = result.carbsG,
            fatG     = result.fatG,
            fiberG   = result.fiberG,
            category = result.category
        )
        openAddSheet(food)
    }

    fun addFood(food: IndianFood, mealType: String, servings: Float = 1f) {
        viewModelScope.launch {
            repo.addFoodEntry(
                FoodEntry(
                    dateMillis         = _selectedDayMillis.value,
                    mealType           = mealType,
                    foodName           = food.name,
                    servingDescription = if (servings == 1f) food.serving else "$servings × ${food.serving}",
                    calories           = (food.calories * servings).toInt(),
                    proteinG           = food.proteinG * servings,
                    carbsG             = food.carbsG * servings,
                    fatG               = food.fatG * servings,
                    fiberG             = food.fiberG * servings,
                    servings           = servings
                )
            )
        }
        closeAddSheet()
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { repo.deleteFoodEntry(entry) }
    }

    private val _editingEntry = MutableStateFlow<FoodEntry?>(null)
    val editingEntry: StateFlow<FoodEntry?> = _editingEntry.asStateFlow()

    fun startEditing(entry: FoodEntry) { _editingEntry.value = entry }
    fun stopEditing()                  { _editingEntry.value = null }

    fun saveEdit(
        original: FoodEntry,
        foodName: String,
        servingDescription: String,
        mealType: String,
        servings: Float,
        calories: Int,
        proteinG: Float,
        carbsG: Float,
        fatG: Float,
        fiberG: Float = 0f
    ) {
        viewModelScope.launch {
            repo.updateFoodEntry(
                original.copy(
                    foodName           = foodName.trim(),
                    servingDescription = servingDescription.trim(),
                    mealType           = mealType,
                    servings           = servings,
                    calories           = calories,
                    proteinG           = proteinG,
                    carbsG             = carbsG,
                    fatG               = fatG,
                    fiberG             = fiberG
                )
            )
        }
        stopEditing()
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

private fun AiFoodCacheEntry.toIndianFood() = IndianFood(
    name     = name,
    category = category,
    serving  = serving,
    calories = calories,
    proteinG = proteinG,
    carbsG   = carbsG,
    fatG     = fatG,
    fiberG   = fiberG
)

class NutritionViewModelFactory(
    private val repo: FitTrackRepository,
    private val profileStore: UserProfileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NutritionViewModel(repo, profileStore) as T
}
