package com.example.weighttrackerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weighttrackerapp.data.Goal
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.domain.Outcome
import com.example.weighttrackerapp.domain.WeightService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Holds what the main screen shows and runs all database work off the main thread.

class WeightViewModel(
    private val userId: Long,
    private val service: WeightService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    data class UiState(val entries: List<WeightEntry> = emptyList(), val goal: Goal? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // One-time messages (toasts), as opposed to state that should survive rotation.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun addEntry(date: String, weight: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { service.addEntry(userId, date, weight) }) {
                is Outcome.Failure -> _messages.tryEmit(result.message)
                is Outcome.Success -> {
                    val added = result.value
                    _messages.tryEmit(
                        when {
                            added.goalReached && added.notificationSent -> "Goal reached! Text message sent."
                            added.goalReached -> "Goal reached! Add a phone number and allow SMS to get a text."
                            else -> "Added"
                        }
                    )
                    reload()
                }
            }
        }
    }

    fun updateEntry(entryId: Long, date: String, weight: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { service.updateEntry(userId, entryId, date, weight) }) {
                is Outcome.Failure -> _messages.tryEmit(result.message)
                is Outcome.Success -> {
                    _messages.tryEmit("Updated")
                    reload()
                }
            }
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { service.deleteEntry(userId, entryId) }) {
                is Outcome.Failure -> _messages.tryEmit(result.message)
                is Outcome.Success -> {
                    _messages.tryEmit("Deleted")
                    reload()
                }
            }
        }
    }

    fun saveGoal(goalText: String, phoneText: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { service.saveGoal(userId, goalText, phoneText) }) {
                is Outcome.Failure -> _messages.tryEmit(result.message)
                is Outcome.Success -> {
                    _messages.tryEmit("Goal saved")
                    reload()
                }
            }
        }
    }

    private suspend fun reload() {
        val newState = withContext(ioDispatcher) {
            UiState(service.listEntries(userId), service.getGoal(userId))
        }
        _state.value = newState
    }
}

class WeightViewModelFactory(
    private val userId: Long,
    private val service: WeightService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeightViewModel::class.java)) { "Unknown ViewModel: $modelClass" }
        return WeightViewModel(userId, service) as T
    }
}