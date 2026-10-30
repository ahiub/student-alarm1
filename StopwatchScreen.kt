package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StopwatchViewModel
import kotlinx.coroutines.delay

/** 1:02:03.45 or 02:03.45 */
private fun formatStopwatch(ms: Long): String {
    val totalCenti = ms / 10
    val centi = (totalCenti % 100).toInt()
    val totalSec = totalCenti / 100
    val sec = (totalSec % 60).toInt()
    val min = ((totalSec / 60) % 60).toInt()
    val hours = (totalSec / 3600).toInt()
    return if (hours > 0) {
        "%d:%02d:%02d.%02d".format(hours, min, sec, centi)
    } else {
        "%02d:%02d.%02d".format(min, sec, centi)
    }
}

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val running by viewModel.running.collectAsStateWithLifecycle()
    val frozenMs by viewModel.frozenMs.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()

    // The clock is only read while this screen is visible and the stopwatch is running.
    var displayMs by remember { mutableLongStateOf(viewModel.elapsedNow()) }
    LaunchedEffect(running, frozenMs) {
        displayMs = viewModel.elapsedNow()
        while (running) {
            displayMs = viewModel.elapsedNow()
            delay(30)
        }
    }

    // Keep the screen on while the stopwatch is running.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    // Best / worst lap highlight (needs at least 3 laps to be meaningful).
    val bestLap = if (laps.size >= 3) laps.minByOrNull { it.lapMs }?.number else null
    val worstLap = if (laps.size >= 3) laps.maxByOrNull { it.lapMs }?.number else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Stopwatch",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Start, pause and record laps",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big time display
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatStopwatch(displayMs),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = if (running) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    modifier = Modifier.testTag("stopwatch_time")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls: Reset | Start/Pause | Lap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.reset() },
                enabled = !running && (displayMs > 0L || laps.isNotEmpty()),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("stopwatch_reset")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(28.dp)
                )
            }

            Button(
                onClick = { viewModel.toggle() },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(96.dp)
                    .testTag("stopwatch_start_pause")
            ) {
                Icon(
                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (running) "Pause" else "Start",
                    modifier = Modifier.size(44.dp)
                )
            }

            OutlinedButton(
                onClick = { viewModel.lap() },
                enabled = running,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("stopwatch_lap")
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Lap",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Laps
        if (laps.isEmpty()) {
            Text(
                text = "Press the flag while running to record a lap.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Lap",
                    modifier = Modifier.width(56.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    "Lap time",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    "Total",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(laps, key = { it.number }) { lap ->
                    val tint = when (lap.number) {
                        bestLap -> MaterialTheme.colorScheme.tertiary
                        worstLap -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${lap.number}",
                                modifier = Modifier.width(48.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tint
                                )
                            )
                            Text(
                                formatStopwatch(lap.lapMs),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tint
                                )
                            )
                            Text(
                                formatStopwatch(lap.totalMs),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
