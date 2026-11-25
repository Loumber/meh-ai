package com.meh.ai.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.PendingApproval
import com.meh.ai.ui.theme.MehTheme

@Composable
internal fun ApprovalDialog(
    pending: PendingApproval,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(pending.tool) {
        titleFocus.requestFocus()
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    ),
            ) {
                ApprovalDialogCard(
                    pending = pending,
                    titleFocus = titleFocus,
                    onApprove = onApprove,
                    onReject = onReject,
                )
            }
        }
    }
}

@Composable
private fun ApprovalDialogCard(
    pending: PendingApproval,
    titleFocus: FocusRequester,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(MehShapes.card)
            .background(colors.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Подтвердить действие",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            modifier = Modifier
                .focusRequester(titleFocus)
                .focusable()
                .semantics {
                    heading()
                    traversalIndex = 0f
                },
        )
        Row(
            modifier = Modifier.semantics { traversalIndex = 0.4f },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pending.tool,
                style = MehTheme.mono.label,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            AccentChip(text = "критичное", color = colors.tertiary)
        }
        if (!pending.skillName.isNullOrBlank()) {
            val skill = pending.skillName
            Text(
                text = buildAnnotatedString {
                    append("из навыка ")
                    withStyle(SpanStyle(fontFamily = MehTheme.mono.label.fontFamily)) {
                        append(skill)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.semantics { traversalIndex = 0.5f },
            )
        }
        Text(
            text = pending.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.semantics { traversalIndex = 1f },
        )
        Column(Modifier.semantics { traversalIndex = 3f }) {
            Text(
                text = "Аргументы",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Аргументы инструмента" },
            )
            Spacer(Modifier.height(4.dp))
            ActionInputBlock(raw = pending.actionInputJson)
        }
        Row(
            modifier = Modifier.semantics { traversalIndex = 2f },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = MehShapes.control,
                border = BorderStroke(1.dp, colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
            ) {
                Text("Отклонить", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onApprove,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics { contentDescription = "Подтвердить действие" },
                shape = MehShapes.control,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text("Подтвердить", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
