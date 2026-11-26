package com.meh.ai.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantReducerTest {
    private val reducer = AssistantReducer()

    @Test
    fun sendPromptStartsAnExpandedTurn() {
        val next = reducer.reduce(AssistantUiState(draft = "черновик"), AssistantEvent.SendPrompt("  задача  "))

        assertEquals(RunPhase.INFERRING, next.runPhase)
        assertEquals("", next.draft)
        assertEquals("задача", next.retryPrompt)
        assertNull(next.banner)
        val user = next.items[0] as ChatItem.UserMessage
        val turn = next.items[1] as ChatItem.AgentTurn
        assertEquals("задача", user.text)
        assertTrue(turn.traceExpanded)
        assertTrue(turn.steps.isEmpty())
        assertEquals(InferenceMode.LOCAL, turn.mode)
    }

    @Test
    fun blankPromptAndBusyPhaseAreIgnored() {
        val idle = reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("   "))
        assertEquals(AssistantUiState(), idle)

        val busy = AssistantUiState(runPhase = RunPhase.INFERRING, draft = "ещё")
        assertEquals(busy, reducer.reduce(busy, AssistantEvent.SendPrompt("ещё")))
    }

    @Test
    fun draftIsFrozenWhileWaiting() {
        val waiting = waitingState(draft = "старый")
        val next = reducer.reduce(waiting, AssistantEvent.UpdateDraft("новый"))
        assertEquals("старый", next.draft)
    }

    @Test
    fun cancelMarksTheStepStopped() {
        val running = reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("задача"))
        val stopped = reducer.reduce(running, AssistantEvent.CancelRun)
        val turn = stopped.items[1] as ChatItem.AgentTurn

        assertEquals(RunPhase.FAILED, stopped.runPhase)
        assertEquals(AssistantBanner.RunStopped, stopped.banner)
        assertEquals("Остановлено", turn.steps.single().observation)
        assertEquals(ObservationStatus.Error, turn.steps.single().observationStatus)
        assertTrue(turn.traceExpanded)
    }

    @Test
    fun dismissKeepsTheRunWaiting() {
        val waiting = waitingState()
        val hidden = reducer.reduce(waiting, AssistantEvent.DismissApprovalDialog)

        assertFalse(hidden.approvalDialogVisible)
        assertEquals(RunPhase.WAITING_FOR_USER_APPROVAL, hidden.runPhase)
        assertEquals(waiting.pendingApproval, hidden.pendingApproval)
    }

    @Test
    fun rejectStopsTheRunWithoutCallingTheTool() {
        val rejected = reducer.reduce(waitingState(), AssistantEvent.RejectPending)
        val turn = rejected.items.single() as ChatItem.AgentTurn

        assertEquals(RunPhase.REJECTED_BY_USER, rejected.runPhase)
        assertNull(rejected.pendingApproval)
        assertFalse(rejected.approvalDialogVisible)
        assertFalse(turn.traceExpanded)
        assertEquals("Остановился: действие отклонено", turn.finalAnswer)
        assertEquals(ObservationStatus.Rejected, turn.steps.single().observationStatus)
        assertEquals("Пользователь отклонил вызов", turn.steps.single().observation)
    }

    @Test
    fun approveReturnsToInferenceAndClearsTheWaitingObservation() {
        val approved = reducer.reduce(waitingState(), AssistantEvent.ApprovePending)
        val step = (approved.items.single() as ChatItem.AgentTurn).steps.single()

        assertEquals(RunPhase.INFERRING, approved.runPhase)
        assertNull(approved.pendingApproval)
        assertFalse(approved.approvalDialogVisible)
        assertNull(step.observation)
        assertEquals(ObservationStatus.Pending, step.observationStatus)
    }

    @Test
    fun cloudSwitchWithoutAKeyStaysLocal() {
        val next = reducer.reduce(AssistantUiState(), AssistantEvent.SwitchMode(InferenceMode.CLOUD))

        assertEquals(InferenceMode.LOCAL, next.mode)
        assertEquals(AssistantBanner.MissingApiKey, next.banner)
    }

    @Test
    fun cloudSwitchWithAKeyClearsTheBanner() {
        val ready = AssistantUiState(hasApiKey = true, cloudModelLabel = "openai/gpt-4.1-mini", banner = AssistantBanner.MissingApiKey)
        val next = reducer.reduce(ready, AssistantEvent.SwitchMode(InferenceMode.CLOUD))

        assertEquals(InferenceMode.CLOUD, next.mode)
        assertNull(next.banner)
    }

    @Test
    fun modeSwitchIsLockedWhileInferring() {
        val running = reducer.reduce(AssistantUiState(hasApiKey = true), AssistantEvent.SendPrompt("задача"))
        val next = reducer.reduce(running, AssistantEvent.SwitchMode(InferenceMode.CLOUD))

        assertEquals(running, next)
    }

    @Test
    fun closingAFailureBannerReturnsToIdle() {
        val stopped = reducer.reduce(
            reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("задача")),
            AssistantEvent.CancelRun,
        )
        val closed = reducer.reduce(stopped, AssistantEvent.ConsumeError)

        assertEquals(RunPhase.IDLE, closed.runPhase)
        assertNull(closed.banner)
    }

    @Test
    fun finishRunWritesTheAnswerAfterThinking() {
        val running = reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("задача"))
        val done = reducer.reduce(running, AssistantEvent.FinishRun("анекдот"))
        val turn = done.items[1] as ChatItem.AgentTurn

        assertEquals(RunPhase.COMPLETED, done.runPhase)
        assertEquals("анекдот", turn.finalAnswer)
        assertFalse(turn.traceExpanded)
    }

    @Test
    fun finishRunIsIgnoredAfterCancel() {
        val stopped = reducer.reduce(
            reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("задача")),
            AssistantEvent.CancelRun,
        )
        val done = reducer.reduce(stopped, AssistantEvent.FinishRun("анекдот"))

        assertEquals(stopped, done)
    }

    @Test
    fun toggleTraceFlipsOnlyTheRequestedTurn() {
        val sent = reducer.reduce(AssistantUiState(), AssistantEvent.SendPrompt("задача"))
        val turn = sent.items[1] as ChatItem.AgentTurn
        val toggled = reducer.reduce(sent, AssistantEvent.ToggleTrace(turn.id))
        val updated = toggled.items[1] as ChatItem.AgentTurn

        assertFalse(updated.traceExpanded)
    }

    private fun waitingState(draft: String = ""): AssistantUiState {
        return AssistantUiState(
            draft = draft,
            runPhase = RunPhase.WAITING_FOR_USER_APPROVAL,
            approvalDialogVisible = true,
            pendingApproval = PendingApproval(tool = "send_sms", summary = "sms", actionInputJson = "{}"),
            items = listOf(
                ChatItem.AgentTurn(
                    id = "turn",
                    steps = listOf(
                        ReActStep(
                            index = 1,
                            thought = "нужно отправить",
                            action = "send_sms",
                            actionInput = "{}",
                            observation = "Вызов остановлен до подтверждения",
                            observationStatus = ObservationStatus.WaitingApproval,
                            requiresApproval = true,
                        ),
                    ),
                    finalAnswer = null,
                    traceExpanded = true,
                    mode = InferenceMode.LOCAL,
                    modelLabel = "model.gguf",
                ),
            ),
        )
    }
}

class TracePhaseTest {
    @Test
    fun latestRunUsesTheLivePhase() {
        val turn = ChatItem.AgentTurn(
            id = "1",
            steps = emptyList(),
            finalAnswer = null,
            traceExpanded = true,
            mode = InferenceMode.LOCAL,
            modelLabel = "",
        )

        assertEquals(TracePhase.Writing, turn.tracePhase(isLatest = true, RunPhase.INFERRING))
        assertEquals(TracePhase.Failed, turn.tracePhase(isLatest = true, RunPhase.FAILED))
    }

    @Test
    fun olderTurnKeepsItsOwnOutcome() {
        val turn = ChatItem.AgentTurn(
            id = "1",
            steps = listOf(
                ReActStep(1, "t", "send_sms", null, "отклонено", ObservationStatus.Rejected, true),
            ),
            finalAnswer = null,
            traceExpanded = false,
            mode = InferenceMode.LOCAL,
            modelLabel = "",
        )

        assertEquals(TracePhase.Rejected, turn.tracePhase(isLatest = false, RunPhase.INFERRING))
        assertTrue(turn.showsTrace(isLatest = false, RunPhase.IDLE))
    }
}
