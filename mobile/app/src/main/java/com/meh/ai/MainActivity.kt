package com.meh.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.meh.ai.assistant.ui.AssistantScreen
import com.meh.ai.ui.theme.MehAITheme
import com.meh.ai.ui.theme.Night
import com.meh.ai.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val barColor = (if (darkTheme) Night else Paper).toArgb()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(barColor)
                    } else {
                        SystemBarStyle.light(barColor, barColor)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(barColor)
                    } else {
                        SystemBarStyle.light(barColor, barColor)
                    },
                )
            }
            MehAITheme(darkTheme = darkTheme) {
                AssistantScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
