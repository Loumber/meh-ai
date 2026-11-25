package com.meh.ai.assistant.domain

sealed interface AssistantEvent {
    data class UpdateDraft(val text: String) : AssistantEvent
    data class SendPrompt(val text: String) : AssistantEvent
    data object CancelRun : AssistantEvent
    data class ToggleTrace(val turnId: String) : AssistantEvent
    data object ApprovePending : AssistantEvent
    data object RejectPending : AssistantEvent
    data object DismissApprovalDialog : AssistantEvent
    data object ShowApprovalDialog : AssistantEvent
    data class SwitchMode(val mode: InferenceMode) : AssistantEvent
    data object ConsumeError : AssistantEvent
    data class FinishRun(val answer: String) : AssistantEvent
}
