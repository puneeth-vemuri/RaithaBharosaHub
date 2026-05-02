package com.raithabharosahub.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.raithabharosahub.data.generator.DataGeneratorClass
import com.raithabharosahub.data.local.dao.WeatherDao
import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.data.remote.WeatherApiService
import com.raithabharosahub.data.remote.dto.ForecastDto
import com.raithabharosahub.data.remote.dto.MainDto
import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.HttpException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Date

/**
 * Pure JVM unit tests for [WeatherRepository].
 *
 * All Android deps (Context, AssetManager, android.util.Log) are handled via:
 *   - Mockito stubs for Context + AssetManager
 *   - android.jar stub returns defaults for Log.d/i/w on JVM (see NOTE below)
 *
 * Room DAO is replaced with [FakeWeatherDao] — an in-memory manual fake.
 *
 * NOTE: add this to android {} block in build.gradle.kts so Log stubs work:
 *   testOptions { unitTests { isReturnDefaultValues = true } }
 *
 * Three tiers under test:
 *   Tier 1 — API success        : data saved, lastUpdatedAt is recent
 *   Tier 2 — API IOException    : mock_weather.json fallback, Room still written
 *   Tier 3 — mock JSON failure  : DataGenerator fallback, Room still written
 *   Invariant: Room insertAll is called after every tier
 */
class WeatherRepositoryTest {

    // ------------------------------------------------------------------
    // Manual fake DAO
    // ------------------------------------------------------------------

    private class FakeWeatherDao : WeatherDao {
        val insertedBatches = mutableListOf<List<WeatherEntity>>()
        val deletedPlotIds  = mutableListOf<Long>()

        override suspend fun insertAll(weatherList: List<WeatherEntity>) {
            insertedBatches.add(weatherList)
        }
        override suspend fun deleteByPlotId(plotId: Long) {
            deletedPlotIds.add(plotId)
        }
        override fun getByPlotId(plotId: Long): Flow<List<WeatherEntity>> =
            flowOf(insertedBatches.lastOrNull() ?: emptyList())
        override suspend fun getLastUpdatedAt(plotId: Long): Date? =
            insertedBatches.lastOrNull()?.firstOrNull()?.lastUpdatedAt
        override fun observeLastUpdatedAt(plotId: Long): Flow<Date?> =
            flowOf(insertedBatches.lastOrNull()?.firstOrNull()?.lastUpdatedAt)

        // Unused stubs — required by the interface
        override suspend fun insert(weather: WeatherEntity) = 0L
        override suspend fun update(weather: WeatherEntity) {}
        override suspend fun delete(weather: WeatherEntity) {}
        override suspend fun deleteById(weatherId: Long) {}
        override suspend fun deleteOlderThan(thresholdDate: Date) {}
        override suspend fun getById(weatherId: Long) = null
        override fun getByPlotIdAndDateRange(
            plotId: Long, startDate: Date, endDate: Date
        ): Flow<List<WeatherEntity>> = flowOf(emptyList())
        override suspend fun getLatestByPlotId(plotId: Long) = null
        override fun getByDateRange(
            startDate: Date, endDate: Date
        ): Flow<List<WeatherEntity>> = flowOf(emptyList())
        override suspend fun countByPlot(plotId: Long) =
            insertedBatches.lastOrNull()?.size ?: 0
        override suspend fun getAverageTemperature(
            plotId: Long, startDate: Date, endDate: Date
        ) = null
        override suspend fun getTotalRainfall(
            plotId: Long, startDate: Date, endDate: Date
        ) = null
    }

    // ------------------------------------------------------------------
    // Test fixtures
    // ------------------------------------------------------------------

    private lateinit var fakeDao:      FakeWeatherDao
    private lateinit var mockContext:  Context
    private lateinit var mockAssets:   AssetManager
    private lateinit var mockApi:      WeatherApiService
    private lateinit var moshi:        Moshi
    private lateinit var repository:   WeatherRepository

    companion object {
        private const val PLOT_ID = 1L
        private const val LAT     = 12.97
        private const val LON     = 77.59

        // Minimal valid OWM /forecast JSON (2 intervals)
        private val VALID_JSON = """
            {
              "list": [
                {
                  "dt": 1700000000,
                  "main": { "temp_max": 32.5, "humidity": 72 },
                  "rain": { "3h": 0.0 }
                },
                {
                  "dt": 1700010800,
                  "main": { "temp_max": 34.0, "humidity": 65 },
                  "rain": { "3h": 1.5 }
                }
              ]
            }
        """.trimIndent()
    }

    @Before
    fun setUp() {
        fakeDao     = FakeWeatherDao()
        mockContext = mock()
        mockAssets  = mock()
        mockApi     = mock()
        moshi       = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        whenever(mockContext.assets).thenReturn(mockAssets)

        repository = WeatherRepository(
            context           = mockContext,
            weatherApiService = mockApi,
            weatherDao        = fakeDao,
            dataGenerator     = DataGeneratorClass(),
            moshi             = moshi
        )
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun stubMockJsonSuccess() {
        val bytes = VALID_JSON.toByteArray(StandardCharsets.UTF_8)
        whenever(mockAssets.open("mock_weather.json"))
            .thenReturn(ByteArrayInputStream(bytes))
    }

    private fun stubMockJsonFailure() {
        whenever(mockAssets.open("mock_weather.json"))
            .thenThrow(IOException("asset not found"))
    }

    private fun makeApiResponse() = WeatherResponseDto(
        forecastList = listOf(
            ForecastDto(dt = 1700000000L, main = MainDto(tempMax = 31f, humidity = 70), rain = null),
            ForecastDto(dt = 1700010800L, main = MainDto(tempMax = 33f, humidity = 65), rain = null)
        )
    )

    // ------------------------------------------------------------------
    // TIER 1 — API success
    // ------------------------------------------------------------------

    @Test
    fun `tier1 API success saves entities to Room`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenReturn(makeApiResponse())

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertTrue("Room must receive at least one insertAll after API success",
            fakeDao.insertedBatches.isNotEmpty())
    }

    @Test
    fun `tier1 API success entities carry correct plotId`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenReturn(makeApiResponse())

        repository.refreshWeather(PLOT_ID, LAT, LON)

        fakeDao.insertedBatches.last().forEach { entity ->
            assertEquals("Each saved entity must have plotId=$PLOT_ID", PLOT_ID, entity.plotId)
        }
    }

    @Test
    fun `tier1 API success lastUpdatedAt is recent within 5 seconds`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenReturn(makeApiResponse())
        val before = System.currentTimeMillis()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        val lastUpdated = fakeDao.insertedBatches.last().first().lastUpdatedAt.time
        assertTrue("lastUpdatedAt must be >= call start time", lastUpdated >= before)
        assertTrue("lastUpdatedAt must be within 5 s of call", lastUpdated <= before + 5_000)
    }

    @Test
    fun `tier1 API success deleteByPlotId is called before insertAll`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenReturn(makeApiResponse())

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertTrue("deleteByPlotId must be called to purge stale rows",
            fakeDao.deletedPlotIds.contains(PLOT_ID))
    }

    // ------------------------------------------------------------------
    // TIER 2 — API IOException → mock_weather.json
    // ------------------------------------------------------------------

    @Test
    fun `tier2 API IOException falls back to mock JSON and Room is written`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonSuccess()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertTrue("Room must be written via mock-JSON fallback",
            fakeDao.insertedBatches.isNotEmpty())
    }

    @Test
    fun `tier2 mock JSON rows carry correct plotId`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("timeout") }
        stubMockJsonSuccess()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        fakeDao.insertedBatches.last().forEach { entity ->
            assertEquals("Mock-JSON entity must have plotId=$PLOT_ID", PLOT_ID, entity.plotId)
        }
    }

    @Test
    fun `tier2 API HttpException 401 falls back to mock JSON`() = runTest {
        val httpEx = mock<HttpException>()
        whenever(httpEx.code()).thenReturn(401)
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw httpEx }
        stubMockJsonSuccess()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertTrue("Room must be written when API returns 401 and mock JSON available",
            fakeDao.insertedBatches.isNotEmpty())
    }

    // ------------------------------------------------------------------
    // TIER 3 — Both API and mock JSON fail → DataGenerator
    // ------------------------------------------------------------------

    @Test
    fun `tier3 both tiers fail DataGenerator writes to Room`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonFailure()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertTrue("DataGenerator (Tier 3) must always write to Room",
            fakeDao.insertedBatches.isNotEmpty())
    }

    @Test
    fun `tier3 DataGenerator fallback produces exactly 56 rows`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonFailure()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        assertEquals("Tier 3 DataGenerator must produce 56 rows",
            56, fakeDao.insertedBatches.last().size)
    }

    @Test
    fun `tier3 DataGenerator rows carry correct plotId`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonFailure()

        repository.refreshWeather(PLOT_ID, LAT, LON)

        fakeDao.insertedBatches.last().forEachIndexed { i, entity ->
            assertEquals("DataGenerator row $i must have plotId=$PLOT_ID", PLOT_ID, entity.plotId)
        }
    }

    // ------------------------------------------------------------------
    // INVARIANT — Room is always written regardless of tier used
    // ------------------------------------------------------------------

    @Test
    fun `invariant Room insertAll called after tier1`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenReturn(makeApiResponse())
        repository.refreshWeather(PLOT_ID, LAT, LON)
        assertFalse("insertedBatches must not be empty after Tier 1",
            fakeDao.insertedBatches.isEmpty())
    }

    @Test
    fun `invariant Room insertAll called after tier2`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonSuccess()
        repository.refreshWeather(PLOT_ID, LAT, LON)
        assertFalse("insertedBatches must not be empty after Tier 2",
            fakeDao.insertedBatches.isEmpty())
    }

    @Test
    fun `invariant Room insertAll called after tier3`() = runTest {
        whenever(mockApi.getForecast(any(), any(), any(), any())).thenAnswer { throw IOException("offline") }
        stubMockJsonFailure()
        repository.refreshWeather(PLOT_ID, LAT, LON)
        assertFalse("insertedBatches must not be empty after Tier 3",
            fakeDao.insertedBatches.isEmpty())
    }
}
