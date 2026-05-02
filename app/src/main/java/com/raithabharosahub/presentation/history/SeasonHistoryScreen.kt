package com.raithabharosahub.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raithabharosahub.R
import com.raithabharosahub.data.local.entity.SeasonEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SeasonHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val successMessage = stringResource(R.string.season_saved_success)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            scope.launch {
                snackbarHostState.showSnackbar(successMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.season_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.season_history_count, uiState.seasons.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.content_desc_filter)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleAddNew() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isAddingNew) Icons.Default.Cancel else Icons.Default.Add,
                    contentDescription = if (uiState.isAddingNew) {
                        stringResource(R.string.content_desc_cancel)
                    } else {
                        stringResource(R.string.content_desc_add_season)
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HeroSummaryCard(
                            totalSeasons = uiState.seasons.size,
                            completedSeasons = uiState.seasons.count { it.harvestDate != null && it.yieldKg != null },
                            activeFilter = uiState.selectedCropFilter
                        )
                    }

                    item {
                        CropFilterSection(
                            seasons = uiState.seasons,
                            selectedCropFilter = uiState.selectedCropFilter,
                            onSelectFilter = viewModel::selectFilter
                        )
                    }

                    item {
                        YieldChart(
                            seasons = uiState.seasons,
                            selectedCropFilter = uiState.selectedCropFilter
                        )
                    }

                    item {
                        AnimatedVisibility(
                            visible = uiState.isAddingNew,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            AddSeasonForm(
                                formState = uiState.formState,
                                onUpdateField = viewModel::updateFormField,
                                onSave = viewModel::saveEntry
                            )
                        }
                    }

                    item {
                        SeasonStatistics(seasons = uiState.seasons)
                    }

                    item {
                        SeasonsHeader(
                            seasonCount = uiState.seasons.size,
                            activeFilter = uiState.selectedCropFilter
                        )
                    }

                    if (uiState.seasons.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    } else {
                        items(uiState.seasons) { season ->
                            SeasonCard(
                                season = season,
                                onDelete = { viewModel.deleteEntry(season.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    totalSeasons: Int,
    completedSeasons: Int,
    activeFilter: String?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.season_history_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.add_first_season),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = totalSeasons.toString(),
                    label = stringResource(R.string.total_seasons),
                    accent = MaterialTheme.colorScheme.primary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = completedSeasons.toString(),
                    label = stringResource(R.string.completed),
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }

            if (activeFilter != null) {
                AssistChip(
                    onClick = { },
                    label = { Text(activeFilter) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CropFilterSection(
    seasons: List<SeasonEntity>,
    selectedCropFilter: String?,
    onSelectFilter: (String?) -> Unit
) {
    val crops = seasons.map { it.crop }.distinct()

    if (crops.isEmpty()) return

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.filter_by_crop),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCropFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text(stringResource(R.string.all_crops)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                items(crops) { crop ->
                    FilterChip(
                        selected = selectedCropFilter == crop,
                        onClick = { onSelectFilter(crop) },
                        label = { Text(crop) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
        
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSeasonForm(
    formState: SeasonFormState,
    onUpdateField: (SeasonFormField, String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSowDatePicker by remember { mutableStateOf(false) }
    var showHarvestDatePicker by remember { mutableStateOf(false) }
    
    val sowDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            formState.sowDate.toLong()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )
    val harvestDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            formState.harvestDate.toLong()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    // Helper functions to format dates safely
    val sowDateText = runCatching {
        dateFormat.format(Date(formState.sowDate.toLong()))
    }.getOrElse { stringResource(R.string.sow_date_hint) }
    
    val harvestDateText = runCatching {
        if (formState.harvestDate.isNotBlank()) {
            dateFormat.format(Date(formState.harvestDate.toLong()))
        } else {
            stringResource(R.string.harvest_date_hint)
        }
    }.getOrElse { stringResource(R.string.harvest_date_hint) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = stringResource(R.string.add_new_season),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.season_history_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = formState.crop,
                onValueChange = { onUpdateField(SeasonFormField.CROP, it) },
                label = { Text(stringResource(R.string.crop_hint)) },
                isError = formState.validationErrors.containsKey("crop"),
                supportingText = {
                    if (formState.validationErrors.containsKey("crop")) {
                        Text(formState.validationErrors["crop"] ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Sow Date Picker Button
            Button(
                onClick = { showSowDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (formState.validationErrors.containsKey("sowDate")) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sow_date_hint) + ": $sowDateText")
            }
            
            // Harvest Date Picker Button (optional)
            Button(
                onClick = { showHarvestDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.harvest_date_hint) + ": $harvestDateText")
            }

            OutlinedTextField(
                value = formState.yieldKg,
                onValueChange = { onUpdateField(SeasonFormField.YIELD_KG, it) },
                label = { Text(stringResource(R.string.yield_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { onUpdateField(SeasonFormField.NOTES, it) },
                label = { Text(stringResource(R.string.notes_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_season))
            }
        }
        
        // Sow Date Picker Dialog
        if (showSowDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showSowDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sowDatePickerState.selectedDateMillis?.let { selectedDate ->
                                onUpdateField(SeasonFormField.SOW_DATE, selectedDate.toString())
                            }
                            showSowDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSowDatePicker = false }
                    ) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            ) {
                DatePicker(state = sowDatePickerState)
            }
        }
        
        // Harvest Date Picker Dialog
        if (showHarvestDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showHarvestDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            harvestDatePickerState.selectedDateMillis?.let { selectedDate ->
                                onUpdateField(SeasonFormField.HARVEST_DATE, selectedDate.toString())
                            }
                            showHarvestDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showHarvestDatePicker = false }
                    ) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            ) {
                DatePicker(state = harvestDatePickerState)
            }
        }
    }
}

@Composable
private fun SeasonStatistics(seasons: List<SeasonEntity>) {
    val completedSeasons = seasons.filter { it.harvestDate != null && it.yieldKg != null }
    val totalYield = completedSeasons.sumOf { it.yieldKg?.toDouble() ?: 0.0 }
    val avgYield = if (completedSeasons.isNotEmpty()) totalYield / completedSeasons.size else 0.0

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.avg_yield_kg),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = seasons.size.toString(),
                    label = stringResource(R.string.total_seasons),
                    accent = MaterialTheme.colorScheme.primary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = completedSeasons.size.toString(),
                    label = stringResource(R.string.completed),
                    accent = MaterialTheme.colorScheme.tertiary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = String.format(Locale.getDefault(), "%.1f", avgYield),
                    label = stringResource(R.string.avg_yield_kg),
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SeasonsHeader(
    seasonCount: Int,
    activeFilter: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.season_history_count, seasonCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (activeFilter != null) {
            AssistChip(
                onClick = { },
                label = { Text(activeFilter) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SeasonCard(
    season: SeasonEntity,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (season.harvestDate == null) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = season.crop,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.plot_number, season.plotId)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = if (season.harvestDate == null) {
                                    stringResource(R.string.ongoing)
                                } else {
                                    stringResource(R.string.completed)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (season.harvestDate == null) {
                                    Icons.Default.History
                                } else {
                                    Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (season.harvestDate == null) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sow_date_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(season.sowDate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    season.harvestDate?.let { harvestDate ->
                        Text(
                            text = stringResource(R.string.harvest_date_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(harvestDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                season.yieldKg?.let { yield ->
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${yield} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.yield),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            season.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.notes_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.no_season_history),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.add_first_season),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun YieldChart(seasons: List<SeasonEntity>, selectedCropFilter: String?) {
    val completedSeasons = seasons.filter { it.harvestDate != null && it.yieldKg != null }
        .filter { selectedCropFilter == null || it.crop == selectedCropFilter }
        .sortedBy { it.sowDate }
        
    if (completedSeasons.isEmpty()) return

    val chartDesc = stringResource(R.string.chart_yield_description)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            factory = { context ->
                com.github.mikephil.charting.charts.BarChart(context).apply {
                    description.text = chartDesc
                    setDrawGridBackground(false)
                    axisRight.isEnabled = false
                    
                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.granularity = 1f
                }
            },
            update = { chart ->
                val entries = mutableListOf<com.github.mikephil.charting.data.BarEntry>()
                val colors = mutableListOf<Int>()
                val labels = mutableListOf<String>()

                val dateFormat = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())

                completedSeasons.forEachIndexed { index, season ->
                    entries.add(com.github.mikephil.charting.data.BarEntry(index.toFloat(), season.yieldKg!!))
                    labels.add(dateFormat.format(season.sowDate))
                    
                    val color = when (season.crop) {
                        "Paddy" -> android.graphics.Color.parseColor("#1DA34A")
                        "Ragi" -> android.graphics.Color.parseColor("#F59E0B")
                        "Sugarcane" -> android.graphics.Color.parseColor("#0EA5E9")
                        else -> android.graphics.Color.DKGRAY
                    }
                    colors.add(color)
                }

                val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Yield (kg)").apply {
                    this.colors = colors
                    valueTextSize = 10f
                }

                chart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                chart.data = com.github.mikephil.charting.data.BarData(dataSet)
                chart.invalidate()
            }
        )
    }
}
