package com.raithabharosahub.data.local.dao

import androidx.room.*
import com.raithabharosahub.data.local.entity.PlotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PlotEntity.
 * Provides CRUD operations for farming plots.
 */
@Dao
interface PlotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plot: PlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plots: List<PlotEntity>)

    @Update
    suspend fun update(plot: PlotEntity)

    @Delete
    suspend fun delete(plot: PlotEntity)

    @Query("DELETE FROM plot WHERE id = :plotId")
    suspend fun deleteById(plotId: Long)

    @Query("DELETE FROM plot WHERE farmer_id = :farmerId")
    suspend fun deleteByFarmerId(farmerId: Long)

    @Query("SELECT * FROM plot WHERE id = :plotId")
    suspend fun getById(plotId: Long): PlotEntity?

    @Query("SELECT * FROM plot WHERE farmer_id = :farmerId ORDER BY label ASC")
    fun getByFarmerId(farmerId: Long): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plot ORDER BY label ASC")
    fun getAll(): Flow<List<PlotEntity>>

    @Query("SELECT COUNT(*) FROM plot WHERE farmer_id = :farmerId")
    suspend fun countByFarmer(farmerId: Long): Int

    @Query("SELECT * FROM plot WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
    fun getPlotsInArea(latMin: Double, latMax: Double, lonMin: Double, lonMax: Double): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plot WHERE label LIKE '%' || :query || '%' ORDER BY label ASC")
    fun searchByLabel(query: String): Flow<List<PlotEntity>>
}