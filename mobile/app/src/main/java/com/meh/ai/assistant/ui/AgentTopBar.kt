package com.meh.ai.assistant.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.InferenceMode

@Composable
internal fun AgentTopBar(
    agentName: String,
    subtitle: String,
    subtitleIsError: Boolean,
    mode: InferenceMode,
    modeEnabled: Boolean,
    onMode: (InferenceMode) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text(
                text = agentName,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (subtitleIsError) colors.error else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ModeSwitch(mode = mode, enabled = modeEnabled, onMode = onMode)
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
    }
}

@Composable
private fun ModeSwitch(
    mode: InferenceMode,
    enabled: Boolean,
    onMode: (InferenceMode) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .alpha(if (enabled) 1f else 0.45f)
                .selectableGroup(),
        ) {
            ModeTab(
                label = InferenceMode.LOCAL.label(),
                selected = mode == InferenceMode.LOCAL,
                enabled = enabled,
                onClick = { onMode(InferenceMode.LOCAL) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            ModeTab(
                label = InferenceMode.CLOUD.label(),
                selected = mode == InferenceMode.CLOUD,
                enabled = enabled,
                onClick = { onMode(InferenceMode.CLOUD) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        if (!enabled) {
            Text(
                text = "Дождитесь завершения шага",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val indicator by animateColorAsState(
        targetValue = if (selected) colors.onSurface else colors.outlineVariant.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "mode-indicator",
    )
    Column(
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .padding(start = 28.dp, end = 28.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(indicator),
        )
    }
}
