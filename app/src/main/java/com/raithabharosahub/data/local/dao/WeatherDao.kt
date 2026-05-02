package com.raithabharosahub.data.local.dao

import androidx.room.*
import com.raithabharosahub.data.local.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for WeatherEntity.
 * Provides CRUD operations and query helpers for weather data.
 */
@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: WeatherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(weatherList: List<WeatherEntity>)

    @Update
    suspend fun update(weather: WeatherEntity)

    @Delete
    suspend fun delete(weather: WeatherEntity)

    @Query("DELETE FROM weather WHERE id = :weatherId")
    suspend fun deleteById(weatherId: Long)

    @Query("DELETE FROM weather WHERE plot_id = :plotId")
    suspend fun deleteByPlotId(plotId: Long)

    @Query("DELETE FROM weather WHERE fetched_at < :thresholdDate")
    suspend fun deleteOlderThan(thresholdDate: Date)

    @Query("SELECT * FROM weather WHERE id = :weatherId")
    suspend fun getById(weatherId: Long): WeatherEntity?

    /** Primary UI query — observe all forecast rows for a plot, newest first. */
    @Query("SELECT * FROM weather WHERE plot_id = :plotId ORDER BY date ASC")
    fun getByPlotId(plotId: Long): Flow<List<WeatherEntity>>

    @Query("SELECT * FROM weather WHERE plot_id = :plotId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getByPlotIdAndDateRange(plotId: Long, startDate: Date, endDate: Date): Flow<List<WeatherEntity>>

    @Query("SELECT * FROM weather WHERE plot_id = :plotId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestByPlotId(plotId: Long): WeatherEntity?

    @Query("SELECT * FROM weather WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<WeatherEntity>>

    @Query("SELECT COUNT(*) FROM weather WHERE plot_id = :plotId")
    suspend fun countByPlot(plotId: Long): Int

    @Query("SELECT AVG(temp_max) FROM weather WHERE plot_id = :plotId AND date >= :startDate AND date <= :endDate")
    suspend fun getAverageTemperature(plotId: Long, startDate: Date, endDate: Date): Float?

    @Query("SELECT SUM(rain_mm) FROM weather WHERE plot_id = :plotId AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalRainfall(plotId: Long, startDate: Date, endDate: Date): Float?

    /**
     * Returns the most recent [WeatherEntity.lastUpdatedAt] for a plot.
     * The UI uses this to display "Last updated X minutes ago" staleness text.
     * Returns null if no data exists yet for the plot.
     */
    @Query("SELECT last_updated_at FROM weather WHERE plot_id = :plotId ORDER BY last_updated_at DESC LIMIT 1")
    suspend fun getLastUpdatedAt(plotId: Long): Date?

    /**
     * Same as [getLastUpdatedAt] but as a Flow so the UI auto-refreshes
     * whenever new data is written.
     */
    @Query("SELECT last_updated_at FROM weather WHERE plot_id = :plotId ORDER BY last_updated_at DESC LIMIT 1")
    fun observeLastUpdatedAt(plotId: Long): Flow<Date?>
}