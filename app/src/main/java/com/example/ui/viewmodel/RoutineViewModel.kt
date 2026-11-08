package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Routine
import com.example.data.model.RoutineLog
import com.example.data.repository.RoutineRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RoutinePlayerState(
    val activePlayingRoutine: Routine? = null,
    val playingStepIndex: Int = 0,
    val completedStepsSet: Set<Int> = emptySet(),
    val isTimerRunning: Boolean = false,
    val elapsedSeconds: Int = 0
)

data class RoutineCelebrationState(
    val showCelebration: Boolean = false,
    val celebrationStreak: Int = 0,
    val userMessage: String? = null
)

data class RoutineUiState(
    val routines: List<Routine> = emptyList(),
    val routineLogs: List<RoutineLog> = emptyList(),
    val selectedTimeOfDay: String = "All",
    val activePlayingRoutine: Routine? = null,
    val playingStepIndex: Int = 0,
    val completedStepsSet: Set<Int> = emptySet(),
    val isTimerRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val showCelebrationDialog: Boolean = false,
    val celebrationStreak: Int = 0,
    val userMessage: String? = null
)

class RoutineViewModel(
    private val repository: RoutineRepository,
    private val userId: String
) : ViewModel() {

    private val _selectedTimeOfDay = MutableStateFlow("All")
    private val _activePlayingRoutine = MutableStateFlow<Routine?>(null)
    private val _playingStepIndex = MutableStateFlow(0)
    private val _completedStepsSet = MutableStateFlow<Set<Int>>(emptySet())
    private val _isTimerRunning = MutableStateFlow(false)
    private val _elapsedSeconds = MutableStateFlow(0)
    private val _showCelebration = MutableStateFlow(false)
    private val _celebrationStreak = MutableStateFlow(0)
    private val _userMessage = MutableStateFlow<String?>(null)

    private var timerJob: Job? = null

    private val _playerState = combine(
        _activePlayingRoutine,
        _playingStepIndex,
        _completedStepsSet,
        _isTimerRunning,
        _elapsedSeconds
    ) { activeRoutine, stepIndex, completedSteps, isTimer, elapsed ->
        RoutinePlayerState(activeRoutine, stepIndex, completedSteps, isTimer, elapsed)
    }

    private val _celebrationState = combine(
        _showCelebration,
        _celebrationStreak,
        _userMessage
    ) { showCel, celStreak, message ->
        RoutineCelebrationState(showCel, celStreak, message)
    }

    val uiState: StateFlow<RoutineUiState> = combine(
        repository.observeRoutines().catch { emit(emptyList()) },
        repository.observeRoutineLogs().catch { emit(emptyList()) },
        _selectedTimeOfDay,
        _playerState,
        _celebrationState
    ) { routines: List<Routine>, logs: List<RoutineLog>, timeOfDay: String, player: RoutinePlayerState, cel: RoutineCelebrationState ->
        RoutineUiState(
            routines = if (timeOfDay == "All") routines else routines.filter { it.timeOfDay.equals(timeOfDay, ignoreCase = true) },
            routineLogs = logs,
            selectedTimeOfDay = timeOfDay,
            activePlayingRoutine = player.activePlayingRoutine,
            playingStepIndex = player.playingStepIndex,
            completedStepsSet = player.completedStepsSet,
            isTimerRunning = player.isTimerRunning,
            elapsedSeconds = player.elapsedSeconds,
            showCelebrationDialog = cel.showCelebration,
            celebrationStreak = cel.celebrationStreak,
            userMessage = cel.userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutineUiState()
    )

    fun setTimeOfDayFilter(timeOfDay: String) {
        _selectedTimeOfDay.value = timeOfDay
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun dismissCelebration() {
        _showCelebration.value = false
    }

    fun createRoutine(
        title: String,
        description: String?,
        timeOfDay: String,
        targetTime: String?,
        daysOfWeek: List<String>,
        steps: List<String>
    ) {
        if (title.isBlank() || steps.isEmpty()) return
        viewModelScope.launch {
            val result = repository.createRoutine(
                title = title.trim(),
                description = description?.takeIf { it.isNotBlank() },
                timeOfDay = timeOfDay,
                targetTime = targetTime?.takeIf { it.isNotBlank() },
                daysOfWeek = daysOfWeek,
                steps = steps.filter { it.isNotBlank() }
            )
            if (result.isSuccess) {
                _userMessage.value = "Routine created!"
            } else {
                _userMessage.value = "Failed to create routine"
            }
        }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch {
            val result = repository.updateRoutine(routine)
            if (result.isFailure) {
                _userMessage.value = "Failed to update routine"
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            val result = repository.deleteRoutine(routineId)
            if (result.isSuccess) {
                _userMessage.value = "Routine removed"
            } else {
                _userMessage.value = "Failed to delete routine"
            }
        }
    }

    fun startRoutine(routine: Routine) {
        _activePlayingRoutine.value = routine
        _playingStepIndex.value = 0
        _completedStepsSet.value = emptySet()
        _elapsedSeconds.value = 0
        _isTimerRunning.value = true
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_isTimerRunning.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    fun toggleTimer() {
        _isTimerRunning.value = !_isTimerRunning.value
    }

    fun toggleCurrentStepCompletion(index: Int) {
        val current = _completedStepsSet.value.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        _completedStepsSet.value = current
    }

    fun nextStep() {
        val routine = _activePlayingRoutine.value ?: return
        if (_playingStepIndex.value < routine.steps.size - 1) {
            _playingStepIndex.value += 1
        }
    }

    fun prevStep() {
        if (_playingStepIndex.value > 0) {
            _playingStepIndex.value -= 1
        }
    }

    fun finishRoutine() {
        val routine = _activePlayingRoutine.value ?: return
        timerJob?.cancel()
        _isTimerRunning.value = false

        val stepsDone = _completedStepsSet.value.size
        val totalSteps = routine.steps.size
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val result = repository.completeRoutine(
                routineId = routine.id,
                currentRoutine = routine,
                stepsCompleted = stepsDone,
                totalSteps = totalSteps,
                todayDateString = todayStr
            )
            if (result.isSuccess) {
                val newStreak = if (routine.lastCompletedDate == todayStr) routine.currentStreak else routine.currentStreak + 1
                _celebrationStreak.value = newStreak
                _showCelebration.value = true
                _userMessage.value = "Routine finished! Great job!"
            }
            _activePlayingRoutine.value = null
        }
    }

    fun cancelPlayingRoutine() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _activePlayingRoutine.value = null
    }

    companion object {
        fun provideFactory(
            repository: RoutineRepository,
            userId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RoutineViewModel(repository, userId) as T
            }
        }
    }
}
