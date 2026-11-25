package com.meh.ai.assistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.meh.ai.assistant.domain.AssistantBanner
import com.meh.ai.assistant.domain.AssistantUiState
import com.meh.ai.assistant.domain.ChatItem
import com.meh.ai.assistant.domain.InferenceMode
import com.meh.ai.assistant.domain.ObservationStatus
import com.meh.ai.assistant.domain.ReActStep
import com.meh.ai.assistant.domain.TracePhase

internal fun InferenceMode.label(): String = when (this) {
    InferenceMode.LOCAL -> "Local"
    InferenceMode.CLOUD -> "Cloud"
}

internal fun AssistantUiState.headerSubtitle(): String = when {
    mode == InferenceMode.CLOUD && cloudOffline -> "нет сети"
    mode == InferenceMode.CLOUD && cloudModelLabel.isBlank() -> "OpenRouter"
    mode == InferenceMode.CLOUD -> "OpenRouter · $cloudModelLabel"
    else -> localModelLabel
}

internal fun AssistantBanner.message(): String = when (this) {
    AssistantBanner.RunStopped -> "Остановлено"
    AssistantBanner.MissingApiKey -> "Сначала сохраните ключ OpenRouter"
    AssistantBanner.MissingLocalModel -> "Выберите GGUF на вкладке Память"
}

internal fun TracePhase.label(): String = when (this) {
    TracePhase.Writing -> "идёт вывод"
    TracePhase.AwaitingApproval -> "ожидает подтверждения"
    TracePhase.Done -> "готово"
    TracePhase.Failed -> "ошибка"
    TracePhase.Rejected -> "отклонено"
}

@Composable
internal fun TracePhase.color(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        TracePhase.AwaitingApproval -> colors.tertiary
        TracePhase.Failed, TracePhase.Rejected -> colors.error
        TracePhase.Writing -> colors.onSurfaceVariant
        TracePhase.Done -> colors.outline
    }
}

@Composable
internal fun ObservationStatus.color(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        ObservationStatus.Pending -> colors.outline
        ObservationStatus.WaitingApproval -> colors.tertiary
        ObservationStatus.Success -> colors.onSurfaceVariant
        ObservationStatus.Error, ObservationStatus.Rejected -> colors.error
    }
}

internal fun ChatItem.AgentTurn.modeCaption(): String? {
    if (modelLabel.isBlank()) return null
    return "${mode.label()} · $modelLabel"
}

internal fun traceTitle(steps: List<ReActStep>, expanded: Boolean): String {
    val count = steps.size
    if (expanded && count > 0) {
        return "Цепочка · шаг ${steps.last().index} из $count"
    }
    return "Цепочка · $count ${stepWord(count)}"
}

internal fun stepWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "шагов"
        mod10 == 1 -> "шаг"
        mod10 in 2..4 -> "шага"
        else -> "шагов"
    }
}
