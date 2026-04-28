package com.raithabharosahub.data.local.dao

import androidx.room.*
import com.raithabharosahub.data.local.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for SeasonEntity.
 * Provides CRUD operations for cropping seasons.
 */
@Dao
interface SeasonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(season: SeasonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seasons: List<SeasonEntity>)

    @Update
    suspend fun update(season: SeasonEntity)

    @Delete
    suspend fun delete(season: SeasonEntity)

    @Query("DELETE FROM season WHERE id = :seasonId")
    suspend fun deleteById(seasonId: Long)

    @Query("DELETE FROM season WHERE plot_id = :plotId")
    suspend fun deleteByPlotId(plotId: Long)

    @Query("SELECT * FROM season WHERE id = :seasonId")
    suspend fun getById(seasonId: Long): SeasonEntity?

    @Query("SELECT * FROM season WHERE plot_id = :plotId ORDER BY sow_date DESC")
    fun getByPlotId(plotId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM season WHERE plot_id = :plotId AND crop = :crop ORDER BY sow_date DESC")
    fun getByPlotIdAndCrop(plotId: Long, crop: String): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM season WHERE plot_id = :plotId AND sow_date >= :startDate AND sow_date <= :endDate ORDER BY sow_date ASC")
    fun getByPlotIdAndDateRange(plotId: Long, startDate: Date, endDate: Date): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM season WHERE harvest_date IS NULL ORDER BY sow_date DESC")
    fun getOngoingSeasons(): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM season WHERE harvest_date IS NOT NULL ORDER BY harvest_date DESC")
    fun getCompletedSeasons(): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM season WHERE plot_id = :plotId AND harvest_date IS NULL ORDER BY sow_date DESC LIMIT 1")
    suspend fun getCurrentSeasonByPlotId(plotId: Long): SeasonEntity?

    @Query("SELECT * FROM season ORDER BY sow_date DESC")
    fun getAll(): Flow<List<SeasonEntity>>

    @Query("SELECT COUNT(*) FROM season WHERE plot_id = :plotId")
    suspend fun countByPlot(plotId: Long): Int

    @Query("SELECT SUM(yield_kg) FROM season WHERE plot_id = :plotId AND harvest_date IS NOT NULL")
    suspend fun getTotalYieldByPlot(plotId: Long): Float?

    @Query("SELECT AVG(yield_kg) FROM season WHERE plot_id = :plotId AND crop = :crop AND harvest_date IS NOT NULL")
    suspend fun getAverageYieldByCrop(plotId: Long, crop: String): Float?

    @Query("SELECT * FROM season WHERE crop LIKE '%' || :cropQuery || '%' ORDER BY sow_date DESC")
    fun searchByCrop(cropQuery: String): Flow<List<SeasonEntity>>

    @Query("UPDATE season SET harvest_date = :harvestDate, yield_kg = :yieldKg, notes = :notes WHERE id = :seasonId")
    suspend fun completeSeason(seasonId: Long, harvestDate: Date, yieldKg: Float?, notes: String?)
}