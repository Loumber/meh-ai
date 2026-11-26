package com.meh.ai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.ChatItem
import com.meh.ai.assistant.domain.RunPhase
import com.meh.ai.assistant.domain.TracePhase
import com.meh.ai.assistant.domain.showsTrace
import com.meh.ai.assistant.domain.tracePhase
import kotlinx.coroutines.launch

@Composable
internal fun MessageList(
    items: List<ChatItem>,
    runPhase: RunPhase,
    onToggleTrace: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Опишите задачу. Meh AI покажет шаги, прежде чем ответить.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var stickToBottom by remember { mutableStateOf(true) }
    var userMoved by remember { mutableStateOf(false) }
    val tailKey = remember(items, runPhase) { items.tailKey() }
    val latestTurnId = items.lastAgentTurnId()

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) userMoved = true
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.isAtBottom() }
            .collect { (scrolling, atBottom) ->
                if (!scrolling && userMoved) {
                    stickToBottom = atBottom
                    userMoved = false
                }
            }
    }
    LaunchedEffect(tailKey) {
        if (stickToBottom) listState.revealEnd()
    }

    Box(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(items = items, key = { it.id }) { item ->
                when (item) {
                    is ChatItem.UserMessage -> UserBubble(item.text)
                    is ChatItem.AgentTurn -> AgentBubble(
                        turn = item,
                        isLatest = item.id == latestTurnId,
                        runPhase = runPhase,
                        onToggleTrace = { onToggleTrace(item.id) },
                    )
                }
            }
        }
        if (!stickToBottom) {
            val colors = MaterialTheme.colorScheme
            Text(
                text = "К последнему шагу",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp + bottomInset)
                    .heightIn(min = 48.dp)
                    .clip(MehShapes.control)
                    .background(colors.surfaceContainerHigh)
                    .clickable {
                        stickToBottom = true
                        scope.launch { listState.revealEnd() }
                    }
                    .padding(horizontal = 16.dp)
                    .semantics { role = Role.Button },
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.85f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .clip(MehShapes.bubble)
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun AgentBubble(
    turn: ChatItem.AgentTurn,
    isLatest: Boolean,
    runPhase: RunPhase,
    onToggleTrace: () -> Unit,
) {
    val showTrace = turn.showsTrace(isLatest, runPhase)
    if (turn.finalAnswer.isNullOrBlank() && !showTrace) return
    val phase = turn.tracePhase(isLatest, runPhase)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!turn.finalAnswer.isNullOrBlank()) {
            Text(
                text = turn.finalAnswer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showTrace) {
            ReActTrace(
                turn = turn,
                phaseLabel = phase.label(),
                phaseColor = phase.color(),
                writing = phase == TracePhase.Writing,
                onToggle = onToggleTrace,
            )
        }
    }
}

private fun List<ChatItem>.lastAgentTurnId(): String? {
    return lastOrNull { it is ChatItem.AgentTurn }?.id
}

private fun List<ChatItem>.tailKey(): String {
    val last = lastOrNull() ?: return ""
    return when (last) {
        is ChatItem.UserMessage -> "u:${last.id}:${last.text}"
        is ChatItem.AgentTurn -> buildString {
            append("a:")
            append(last.id)
            append(':')
            append(last.traceExpanded)
            append(':')
            append(last.finalAnswer.orEmpty())
            last.steps.forEach { step ->
                append('|')
                append(step.thought)
                append(step.action)
                append(step.actionInput)
                append(step.observation)
                append(step.observationStatus)
            }
        }
    }
}

private fun LazyListState.isAtBottom(): Boolean {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    if (last.index != info.totalItemsCount - 1) return false
    return last.offset + last.size <= info.viewportEndOffset + 1
}

private suspend fun LazyListState.revealEnd() {
    val last = layoutInfo.totalItemsCount - 1
    if (last < 0) return
    animateScrollToItem(last)
    val info = layoutInfo
    val item = info.visibleItemsInfo.lastOrNull() ?: return
    val overflow = item.offset + item.size - info.viewportEndOffset
    if (overflow > 0) animateScrollBy(overflow.toFloat())
}
