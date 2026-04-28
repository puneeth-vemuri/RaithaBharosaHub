package com.raithabharosahub.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raithabharosahub.R
import com.raithabharosahub.domain.model.SowingState
import com.raithabharosahub.ui.theme.RaithaBharosaHubTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KrishiCalendarScreen(
    viewModel: KrishiCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.calendar_title))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.days.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_weather_data),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // SECTION A — Horizontal scrollable strip (top)
                Text(
                    text = stringResource(R.string.calendar_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DayStrip(
                    days = uiState.days,
                    selectedIndex = uiState.selectedDayIndex,
                    onDaySelected = viewModel::selectDay
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // SECTION B — Day Detail Panel
                uiState.selectedDay?.let { day ->
                    DayDetailPanel(day = day)
                }
            }
        }
    }
}

@Composable
private fun DayStrip(
    days: List<com.raithabharosahub.domain.model.KrishiDay>,
    selectedIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(days) { index, day ->
            DayCard(
                day = day,
                isSelected = index == selectedIndex,
                onClick = { onDaySelected(index) }
            )
        }
    }
}

@Composable
private fun DayCard(
    day: com.raithabharosahub.domain.model.KrishiDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 8.dp) else CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day name
            Text(
                text = stringResource(getDayLabelResource(day.dayLabel)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Weather icon
            Text(
                text = day.weatherIcon,
                fontSize = 24.sp
            )
            
            // Sowing Index badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(getSowingStateColor(day.sowingState)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.sowingScore.toInt()}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Storm warning icon
            if (day.hasStormWarning) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.storm_warning_label),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }
            
            // Crop milestone tag
            day.cropMilestone?.let { milestone ->
                Text(
                    text = "★",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DayDetailPanel(day: com.raithabharosahub.domain.model.KrishiDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Full date header
            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            val dateStr = dateFormat.format(Date(day.date))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Large SowingState indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(getSowingStateColor(day.sowingState))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getSowingStateText(day.sowingState),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = getSowingStateColor(day.sowingState)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${day.sowingScore.toInt()} ${stringResource(R.string.sowing_index_label)})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Storm warning banner
            if (day.hasStormWarning && day.stormWarningMessage != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(day.stormWarningMessage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Data breakdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.day_detail_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Rain
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.rain_mm_label)}: ${String.format("%.1f", day.rainMm)} mm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Temperature (mock - using sowing score as proxy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val temp = 20 + (day.sowingScore / 100 * 15).toInt()
                        Text(
                            text = "${stringResource(R.string.temp_max_label)}: ${temp}°C",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Recommended action
            if (day.recommendedAction != 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recommended_action_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = stringResource(day.recommendedAction),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Crop milestone card
            day.cropMilestone?.let { milestone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.crop_milestone_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(getMilestoneResource(milestone)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getSowingStateColor(state: SowingState): Color {
    return when (state) {
        SowingState.GREEN -> Color(0xFF4CAF50) // Green
        SowingState.YELLOW -> Color(0xFFFFC107) // Yellow/Amber
        SowingState.RED -> Color(0xFFF44336) // Red
    }
}

@Composable
private fun getSowingStateText(state: SowingState): String {
    return when (state) {
        SowingState.GREEN -> stringResource(R.string.sowing_state_good)
        SowingState.YELLOW -> stringResource(R.string.sowing_state_moderate)
        SowingState.RED -> stringResource(R.string.sowing_state_poor)
    }
}

private fun getDayLabelResource(dayLabel: String): Int {
    return when (dayLabel) {
        "day_mon" -> R.string.day_mon
        "day_tue" -> R.string.day_tue
        "day_wed" -> R.string.day_wed
        "day_thu" -> R.string.day_thu
        "day_fri" -> R.string.day_fri
        "day_sat" -> R.string.day_sat
        "day_sun" -> R.string.day_sun
        else -> R.string.day_mon
    }
}

private fun getMilestoneResource(milestone: String): Int {
    return when (milestone) {
        "milestone_sow_day" -> R.string.milestone_sow_day
        "milestone_first_irrigation" -> R.string.milestone_first_irrigation
        "milestone_first_fertilization" -> R.string.milestone_first_fertilization
        else -> R.string.milestone_sow_day
    }
}