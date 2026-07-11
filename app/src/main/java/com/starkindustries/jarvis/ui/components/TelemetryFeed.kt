package com.starkindustries.jarvis.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starkindustries.jarvis.ui.theme.JarvisTheme

data class TelemetryLog(
    val time: String,
    val message: String,
    val type: String // info, warning, error
)

@Composable
fun TelemetryFeed(
    modifier: Modifier = Modifier,
    logs: List<TelemetryLog>
) {
    val themeColors = JarvisTheme.colors
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs are added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .border(1.dp, themeColors.dim, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "TELEMETRY OVERLINK FEED",
            color = themeColors.bright,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                val logColor = when (log.type.toLowerCase()) {
                    "error" -> JarvisTheme.colors.bright // Inherited alert style
                    "warning" -> JarvisTheme.colors.glow
                    else -> themeColors.textSecondary
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "[${log.time}]",
                        color = themeColors.bright.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.message,
                        color = logColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
