package com.meh.ai.assistant.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meh.ai.assistant.domain.AssistantUiState
import com.meh.ai.assistant.domain.ChatItem
import com.meh.ai.assistant.domain.InferenceMode
import com.meh.ai.assistant.domain.ObservationStatus
import com.meh.ai.assistant.domain.PendingApproval
import com.meh.ai.assistant.domain.ReActStep
import com.meh.ai.assistant.domain.RunPhase
import com.meh.ai.ui.theme.MehAITheme

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1F, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssistantEmptyPreview() {
    MehAITheme {
        AssistantContent(
            state = AssistantUiState(localModelLabel = "Qwen2.5-3B-Q4_K_M.gguf", hasLocalModel = true),
            onEvent = {},
            onOpenMemory = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1F, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssistantInferringPreview() {
    MehAITheme {
        AssistantContent(state = inferringSample(), onEvent = {}, onOpenMemory = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1F, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssistantApprovalPreview() {
    MehAITheme {
        AssistantContent(state = approvalSample(dialogVisible = false), onEvent = {}, onOpenMemory = {})
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1F,
    widthDp = 360,
    heightDp = 780,
    fontScale = 1.6f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AssistantApprovalLargeFontPreview() {
    MehAITheme {
        AssistantContent(state = approvalSample(dialogVisible = true), onEvent = {}, onOpenMemory = {})
    }
}

private fun inferringSample(): AssistantUiState {
    return AssistantUiState(
        localModelLabel = "Qwen2.5-3B-Q4_K_M.gguf",
        hasLocalModel = true,
        runPhase = RunPhase.INFERRING,
        items = listOf(
            ChatItem.UserMessage(id = "1", text = "Сколько сейчас заряда?"),
            ChatItem.AgentTurn(
                id = "2",
                steps = listOf(
                    ReActStep(
                        index = 1,
                        thought = "Заряд можно узнать через get_device_info.",
                        action = null,
                        actionInput = null,
                        observation = null,
                        observationStatus = ObservationStatus.Pending,
                        requiresApproval = false,
                    ),
                ),
                finalAnswer = null,
                traceExpanded = true,
                mode = InferenceMode.LOCAL,
                modelLabel = "Qwen2.5-3B-Q4_K_M.gguf",
            ),
        ),
    )
}

private fun approvalSample(dialogVisible: Boolean): AssistantUiState {
    val input = """{"to":"+79990001122","body":"Я выйду через 5 минут"}"""
    return AssistantUiState(
        localModelLabel = "Qwen2.5-3B-Q4_K_M.gguf",
        hasLocalModel = true,
        runPhase = RunPhase.WAITING_FOR_USER_APPROVAL,
        approvalDialogVisible = dialogVisible,
        pendingApproval = PendingApproval(
            tool = "send_sms",
            summary = "SMS на +79990001122: Я выйду через 5 минут",
            actionInputJson = input,
            skillName = "draft_sms.js",
        ),
        items = listOf(
            ChatItem.UserMessage(id = "1", text = "Напиши курьеру, что я выйду через 5 минут"),
            ChatItem.AgentTurn(
                id = "2",
                steps = listOf(
                    ReActStep(
                        index = 1,
                        thought = "Пользователь просит написать курьеру. Номер есть в запросе, текст короткий.",
                        action = "send_sms",
                        actionInput = input,
                        observation = "Вызов остановлен до подтверждения",
                        observationStatus = ObservationStatus.WaitingApproval,
                        requiresApproval = true,
                    ),
                ),
                finalAnswer = null,
                traceExpanded = true,
                mode = InferenceMode.LOCAL,
                modelLabel = "Qwen2.5-3B-Q4_K_M.gguf",
            ),
        ),
    )
}
