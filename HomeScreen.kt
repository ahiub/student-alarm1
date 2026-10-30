package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AlarmEntity
import com.example.data.preferences.AppMode
import com.example.ui.AlarmViewModel
import com.example.ui.components.AlarmItemCard
import com.example.ui.components.GreetingHeader
import com.example.ui.components.HeroAlarmCard

@Composable
fun HomeScreen(
    viewModel: AlarmViewModel,
    onAddAlarmClick: () -> Unit,
    onEditAlarmClick: (AlarmEntity) -> Unit,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val nextAlarm by viewModel.nextAlarm.collectAsStateWithLifecycle()
    val countdownText by viewModel.countdownText.collectAsStateWithLifecycle()
    val groupedAlarms by viewModel.groupedAlarms.collectAsStateWithLifecycle()
    val recentlyDeleted by viewModel.recentlyDeletedAlarm.collectAsStateWithLifecycle()
    val currentMode by viewModel.appMode.collectAsStateWithLifecycle()
    val activeCustomModeName by viewModel.activeCustomModeName.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Observe recently deleted alarm to trigger Undo Snackbar
    LaunchedEffect(recentlyDeleted) {
        recentlyDeleted?.let { deletedAlarm ->
            val result = snackbarHostState.showSnackbar(
                message = "Deleted: ${if (deletedAlarm.title.isNotBlank()) deletedAlarm.title else deletedAlarm.subject}",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearRecentlyDeleted()
            }
        }
    }

    val isStreakEnabled = viewModel.isFeatureEnabled("streak")
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val activeModeLabel = when (currentMode) {
        AppMode.CUSTOM -> "Mode: $activeCustomModeName"
        AppMode.STUDENT -> "Mode: Student"
        AppMode.COMMON -> "Mode: Common"
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val fabScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "fab_scale"
            )
            FloatingActionButton(
                onClick = onAddAlarmClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .scale(fabScale)
                    .size(60.dp)
                    .testTag("fab_add_alarm")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Alarm",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 1. Dynamic Greeting Header & Streak Chip & Active Mode Chip
            item {
                GreetingHeader(
                    streakCount = streakCount,
                    showStreak = isStreakEnabled,
                    activeModeLabel = activeModeLabel,
                    nextAlarmHour = nextAlarm?.hour,
                    nextAlarmMinute = nextAlarm?.minute ?: 0,
                    language = appLanguage,
                    onStreakClick = onStreakClick,
                    onSettingsClick = onStreakClick
                )
            }

            // 2. Next Alarm HERO CARD with live countdown & soft subject gradient
            item {
                HeroAlarmCard(
                    alarm = nextAlarm,
                    countdownText = countdownText,
                    onCardClick = {
                        nextAlarm?.let { onEditAlarmClick(it) }
                    },
                    onAddClick = onAddAlarmClick
                )
            }

            // 3. Grouped Alarm Lists (Today, Tomorrow, Later)
            if (groupedAlarms.today.isNotEmpty()) {
                item {
                    AlarmGroupHeader(title = "Today", count = groupedAlarms.today.size)
                }
                items(
                    items = groupedAlarms.today,
                    key = { "today_${it.id}" }
                ) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                        onClick = { onEditAlarmClick(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            if (groupedAlarms.tomorrow.isNotEmpty()) {
                item {
                    AlarmGroupHeader(title = "Tomorrow", count = groupedAlarms.tomorrow.size)
                }
                items(
                    items = groupedAlarms.tomorrow,
                    key = { "tomorrow_${it.id}" }
                ) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                        onClick = { onEditAlarmClick(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            if (groupedAlarms.later.isNotEmpty()) {
                item {
                    AlarmGroupHeader(title = "Later", count = groupedAlarms.later.size)
                }
                items(
                    items = groupedAlarms.later,
                    key = { "later_${it.id}" }
                ) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                        onClick = { onEditAlarmClick(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            // Extra padding at bottom for FAB clearance
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AlarmGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = "$count alarms",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        )
    }
}
