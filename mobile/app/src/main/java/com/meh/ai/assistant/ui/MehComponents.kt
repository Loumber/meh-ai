package com.meh.ai.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.meh.ai.ui.theme.MehTheme
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object MehShapes {
    val card = RoundedCornerShape(12.dp)
    val bubble = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(8.dp)
}

@Composable
internal fun AccentChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
internal fun InferenceCaret(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "inference-caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "inference-caret-alpha",
    )
    Box(
        modifier
            .padding(start = 12.dp, top = 2.dp)
            .size(width = 2.dp, height = 14.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}

@Composable
internal fun ActionInputBlock(
    raw: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val scroll = rememberScrollState()
    Text(
        text = formatActionInput(raw),
        style = MehTheme.mono.body,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .clip(MehShapes.control)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(scroll)
            .padding(12.dp)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
    )
}

internal fun formatActionInput(raw: String): String {
    val trimmed = raw.trim()
    return try {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: JSONException) {
        raw
    }
}
