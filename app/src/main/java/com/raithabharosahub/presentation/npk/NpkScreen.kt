package com.raithabharosahub.presentation.npk

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raithabharosahub.R
import com.raithabharosahub.util.NpkRecommendationEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NPK Input Centre screen with two tabs: "Enter Test" and "History".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NpkScreen(
    viewModel: NpkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.npk_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.enter_test_tab)) },
                    icon = { Icon(Icons.Filled.Science, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.history_tab)) },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> EnterTestTab(uiState, viewModel)
                1 -> HistoryTab(uiState)
            }
        }
    }
}

/**
 * Tab 1: Enter Test form and recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterTestTab(
    uiState: NpkUiState,
    viewModel: NpkViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.testDate
    )
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nitrogen Input
        item {
            OutlinedTextField(
                value = uiState.nitrogen,
                onValueChange = { viewModel.updateField(NpkField.NITROGEN, it) },
                label = { Text(stringResource(R.string.nitrogen_label)) },
                trailingIcon = { Text(stringResource(R.string.kg_per_ha)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey(NpkField.NITROGEN.name),
                supportingText = {
                    if (uiState.validationErrors.containsKey(NpkField.NITROGEN.name)) {
                        Text(uiState.validationErrors[NpkField.NITROGEN.name] ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Phosphorus Input
        item {
            OutlinedTextField(
                value = uiState.phosphorus,
                onValueChange = { viewModel.updateField(NpkField.PHOSPHORUS, it) },
                label = { Text(stringResource(R.string.phosphorus_label)) },
                trailingIcon = { Text(stringResource(R.string.kg_per_ha)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey(NpkField.PHOSPHORUS.name),
                supportingText = {
                    if (uiState.validationErrors.containsKey(NpkField.PHOSPHORUS.name)) {
                        Text(uiState.validationErrors[NpkField.PHOSPHORUS.name] ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Potassium Input
        item {
            OutlinedTextField(
                value = uiState.potassium,
                onValueChange = { viewModel.updateField(NpkField.POTASSIUM, it) },
                label = { Text(stringResource(R.string.potassium_label)) },
                trailingIcon = { Text(stringResource(R.string.kg_per_ha)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey(NpkField.POTASSIUM.name),
                supportingText = {
                    if (uiState.validationErrors.containsKey(NpkField.POTASSIUM.name)) {
                        Text(uiState.validationErrors[NpkField.POTASSIUM.name] ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Test Date
        item {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateText = dateFormat.format(Date(uiState.testDate))
            
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.test_date_label) + ": $dateText")
            }
        }

        // Lab Name
        item {
            OutlinedTextField(
                value = uiState.labName,
                onValueChange = { viewModel.updateField(NpkField.LAB_NAME, it) },
                label = { Text(stringResource(R.string.lab_name_label)) },
                isError = uiState.validationErrors.containsKey(NpkField.LAB_NAME.name),
                supportingText = {
                    if (uiState.validationErrors.containsKey(NpkField.LAB_NAME.name)) {
                        Text(uiState.validationErrors[NpkField.LAB_NAME.name] ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Calculate Button
        item {
            Button(
                onClick = { viewModel.calculateRecommendations() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.nitrogen.isNotBlank() &&
                          uiState.phosphorus.isNotBlank() &&
                          uiState.potassium.isNotBlank() &&
                          uiState.labName.isNotBlank()
            ) {
                Text(stringResource(R.string.calculate_button))
            }
        }

        // Recommendations
        if (uiState.recommendations.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recommendations_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            items(uiState.recommendations) { recommendation ->
                RecommendationCard(recommendation)
            }

            // Save Button
            item {
                Button(
                    onClick = { viewModel.saveEntry() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.recommendations.isNotEmpty() && !uiState.isSaved
                ) {
                    Text(stringResource(R.string.save_button))
                }
            }
        }

        // Success message
        if (uiState.isSaved) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.npk_test_saved),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            viewModel.updateField(NpkField.TEST_DATE, selectedDate.toString())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Recommendation card for a single nutrient.
 */
@Composable
private fun RecommendationCard(
    recommendation: NpkRecommendationEngine.NpkRecommendation
) {
    val statusColor = when (recommendation.status) {
        NpkRecommendationEngine.NutrientStatus.DEFICIENT -> Color(0xFFD32F2F) // Red
        NpkRecommendationEngine.NutrientStatus.OPTIMAL -> Color(0xFF388E3C)   // Green
        NpkRecommendationEngine.NutrientStatus.EXCESS -> Color(0xFFF57C00)    // Orange/Yellow
    }
    
    val statusText = when (recommendation.status) {
        NpkRecommendationEngine.NutrientStatus.DEFICIENT -> stringResource(R.string.nutrient_deficient)
        NpkRecommendationEngine.NutrientStatus.OPTIMAL -> stringResource(R.string.nutrient_optimal)
        NpkRecommendationEngine.NutrientStatus.EXCESS -> stringResource(R.string.nutrient_excess)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = recommendation.nutrient,
                    style = MaterialTheme.typography.titleMedium
                )
                
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text(statusText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = statusColor,
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(recommendation.messageId),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = stringResource(R.string.deviation_format, recommendation.deviation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Tab 2: History of past NPK tests.
 */
@Composable
private fun HistoryTab(
    uiState: NpkUiState
) {
    if (uiState.history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(R.string.no_history),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.history) { npkEntity ->
                HistoryItem(npkEntity)
            }
        }
    }
}

/**
 * History item card for a past NPK test.
 */
@Composable
private fun HistoryItem(
    npkEntity: com.raithabharosahub.data.local.entity.NpkEntity
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateText = dateFormat.format(npkEntity.testDate)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = npkEntity.labName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NutrientDisplay(stringResource(R.string.nutrient_n), npkEntity.nitrogen)
                NutrientDisplay(stringResource(R.string.nutrient_p), npkEntity.phosphorus)
                NutrientDisplay(stringResource(R.string.nutrient_k), npkEntity.potassium)
            }
        }
    }
}

@Composable
private fun NutrientDisplay(
    label: String,
    value: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.kg_per_ha),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}