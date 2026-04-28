package com.raithabharosahub.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.raithabharosahub.data.local.dao.WeatherDao
import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.data.generator.DataGeneratorClass
import com.raithabharosahub.data.remote.WeatherApiService
import com.raithabharosahub.data.remote.dto.CityDto
import com.raithabharosahub.data.remote.dto.CoordinateDto
import com.raithabharosahub.data.remote.dto.ForecastDto
import com.raithabharosahub.data.remote.dto.MainDto
import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for WeatherRepository.
 * Tests API success, mock fallback, and Room persistence.
 */
class WeatherRepositoryTest {

    private lateinit var repository: WeatherRepository
    private lateinit var fakeWeatherDao: FakeWeatherDao
    private lateinit var mockApiService: MockWeatherApiService
    private lateinit var dataGenerator: DataGeneratorClass
    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeWeatherDao = FakeWeatherDao()
        mockApiService = MockWeatherApiService()
        dataGenerator = DataGeneratorClass()
        moshi = Moshi.Builder().build()

        repository = WeatherRepository(
            context = context,
            weatherApiService = mockApiService,
            weatherDao = fakeWeatherDao,
            dataGenerator = dataGenerator,
            moshi = moshi
        )
    }

    /**
     * Test case 1: API success → data saved to Room → Flow emits
     */
    @Test
    fun test_apiSuccess_savesToRoom_flowEmits() = runBlocking {
        val plotId = 1L
        val latitude = 12.9716
        val longitude = 77.5946

        // Arrange: API returns valid response
        mockApiService.shouldSucceed = true

        // Act: Call refreshWeather
        repository.refreshWeather(plotId, latitude, longitude)

        // Assert: Data was saved to DAO
        val savedData = fakeWeatherDao.getByPlotId(plotId).first()
        assertEquals(expected = 56, actual = savedData.size, message = "Should have 56 forecast items")

        // Verify flow emits data
        assertNotNull(savedData, "Flow should emit saved weather data")
        assertEquals(expected = plotId, actual = savedData[0].plotId, message = "Plot ID should match")
    }

    /**
     * Test case 2: API failure → mock JSON loaded → data saved to Room → Flow emits
     */
    @Test
    fun test_apiFailure_loadsMockJson_savesToRoom_flowEmits() = runBlocking {
        val plotId = 2L
        val latitude = 12.9716
        val longitude = 77.5946

        // Arrange: API will fail
        mockApiService.shouldSucceed = false

        // Act: Call refreshWeather
        repository.refreshWeather(plotId, latitude, longitude)

        // Assert: Mock data was loaded and saved to DAO
        val savedData = fakeWeatherDao.getByPlotId(plotId).first()
        assertEquals(expected = 56, actual = savedData.size, message = "Mock should have 56 forecast items")

        // Verify specific mock data
        val firstItem = savedData[0]
        assertEquals(expected = plotId, actual = firstItem.plotId)
        assertEquals(expected = 28.5f, actual = firstItem.tempMax, message = "First mock temp should be 28.5°C")
    }

    /**
     * Test case 3: Multiple plots can have separate weather data
     */
    @Test
    fun test_multiplePlots_separateWeatherData() = runBlocking {
        val plot1Id = 1L
        val plot2Id = 2L
        val latitude = 12.9716
        val longitude = 77.5946

        mockApiService.shouldSucceed = true

        // Act: Refresh weather for both plots
        repository.refreshWeather(plot1Id, latitude, longitude)
        repository.refreshWeather(plot2Id, latitude - 0.1, longitude - 0.1)

        // Assert: Both plots have data
        val plot1Data = fakeWeatherDao.getByPlotId(plot1Id).first()
        val plot2Data = fakeWeatherDao.getByPlotId(plot2Id).first()

        assertEquals(expected = 56, actual = plot1Data.size)
        assertEquals(expected = 56, actual = plot2Data.size)
        assertEquals(expected = plot1Id, actual = plot1Data[0].plotId)
        assertEquals(expected = plot2Id, actual = plot2Data[0].plotId)
    }

    /**
     * Test case 4: Old data is replaced on refresh
     */
    @Test
    fun test_refreshReplaces_oldData() = runBlocking {
        val plotId = 1L
        val latitude = 12.9716
        val longitude = 77.5946

        mockApiService.shouldSucceed = true

        // Act: First refresh
        repository.refreshWeather(plotId, latitude, longitude)
        var savedData = fakeWeatherDao.getByPlotId(plotId).first()
        val firstCount = savedData.size

        // Act: Second refresh (simulating data update)
        repository.refreshWeather(plotId, latitude, longitude)
        savedData = fakeWeatherDao.getByPlotId(plotId).first()

        // Assert: Old data replaced, not appended
        assertEquals(expected = firstCount, actual = savedData.size, message = "Data should be replaced, not appended")
    }

    /**
     * Fake implementation of WeatherDao for in-memory testing.
     * Stores data in a mutable map without a real database.
     */
    private class FakeWeatherDao : WeatherDao {
        private val dataStore = mutableMapOf<Long, MutableList<WeatherEntity>>()

        override suspend fun insert(weather: WeatherEntity): Long {
            dataStore.computeIfAbsent(weather.plotId) { mutableListOf() }.add(weather)
            return 1L
        }

        override suspend fun insertAll(weatherList: List<WeatherEntity>) {
            weatherList.forEach { insert(it) }
        }

        override suspend fun update(weather: WeatherEntity) {
            // Not implemented for tests
        }

        override suspend fun delete(weather: WeatherEntity) {
            // Not implemented for tests
        }

        override suspend fun deleteById(weatherId: Long) {
            // Not implemented for tests
        }

        override suspend fun deleteByPlotId(plotId: Long) {
            dataStore.remove(plotId)
        }

        override suspend fun deleteOlderThan(thresholdDate: Date) {
            // Not implemented for tests
        }

        override suspend fun getById(weatherId: Long): WeatherEntity? {
            // Not implemented for tests
            return null
        }

        override fun getByPlotId(plotId: Long): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            return kotlinx.coroutines.flow.flowOf(dataStore[plotId] ?: emptyList())
        }

        override fun getByPlotIdAndDateRange(
            plotId: Long,
            startDate: Date,
            endDate: Date
        ): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            // Not implemented for tests
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        override suspend fun getLatestByPlotId(plotId: Long): WeatherEntity? {
            // Not implemented for tests
            return null
        }

        override fun getByDateRange(
            startDate: Date,
            endDate: Date
        ): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            // Not implemented for tests
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Mock implementation of WeatherApiService for testing.
     * Can be configured to succeed or fail.
     */
    private class MockWeatherApiService : WeatherApiService {
        var shouldSucceed = true

        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            apiKey: String,
            units: String,
            cnt: Int
        ): WeatherResponseDto {
            if (!shouldSucceed) {
                throw Exception("API call failed (simulated)")
            }

            // Return a minimal valid response for testing
            return WeatherResponseDto(
                forecastList = createMockForecastList(),
                city = CityDto(
                    id = 1277333,
                    name = "Bengaluru",
                    coord = CoordinateDto(lat = latitude, lon = longitude),
                    country = "IN",
                    timezone = 19800
                )
            )
        }

        private fun createMockForecastList(): List<ForecastDto> {
            // Create 56 minimal forecast items (7 days × 8 items per day)
            return (0 until 56).map { index ->
                ForecastDto(
                    dt = 1709251200L + (index * 3600L),  // 3-hour increments
                    dtTxt = "2024-03-01 ${String.format("%02d", (index % 8) * 3)}:00:00",
                    main = MainDto(
                        temp = 25f + index * 0.1f,
                        tempMin = 20f,
                        tempMax = 28.5f + index * 0.05f,
                        humidity = 65 + (index % 20),
                        pressure = 1012
                    ),
                    rain = if (index % 10 == 0) {
                        com.raithabharosahub.data.remote.dto.RainDto(threeHour = 2.5f)
                    } else {
                        com.raithabharosahub.data.remote.dto.RainDto(threeHour = 0f)
                    },
                    weather = emptyList(),
                    rainProbability = if (index % 10 == 0) 0.3f else 0.0f
                )
            }
        }
    }
}

    /**
     * Test case 1: API success → data saved to Room → Flow emits
     */
    @Test
    fun test_apiSuccess_savesToRoom_flowEmits() = runBlocking {
        val plotId = 1L
        val latitude = 12.9716
        val longitude = 77.5946

        // Arrange: API returns valid response
        mockApiService.shouldSucceed = true

        // Act: Call refreshWeather
        repository.refreshWeather(plotId, latitude, longitude)

        // Assert: Data was saved to DAO
        val savedData = fakeWeatherDao.getByPlotId(plotId).first()
        assertEquals(expected = 56, actual = savedData.size, message = "Should have 56 forecast items")

        // Verify flow emits data
        assertNotNull(savedData, "Flow should emit saved weather data")
        assertEquals(expected = plotId, actual = savedData[0].plotId, message = "Plot ID should match")
    }

    /**
     * Test case 2: API failure → mock JSON loaded → data saved to Room → Flow emits
     */
    @Test
    fun test_apiFailure_loadsMockJson_savesToRoom_flowEmits() = runBlocking {
        val plotId = 2L
        val latitude = 12.9716
        val longitude = 77.5946

        // Arrange: API will fail
        mockApiService.shouldSucceed = false

        // Act: Call refreshWeather
        repository.refreshWeather(plotId, latitude, longitude)

        // Assert: Mock data was loaded and saved to DAO
        val savedData = fakeWeatherDao.getByPlotId(plotId).first()
        assertEquals(expected = 56, actual = savedData.size, message = "Mock should have 56 forecast items")

        // Verify specific mock data
        val firstItem = savedData[0]
        assertEquals(expected = plotId, actual = firstItem.plotId)
        assertEquals(expected = 28.5f, actual = firstItem.tempMax, message = "First mock temp should be 28.5°C")
    }

    /**
     * Test case 3: Multiple plots can have separate weather data
     */
    @Test
    fun test_multipleePlots_separateWeatherData() = runBlocking {
        val plot1Id = 1L
        val plot2Id = 2L
        val latitude = 12.9716
        val longitude = 77.5946

        mockApiService.shouldSucceed = true

        // Act: Refresh weather for both plots
        repository.refreshWeather(plot1Id, latitude, longitude)
        repository.refreshWeather(plot2Id, latitude - 0.1, longitude - 0.1)

        // Assert: Both plots have data
        val plot1Data = fakeWeatherDao.getByPlotId(plot1Id).first()
        val plot2Data = fakeWeatherDao.getByPlotId(plot2Id).first()

        assertEquals(expected = 56, actual = plot1Data.size)
        assertEquals(expected = 56, actual = plot2Data.size)
        assertEquals(expected = plot1Id, actual = plot1Data[0].plotId)
        assertEquals(expected = plot2Id, actual = plot2Data[0].plotId)
    }

    /**
     * Test case 4: Old data is replaced on refresh
     */
    @Test
    fun test_refreshReplaces_oldData() = runBlocking {
        val plotId = 1L
        val latitude = 12.9716
        val longitude = 77.5946

        mockApiService.shouldSucceed = true

        // Act: First refresh
        repository.refreshWeather(plotId, latitude, longitude)
        var savedData = fakeWeatherDao.getByPlotId(plotId).first()
        val firstCount = savedData.size

        // Act: Second refresh (simulating data update)
        repository.refreshWeather(plotId, latitude, longitude)
        savedData = fakeWeatherDao.getByPlotId(plotId).first()

        // Assert: Old data replaced, not appended
        assertEquals(expected = firstCount, actual = savedData.size, message = "Data should be replaced, not appended")
    }

    /**
     * Fake implementation of WeatherDao for in-memory testing.
     * Stores data in a mutable map without a real database.
     */
    private class FakeWeatherDao : WeatherDao {
        private val dataStore = mutableMapOf<Long, MutableList<WeatherEntity>>()

        override suspend fun insert(weather: WeatherEntity): Long {
            dataStore.computeIfAbsent(weather.plotId) { mutableListOf() }.add(weather)
            return 1L
        }

        override suspend fun insertAll(weatherList: List<WeatherEntity>) {
            weatherList.forEach { insert(it) }
        }

        override suspend fun update(weather: WeatherEntity) {
            // Not implemented for tests
        }

        override suspend fun delete(weather: WeatherEntity) {
            // Not implemented for tests
        }

        override suspend fun deleteById(weatherId: Long) {
            // Not implemented for tests
        }

        override suspend fun deleteByPlotId(plotId: Long) {
            dataStore.remove(plotId)
        }

        override suspend fun deleteOlderThan(thresholdDate: Date) {
            // Not implemented for tests
        }

        override suspend fun getById(weatherId: Long): WeatherEntity? {
            // Not implemented for tests
            return null
        }

        override fun getByPlotId(plotId: Long): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            return kotlinx.coroutines.flow.flowOf(dataStore[plotId] ?: emptyList())
        }

        override fun getByPlotIdAndDateRange(
            plotId: Long,
            startDate: Date,
            endDate: Date
        ): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            // Not implemented for tests
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        override suspend fun getLatestByPlotId(plotId: Long): WeatherEntity? {
            // Not implemented for tests
            return null
        }

        override fun getByDateRange(
            startDate: Date,
            endDate: Date
        ): kotlinx.coroutines.flow.Flow<List<WeatherEntity>> {
            // Not implemented for tests
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Mock implementation of WeatherApiService for testing.
     * Can be configured to succeed or fail.
     */
    private class MockWeatherApiService : WeatherApiService {
        var shouldSucceed = true

        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            apiKey: String,
            units: String,
            cnt: Int
        ): WeatherResponseDto {
            if (!shouldSucceed) {
                throw Exception("API call failed (simulated)")
            }

            // Return a minimal valid response for testing
            return WeatherResponseDto(
                forecastList = createMockForecastList(),
                city = CityDto(
                    id = 1277333,
                    name = "Bengaluru",
                    coord = CoordinateDto(lat = latitude, lon = longitude),
                    country = "IN",
                    timezone = 19800
                )
            )
        }

        private fun createMockForecastList(): List<ForecastDto> {
            // Create 56 minimal forecast items (7 days × 8 items per day)
            return (0 until 56).map { index ->
                ForecastDto(
                    dt = 1709251200L + (index * 3600L),  // 3-hour increments
                    dtTxt = "2024-03-01 ${String.format("%02d", (index % 8) * 3)}:00:00",
                    main = MainDto(
                        temp = 25f + index * 0.1f,
                        tempMin = 20f,
                        tempMax = 28.5f + index * 0.05f,
                        humidity = 65 + (index % 20),
                        pressure = 1012
                    ),
                    rain = if (index % 10 == 0) {
                        com.raithabharosahub.data.remote.dto.RainDto(threeHour = 2.5f)
                    } else {
                        com.raithabharosahub.data.remote.dto.RainDto(threeHour = 0f)
                    },
                    weather = emptyList(),
                    rainProbability = if (index % 10 == 0) 0.3f else 0.0f
                )
            }
        }
    }
}
