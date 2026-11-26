package com.meh.ai.assistant.domain

enum class InferenceMode {
    LOCAL,
    CLOUD,
}

enum class RunPhase {
    IDLE,
    INFERRING,
    WAITING_FOR_USER_APPROVAL,
    COMPLETED,
    FAILED,
    REJECTED_BY_USER,
}

enum class ObservationStatus {
    Pending,
    WaitingApproval,
    Success,
    Error,
    Rejected,
}

enum class AssistantBanner {
    RunStopped,
    MissingApiKey,
    MissingLocalModel,
}

enum class TracePhase {
    Writing,
    AwaitingApproval,
    Done,
    Failed,
    Rejected,
}

data class ReActStep(
    val index: Int,
    val thought: String,
    val action: String?,
    val actionInput: String?,
    val observation: String?,
    val observationStatus: ObservationStatus,
    val requiresApproval: Boolean,
)

sealed interface ChatItem {
    val id: String

    data class UserMessage(
        override val id: String,
        val text: String,
    ) : ChatItem

    data class AgentTurn(
        override val id: String,
        val steps: List<ReActStep>,
        val finalAnswer: String?,
        val traceExpanded: Boolean,
        val mode: InferenceMode,
        val modelLabel: String,
    ) : ChatItem
}

data class PendingApproval(
    val tool: String,
    val summary: String,
    val actionInputJson: String,
    val skillName: String? = null,
)

data class AssistantUiState(
    val agentName: String = "Meh AI",
    val mode: InferenceMode = InferenceMode.LOCAL,
    val localModelLabel: String = "",
    val cloudModelLabel: String = "",
    val hasApiKey: Boolean = false,
    val hasLocalModel: Boolean = false,
    val cloudOffline: Boolean = false,
    val items: List<ChatItem> = emptyList(),
    val runPhase: RunPhase = RunPhase.IDLE,
    val pendingApproval: PendingApproval? = null,
    val approvalDialogVisible: Boolean = false,
    val draft: String = "",
    val banner: AssistantBanner? = null,
    val retryPrompt: String? = null,
) {
    val modelLabel: String
        get() = when (mode) {
            InferenceMode.LOCAL -> localModelLabel
            InferenceMode.CLOUD -> cloudModelLabel
        }
}

val RunPhase.acceptsNewPrompt: Boolean
    get() = when (this) {
        RunPhase.IDLE,
        RunPhase.COMPLETED,
        RunPhase.FAILED,
        RunPhase.REJECTED_BY_USER,
        -> true

        RunPhase.INFERRING,
        RunPhase.WAITING_FOR_USER_APPROVAL,
        -> false
    }

val RunPhase.modeSwitchEnabled: Boolean
    get() = this != RunPhase.INFERRING && this != RunPhase.WAITING_FOR_USER_APPROVAL

fun ChatItem.AgentTurn.tracePhase(isLatest: Boolean, runPhase: RunPhase): TracePhase {
    if (!isLatest) return settledTracePhase()
    return when (runPhase) {
        RunPhase.INFERRING -> TracePhase.Writing
        RunPhase.WAITING_FOR_USER_APPROVAL -> TracePhase.AwaitingApproval
        RunPhase.COMPLETED -> TracePhase.Done
        RunPhase.FAILED -> TracePhase.Failed
        RunPhase.REJECTED_BY_USER -> TracePhase.Rejected
        RunPhase.IDLE -> settledTracePhase()
    }
}

fun ChatItem.AgentTurn.showsTrace(isLatest: Boolean, runPhase: RunPhase): Boolean {
    return steps.isNotEmpty() ||
        (isLatest && finalAnswer.isNullOrBlank() &&
            (runPhase == RunPhase.INFERRING || runPhase == RunPhase.WAITING_FOR_USER_APPROVAL))
}

private fun ChatItem.AgentTurn.settledTracePhase(): TracePhase = when {
    steps.any { it.observationStatus == ObservationStatus.Error } -> TracePhase.Failed
    steps.any { it.observationStatus == ObservationStatus.Rejected } -> TracePhase.Rejected
    steps.any { it.observationStatus == ObservationStatus.WaitingApproval } -> TracePhase.AwaitingApproval
    else -> TracePhase.Done
}
