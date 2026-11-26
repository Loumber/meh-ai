package com.meh.ai.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meh.ai.assistant.domain.AssistantEvent
import com.meh.ai.assistant.domain.AssistantReducer
import com.meh.ai.assistant.domain.AssistantUiState
import com.meh.ai.assistant.domain.RunPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel @JvmOverloads constructor(
    private val reducer: AssistantReducer = AssistantReducer(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    private var mockJob: Job? = null
    private var mockGeneration = 0

    fun onEvent(event: AssistantEvent) {
        if (event is AssistantEvent.CancelRun) mockGeneration++
        _uiState.value = reducer.reduce(_uiState.value, event)
        if (event is AssistantEvent.SendPrompt && _uiState.value.runPhase == RunPhase.INFERRING) {
            val generation = ++mockGeneration
            mockJob?.cancel()
            mockJob = viewModelScope.launch {
                delay(MOCK_THINKING_MS)
                if (generation != mockGeneration) return@launch
                _uiState.value = reducer.reduce(_uiState.value, AssistantEvent.FinishRun(MOCK_ANSWER))
            }
        }
    }

    override fun onCleared() {
        mockJob?.cancel()
    }

    private companion object {
        const val MOCK_THINKING_MS = 1_500L
        const val MOCK_ANSWER =
            "Идут три помидора по рельсам и один кричит: Поезд! - шмяк\n- А? - шмяк\n- Чё? - шмяк"
    }
}
