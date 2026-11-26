package com.meh.ai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.RunPhase
import com.meh.ai.assistant.domain.acceptsNewPrompt

@Suppress("DEPRECATION") // Черновик хранится строкой в состоянии экрана, а не в TextFieldState.
@Composable
internal fun PromptField(
    draft: String,
    runPhase: RunPhase,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val readOnly = runPhase == RunPhase.WAITING_FOR_USER_APPROVAL
    val showStop = runPhase == RunPhase.INFERRING
    val showSend = runPhase.acceptsNewPrompt
    val sendEnabled = draft.isNotBlank()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (focused && !readOnly) colors.outline else colors.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.onSurface),
            readOnly = readOnly,
            maxLines = 6,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MehShapes.card)
                        .background(colors.surfaceContainer)
                        .border(1.dp, borderColor, MehShapes.card)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (draft.isEmpty()) {
                        Text(
                            text = "Задача для Meh AI",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
        if (showStop || showSend) {
            Spacer(Modifier.width(8.dp))
            if (showStop) {
                ComposerButton(
                    contentDescription = "Остановить генерацию",
                    onClick = {
                        focusManager.clearFocus()
                        onStop()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                ComposerButton(
                    contentDescription = "Отправить",
                    enabled = sendEnabled,
                    onClick = {
                        focusManager.clearFocus()
                        onSend(draft)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = if (sendEnabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}
