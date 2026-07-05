package com.sunitha.fittrack.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunitha.fittrack.data.datastore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(private val store: UserProfileStore) : ViewModel() {

    var name:          String        by mutableStateOf("")
    var dobMillis:     Long          by mutableLongStateOf(0L)
    var gender:        Gender        by mutableStateOf(Gender.FEMALE)
    var heightCmText:  String        by mutableStateOf("")
    var goal:          FitnessGoal   by mutableStateOf(FitnessGoal.MAINTAIN)
    var activityLevel: ActivityLevel by mutableStateOf(ActivityLevel.MODERATE)

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    // Pre-populate when editing an existing profile
    init {
        viewModelScope.launch {
            val existing = store.profile.first()
            if (existing.isSetUp) {
                name          = existing.name
                dobMillis     = existing.dobMillis
                gender        = existing.gender
                heightCmText  = if (existing.heightCm > 0f) existing.heightCm.toInt().toString() else ""
                goal          = existing.goal
                activityLevel = existing.activityLevel
            }
        }
    }

    val isValid get() = name.isNotBlank() && dobMillis > 0L && heightCmText.toFloatOrNull() != null

    fun save() {
        viewModelScope.launch {
            store.save(
                UserProfile(
                    name          = name.trim(),
                    dobMillis     = dobMillis,
                    gender        = gender,
                    heightCm      = heightCmText.toFloatOrNull() ?: 0f,
                    goal          = goal,
                    activityLevel = activityLevel,
                    isSetUp       = true
                )
            )
            _saved.value = true
        }
    }
}

class OnboardingViewModelFactory(private val store: UserProfileStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(store) as T
}
