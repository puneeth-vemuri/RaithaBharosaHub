package com.raithabharosahub.presentation.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.ThermostatAuto
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raithabharosahub.R
import com.raithabharosahub.domain.model.SowingState
import com.raithabharosahub.ui.theme.BrandGreen
import com.raithabharosahub.ui.theme.RaithaBharosaHubTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // App icon (green leaf) + app name
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = stringResource(R.string.app_name))
                        if (uiState.isOfflineMode) {
                            AssistChip(
                                onClick = { },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFFFB300), // Amber
                                    labelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = null,
                                modifier = Modifier.height(24.dp),
                                label = {
                                    Text(
                                        text = stringResource(R.string.offline_mode),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                    IconButton(
                        onClick = {
                            // Use default Bangalore coordinates for refresh
                            viewModel.refresh(12.9716, 77.5946)
                        },
                        modifier = Modifier.size(48.dp) // Minimum touch target
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.generateSimulatedData() }
            ) {
                Text(text = stringResource(R.string.simulate_data))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Circular Gauge
                CircularGauge(
                    score = uiState.sowingResult?.score ?: 0f,
                    sowingState = uiState.sowingResult?.state ?: SowingState.RED,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Kannada action text
                val messageId = uiState.sowingResult?.messageId ?: R.string.wait_conditions_poor
                Text(
                    text = stringResource(messageId),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Data cards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DataCard(
                        value = uiState.moisture,
                        unit = "%",
                        label = stringResource(R.string.moisture_label),
                        icon = Icons.Filled.LocalDrink,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        value = uiState.temperature,
                        unit = "°C",
                        label = stringResource(R.string.temperature_label),
                        icon = Icons.Filled.ThermostatAuto,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        value = uiState.humidity,
                        unit = "%",
                        label = stringResource(R.string.humidity_label),
                        icon = Icons.Filled.AcUnit,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        value = uiState.rainForecast24h,
                        unit = "mm",
                        label = stringResource(R.string.rain_24h_label),
                        icon = Icons.Filled.Cloud,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Last updated + loading indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val lastUpdatedText = uiState.lastUpdated.ifEmpty { "Never" }
                        Text(
                            text = stringResource(R.string.last_updated, lastUpdatedText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            item {
                // Error message
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularGauge(
    score: Float,
    sowingState: SowingState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "sowing_index_gauge"
    )

    val animatedColor by animateColorAsState(
        targetValue = when {
            score > 70 -> Color(0xFF1DA34A)
            score > 40 -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        },
        animationSpec = tween(durationMillis = 600),
        label = "gauge_color"
    )

    val stateText = when (sowingState) {
        SowingState.GREEN -> stringResource(R.string.sow_now)
        SowingState.YELLOW -> stringResource(R.string.caution_check_conditions)
        SowingState.RED -> stringResource(R.string.wait_conditions_poor)
    }

    Box(
        modifier = modifier
            .height(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center),
            progress = animatedProgress,
            color = animatedColor,
            trackColor = Color.LightGray.copy(alpha = 0.3f),
            strokeWidth = 24.dp
        )

        // Score text in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = animatedColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.score_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DataCard(
    value: Float,
    unit: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    softWrap = false
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    softWrap = false
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}