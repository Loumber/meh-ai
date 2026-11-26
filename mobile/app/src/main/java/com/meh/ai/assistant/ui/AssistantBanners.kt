package com.meh.ai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.meh.ai.assistant.domain.AssistantBanner
import com.meh.ai.ui.theme.MehTheme

@Composable
internal fun RunBanner(
    banner: AssistantBanner,
    retryPrompt: String?,
    onRetry: (String) -> Unit,
    onOpenMemory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.errorContainer)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = banner.message(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        BannerAction(banner = banner, retryPrompt = retryPrompt, onRetry = onRetry, onOpenMemory = onOpenMemory)
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Закрыть",
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BannerAction(
    banner: AssistantBanner,
    retryPrompt: String?,
    onRetry: (String) -> Unit,
    onOpenMemory: () -> Unit,
) {
    when (banner) {
        AssistantBanner.RunStopped -> {
            if (!retryPrompt.isNullOrBlank()) {
                TextButton(onClick = { onRetry(retryPrompt) }) {
                    Text("Повторить", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        AssistantBanner.MissingApiKey,
        AssistantBanner.MissingLocalModel,
        -> {
            TextButton(onClick = onOpenMemory) {
                Text("Открыть память", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun ApprovalBanner(
    tool: String,
    onReview: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tertiaryContainer)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                append("Ожидает подтверждения · ")
                withStyle(SpanStyle(fontFamily = MehTheme.mono.label.fontFamily)) {
                    append(tool)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onReview) {
            Text("Проверить", color = colors.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}
