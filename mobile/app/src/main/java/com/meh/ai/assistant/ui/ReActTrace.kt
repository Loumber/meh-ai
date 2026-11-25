package com.meh.ai.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.ChatItem
import com.meh.ai.assistant.domain.ReActStep
import com.meh.ai.ui.theme.MehTheme

@Composable
internal fun ReActTrace(
    turn: ChatItem.AgentTurn,
    phaseLabel: String,
    phaseColor: Color,
    writing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = turn.traceExpanded
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        TraceHeader(
            title = traceTitle(turn.steps, expanded),
            caption = if (expanded) null else turn.modeCaption(),
            phaseLabel = phaseLabel,
            phaseColor = phaseColor,
            expanded = expanded,
            onToggle = onToggle,
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (turn.steps.isEmpty() && writing) {
                    InferenceCaret()
                }
                turn.steps.forEachIndexed { index, step ->
                    ReActStepBlock(
                        step = step,
                        showCaret = writing && index == turn.steps.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun TraceHeader(
    title: String,
    caption: String?,
    phaseLabel: String,
    phaseColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) "развёрнута" else "свёрнута"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.labelMedium,
            color = phaseColor,
        )
    }
}

@Composable
private fun ReActStepBlock(
    step: ReActStep,
    showCaret: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (step.thought.isNotBlank()) {
            StepSection(label = "Thought") {
                Text(
                    text = step.thought,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!step.action.isNullOrBlank()) {
            StepSection(label = "Action") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.action,
                        style = MehTheme.mono.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (step.requiresApproval) {
                        Spacer(Modifier.width(8.dp))
                        AccentChip(text = "подтверждение", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        if (!step.actionInput.isNullOrBlank()) {
            StepSection(label = "action_input") {
                ActionInputBlock(raw = step.actionInput)
            }
        }
        if (!step.observation.isNullOrBlank()) {
            StepSection(label = "Observation") {
                ObservationText(text = step.observation, statusColor = step.observationStatus.color())
            }
        }
        if (showCaret) {
            InferenceCaret()
        }
    }
}

@Composable
private fun StepSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.padding(start = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun ObservationText(
    text: String,
    statusColor: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawLine(
                    color = statusColor,
                    start = Offset(stroke / 2f, 0f),
                    end = Offset(stroke / 2f, size.height),
                    strokeWidth = stroke,
                )
            }
            .padding(start = 12.dp),
    )
}
