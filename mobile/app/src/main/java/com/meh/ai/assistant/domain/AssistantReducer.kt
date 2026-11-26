package com.meh.ai.assistant.domain

/**
 * Применяет события экрана к состоянию.
 * Инференс, инструменты и сеть сюда не входят: редьюсер только меняет ленту и фазу.
 */
class AssistantReducer {
    private var nextId = 1L

    fun reduce(state: AssistantUiState, event: AssistantEvent): AssistantUiState {
        return when (event) {
            is AssistantEvent.UpdateDraft -> updateDraft(state, event.text)
            is AssistantEvent.SendPrompt -> sendPrompt(state, event.text)
            AssistantEvent.CancelRun -> cancelRun(state)
            is AssistantEvent.ToggleTrace -> toggleTrace(state, event.turnId)
            AssistantEvent.ApprovePending -> approvePending(state)
            AssistantEvent.RejectPending -> rejectPending(state)
            AssistantEvent.DismissApprovalDialog -> dismissApprovalDialog(state)
            AssistantEvent.ShowApprovalDialog -> showApprovalDialog(state)
            is AssistantEvent.SwitchMode -> switchMode(state, event.mode)
            AssistantEvent.ConsumeError -> consumeError(state)
            is AssistantEvent.FinishRun -> finishRun(state, event.answer)
        }
    }

    private fun updateDraft(state: AssistantUiState, text: String): AssistantUiState {
        if (state.runPhase == RunPhase.WAITING_FOR_USER_APPROVAL) return state
        return state.copy(draft = text)
    }

    private fun sendPrompt(state: AssistantUiState, text: String): AssistantUiState {
        val prompt = text.trim()
        if (prompt.isEmpty() || !state.runPhase.acceptsNewPrompt) return state
        val user = ChatItem.UserMessage(id = newId(), text = prompt)
        val turn = ChatItem.AgentTurn(
            id = newId(),
            steps = emptyList(),
            finalAnswer = null,
            traceExpanded = true,
            mode = state.mode,
            modelLabel = state.modelLabel,
        )
        return state.copy(
            items = state.items + user + turn,
            runPhase = RunPhase.INFERRING,
            draft = "",
            banner = null,
            retryPrompt = prompt,
            pendingApproval = null,
            approvalDialogVisible = false,
        )
    }

    private fun cancelRun(state: AssistantUiState): AssistantUiState {
        if (state.runPhase != RunPhase.INFERRING) return state
        return state.updateLatestTurn { turn ->
            val steps = if (turn.steps.isEmpty()) {
                listOf(stoppedStep(index = 1))
            } else {
                turn.steps.mapIndexed { index, step ->
                    if (index == turn.steps.lastIndex) step.observed(StoppedObservation, ObservationStatus.Error) else step
                }
            }
            turn.copy(steps = steps, traceExpanded = true)
        }.copy(
            runPhase = RunPhase.FAILED,
            banner = AssistantBanner.RunStopped,
        )
    }

    private fun toggleTrace(state: AssistantUiState, turnId: String): AssistantUiState {
        return state.copy(
            items = state.items.map { item ->
                if (item is ChatItem.AgentTurn && item.id == turnId) {
                    item.copy(traceExpanded = !item.traceExpanded)
                } else {
                    item
                }
            },
        )
    }

    private fun approvePending(state: AssistantUiState): AssistantUiState {
        if (state.pendingApproval == null || state.runPhase != RunPhase.WAITING_FOR_USER_APPROVAL) return state
        return state.updateLatestTurn { turn ->
            turn.copy(
                steps = turn.steps.map { step ->
                    if (step.observationStatus == ObservationStatus.WaitingApproval) {
                        step.observed(text = null, status = ObservationStatus.Pending)
                    } else {
                        step
                    }
                },
                traceExpanded = true,
            )
        }.copy(
            runPhase = RunPhase.INFERRING,
            pendingApproval = null,
            approvalDialogVisible = false,
        )
    }

    private fun rejectPending(state: AssistantUiState): AssistantUiState {
        val pending = state.pendingApproval ?: return state
        if (state.runPhase != RunPhase.WAITING_FOR_USER_APPROVAL) return state
        return state.updateLatestTurn { turn ->
            val steps = if (turn.steps.isEmpty()) {
                listOf(
                    ReActStep(
                        index = 1,
                        thought = "",
                        action = pending.tool,
                        actionInput = pending.actionInputJson,
                        observation = RejectedObservation,
                        observationStatus = ObservationStatus.Rejected,
                        requiresApproval = true,
                    ),
                )
            } else {
                val target = turn.steps.indexOfLast {
                    it.observationStatus == ObservationStatus.WaitingApproval
                }.let { index -> if (index >= 0) index else turn.steps.lastIndex }
                turn.steps.mapIndexed { index, step ->
                    if (index == target) step.observed(RejectedObservation, ObservationStatus.Rejected) else step
                }
            }
            turn.copy(
                steps = steps,
                finalAnswer = RejectedAnswer,
                traceExpanded = false,
            )
        }.copy(
            runPhase = RunPhase.REJECTED_BY_USER,
            pendingApproval = null,
            approvalDialogVisible = false,
        )
    }

    private fun dismissApprovalDialog(state: AssistantUiState): AssistantUiState {
        if (state.runPhase != RunPhase.WAITING_FOR_USER_APPROVAL) return state
        return state.copy(approvalDialogVisible = false)
    }

    private fun showApprovalDialog(state: AssistantUiState): AssistantUiState {
        if (state.pendingApproval == null || state.runPhase != RunPhase.WAITING_FOR_USER_APPROVAL) return state
        return state.copy(approvalDialogVisible = true)
    }

    private fun switchMode(state: AssistantUiState, mode: InferenceMode): AssistantUiState {
        if (!state.runPhase.modeSwitchEnabled || state.mode == mode) return state
        return when (mode) {
            InferenceMode.CLOUD -> if (state.hasApiKey) {
                state.copy(mode = mode, banner = null)
            } else {
                state.copy(banner = AssistantBanner.MissingApiKey)
            }

            InferenceMode.LOCAL -> if (state.hasLocalModel) {
                state.copy(mode = mode, banner = null)
            } else {
                state.copy(banner = AssistantBanner.MissingLocalModel)
            }
        }
    }

    private fun finishRun(state: AssistantUiState, answer: String): AssistantUiState {
        if (state.runPhase != RunPhase.INFERRING) return state
        return state.updateLatestTurn { turn ->
            turn.copy(finalAnswer = answer, traceExpanded = false)
        }.copy(runPhase = RunPhase.COMPLETED)
    }

    private fun consumeError(state: AssistantUiState): AssistantUiState {
        val phase = if (state.runPhase == RunPhase.FAILED) RunPhase.IDLE else state.runPhase
        return state.copy(banner = null, runPhase = phase)
    }

    private fun newId(): String = (nextId++).toString()

    private fun stoppedStep(index: Int) = ReActStep(
        index = index,
        thought = "",
        action = null,
        actionInput = null,
        observation = StoppedObservation,
        observationStatus = ObservationStatus.Error,
        requiresApproval = false,
    )

    private companion object {
        const val StoppedObservation = "Остановлено"
        const val RejectedObservation = "Пользователь отклонил вызов"
        const val RejectedAnswer = "Остановился: действие отклонено"
    }
}

private fun ReActStep.observed(text: String?, status: ObservationStatus): ReActStep {
    return copy(observation = text, observationStatus = status)
}

private fun AssistantUiState.updateLatestTurn(
    transform: (ChatItem.AgentTurn) -> ChatItem.AgentTurn,
): AssistantUiState {
    val index = items.indexOfLast { it is ChatItem.AgentTurn }
    if (index < 0) return this
    val updated = items.toMutableList()
    updated[index] = transform(items[index] as ChatItem.AgentTurn)
    return copy(items = updated)
}
