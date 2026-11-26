package com.meh.ai.assistant.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meh.ai.assistant.domain.AssistantEvent
import com.meh.ai.assistant.domain.AssistantUiState
import com.meh.ai.assistant.domain.InferenceMode
import com.meh.ai.assistant.domain.RunPhase
import com.meh.ai.assistant.domain.modeSwitchEnabled
import com.meh.ai.assistant.presentation.AssistantViewModel

@Composable
fun AssistantScreen(
    onOpenMemory: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssistantContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenMemory = onOpenMemory,
        modifier = modifier,
    )
}

@Composable
internal fun AssistantContent(
    state: AssistantUiState,
    onEvent: (AssistantEvent) -> Unit,
    onOpenMemory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val pending = state.pendingApproval.takeIf {
        state.runPhase == RunPhase.WAITING_FOR_USER_APPROVAL
    }
    LaunchedEffect(state.runPhase) {
        if (state.runPhase == RunPhase.WAITING_FOR_USER_APPROVAL) {
            focusManager.clearFocus()
        }
    }
    BackHandler(enabled = state.approvalDialogVisible) {
        onEvent(AssistantEvent.DismissApprovalDialog)
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            AgentTopBar(
                agentName = state.agentName,
                subtitle = state.headerSubtitle(),
                subtitleIsError = state.mode == InferenceMode.CLOUD && state.cloudOffline,
                mode = state.mode,
                modeEnabled = state.runPhase.modeSwitchEnabled,
                onMode = { onEvent(AssistantEvent.SwitchMode(it)) },
            )
            Box(Modifier.weight(1f)) {
                var overlayHeight by remember { mutableIntStateOf(0) }
                val overlayInset = with(LocalDensity.current) { overlayHeight.toDp() }
                MessageList(
                    items = state.items,
                    runPhase = state.runPhase,
                    onToggleTrace = { onEvent(AssistantEvent.ToggleTrace(it)) },
                    bottomInset = overlayInset,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { overlayHeight = it.height },
                ) {
                    state.banner?.let { banner ->
                        RunBanner(
                            banner = banner,
                            retryPrompt = state.retryPrompt,
                            onRetry = { onEvent(AssistantEvent.SendPrompt(it)) },
                            onOpenMemory = onOpenMemory,
                            onDismiss = { onEvent(AssistantEvent.ConsumeError) },
                        )
                    }
                    if (pending != null) {
                        ApprovalBanner(
                            tool = pending.tool,
                            onReview = { onEvent(AssistantEvent.ShowApprovalDialog) },
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
            PromptField(
                draft = state.draft,
                runPhase = state.runPhase,
                onDraftChange = { onEvent(AssistantEvent.UpdateDraft(it)) },
                onSend = { onEvent(AssistantEvent.SendPrompt(it)) },
                onStop = { onEvent(AssistantEvent.CancelRun) },
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom),
                ),
            )
        }
        if (state.approvalDialogVisible && pending != null) {
            ApprovalDialog(
                pending = pending,
                onApprove = { onEvent(AssistantEvent.ApprovePending) },
                onReject = { onEvent(AssistantEvent.RejectPending) },
            )
        }
    }
}
