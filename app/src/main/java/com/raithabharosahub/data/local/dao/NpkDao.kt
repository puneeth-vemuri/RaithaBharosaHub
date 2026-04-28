package com.raithabharosahub.data.local.dao

import androidx.room.*
import com.raithabharosahub.data.local.entity.NpkEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for NpkEntity.
 * Provides CRUD operations for soil test results.
 * Personal data in this table is encrypted using SQLCipher.
 */
@Dao
interface NpkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(npk: NpkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(npkList: List<NpkEntity>)

    @Update
    suspend fun update(npk: NpkEntity)

    @Delete
    suspend fun delete(npk: NpkEntity)

    @Query("DELETE FROM npk WHERE id = :npkId")
    suspend fun deleteById(npkId: Long)

    @Query("DELETE FROM npk WHERE plot_id = :plotId")
    suspend fun deleteByPlotId(plotId: Long)

    @Query("SELECT * FROM npk WHERE id = :npkId")
    suspend fun getById(npkId: Long): NpkEntity?

    @Query("SELECT * FROM npk WHERE plot_id = :plotId ORDER BY test_date DESC")
    fun getByPlotId(plotId: Long): Flow<List<NpkEntity>>

    @Query("SELECT * FROM npk WHERE plot_id = :plotId AND test_date >= :startDate AND test_date <= :endDate ORDER BY test_date ASC")
    fun getByPlotIdAndDateRange(plotId: Long, startDate: Date, endDate: Date): Flow<List<NpkEntity>>

    @Query("SELECT * FROM npk WHERE plot_id = :plotId ORDER BY test_date DESC LIMIT 1")
    suspend fun getLatestByPlotId(plotId: Long): NpkEntity?

    @Query("SELECT * FROM npk ORDER BY test_date DESC")
    fun getAll(): Flow<List<NpkEntity>>

    @Query("SELECT COUNT(*) FROM npk WHERE plot_id = :plotId")
    suspend fun countByPlot(plotId: Long): Int

    @Query("SELECT AVG(nitrogen) FROM npk WHERE plot_id = :plotId")
    suspend fun getAverageNitrogen(plotId: Long): Float?

    @Query("SELECT AVG(phosphorus) FROM npk WHERE plot_id = :plotId")
    suspend fun getAveragePhosphorus(plotId: Long): Float?

    @Query("SELECT AVG(potassium) FROM npk WHERE plot_id = :plotId")
    suspend fun getAveragePotassium(plotId: Long): Float?

    @Query("SELECT * FROM npk WHERE nitrogen < :threshold OR phosphorus < :threshold OR potassium < :threshold ORDER BY test_date DESC")
    fun getBelowThreshold(threshold: Float): Flow<List<NpkEntity>>

    @Query("SELECT * FROM npk WHERE lab_name LIKE '%' || :labQuery || '%' ORDER BY test_date DESC")
    fun searchByLabName(labQuery: String): Flow<List<NpkEntity>>
}