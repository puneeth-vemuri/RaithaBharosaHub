package com.raithabharosahub.presentation.dashboard

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.data.repository.WeatherRepository
import com.raithabharosahub.domain.calculator.SowingIndexCalculator
import com.raithabharosahub.data.generator.DataGeneratorClass
import com.raithabharosahub.domain.model.SowingResult
import com.raithabharosahub.domain.usecase.GetWeatherForecastUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "dashboard_prefs")

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase,
    private val sowingIndexCalculator: SowingIndexCalculator,
    private val dataGenerator: DataGeneratorClass,
    private val weatherRepository: WeatherRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState.Empty)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val plotIdKey = stringPreferencesKey("selected_plot_id")
    private val latitudeKey = stringPreferencesKey("selected_latitude")
    private val longitudeKey = stringPreferencesKey("selected_longitude")

    init {
        loadStoredLocation()
        observeWeatherAndCalculateIndex()
    }

    private fun loadStoredLocation() {
        viewModelScope.launch {
            val dataStore = context.dataStore
            val prefs = dataStore.data.first()
            val plotId = prefs[plotIdKey]?.toLongOrNull() ?: 1L

            // Start observing weather for this plot
            observeWeatherAndCalculateIndex(plotId)
        }
    }

    private fun observeWeatherAndCalculateIndex(plotId: Long = 1L) {
        getWeatherForecastUseCase(plotId)
            .onEach { weatherList ->
                _uiState.update { currentState ->
                    currentState.copy(
                        weatherList = weatherList,
                        isOfflineMode = weatherList.isEmpty()
                    )
                }
                calculateSowingIndexFromWeather(weatherList)
            }
            .launchIn(viewModelScope)
    }

    private fun calculateSowingIndexFromWeather(weatherList: List<WeatherEntity>) {
        if (weatherList.isEmpty()) {
            // No data, set default/empty state
            _uiState.update { currentState ->
                currentState.copy(
                    sowingResult = null,
                    moisture = 0f,
                    temperature = 0f,
                    humidity = 0f,
                    rainForecast24h = 0f,
                    lastUpdated = formatTimestamp(Date())
                )
            }
            return
        }

        // Use the latest weather entry
        val latestWeather = weatherList.maxByOrNull { it.date } ?: weatherList.first()
        
        // Calculate sowing index
        val sowingResult = sowingIndexCalculator.calculateSowingIndex(
            moisture = latestWeather.humidity, // Using humidity as moisture proxy
            temperature = latestWeather.tempMax,
            rainProbability = latestWeather.rainMm / 100f, // Convert mm to probability
            crop = "paddy" // Default crop - should come from farmer profile
        )

        _uiState.update { currentState ->
            currentState.copy(
                sowingResult = sowingResult,
                moisture = latestWeather.humidity,
                temperature = latestWeather.tempMax,
                humidity = latestWeather.humidity,
                rainForecast24h = latestWeather.rainMm,
                lastUpdated = formatTimestamp(latestWeather.fetchedAt)
            )
        }
    }

    private fun formatTimestamp(date: Date): String {
        return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
    }

    fun refresh(lat: Double, lon: Double) {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // Store location for future refreshes
                context.dataStore.edit { prefs ->
                    prefs[latitudeKey] = lat.toString()
                    prefs[longitudeKey] = lon.toString()
                }
                
                // Trigger refresh in repository - using default plotId 1 for now
                // In a real app, we would get the plotId from the database
                weatherRepository.refreshWeather(1L, lat, lon)
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to refresh weather data. Please check your connection.",
                    isOfflineMode = true
                ) }
            }
        }
    }

    fun generateSimulatedData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current plot ID
                val dataStore = context.dataStore
                val prefs = dataStore.data.first()
                val plotId = prefs[plotIdKey]?.toLongOrNull() ?: 1L
                
                // Generate sample weather data
                val generatedWeather = dataGenerator.generateSampleWeather(plotId)
                
                if (generatedWeather.isNotEmpty()) {
                    // Calculate sowing index from generated data
                    val latestWeather = generatedWeather.last()
                    val sowingResult = sowingIndexCalculator.calculateSowingIndex(
                        moisture = latestWeather.humidity,
                        temperature = latestWeather.tempMax,
                        rainProbability = latestWeather.rainMm / 100f,
                        crop = "paddy"
                    )
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            sowingResult = sowingResult,
                            weatherList = generatedWeather,
                            moisture = latestWeather.humidity,
                            temperature = latestWeather.tempMax,
                            humidity = latestWeather.humidity,
                            rainForecast24h = latestWeather.rainMm,
                            lastUpdated = formatTimestamp(Date()),
                            isLoading = false,
                            isOfflineMode = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "No simulated data generated"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Simulation failed. Please try again."
                ) }
            }
        }
    }

}