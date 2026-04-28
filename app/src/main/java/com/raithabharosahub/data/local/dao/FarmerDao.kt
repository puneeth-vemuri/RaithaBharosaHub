package com.raithabharosahub.data.local.dao

import androidx.room.*
import com.raithabharosahub.data.local.entity.FarmerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FarmerEntity.
 * Provides CRUD operations for farmer profiles.
 */
@Dao
interface FarmerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farmer: FarmerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(farmers: List<FarmerEntity>)

    @Update
    suspend fun update(farmer: FarmerEntity)

    @Delete
    suspend fun delete(farmer: FarmerEntity)

    @Query("DELETE FROM farmer WHERE id = :farmerId")
    suspend fun deleteById(farmerId: Long)

    @Query("SELECT * FROM farmer WHERE id = :farmerId")
    suspend fun getById(farmerId: Long): FarmerEntity?

    @Query("SELECT * FROM farmer WHERE mobile = :mobile")
    suspend fun getByMobile(mobile: String): FarmerEntity?

    @Query("SELECT * FROM farmer ORDER BY name ASC")
    fun getAll(): Flow<List<FarmerEntity>>

    @Query("SELECT COUNT(*) FROM farmer")
    suspend fun count(): Int

    @Query("SELECT * FROM farmer WHERE district = :district ORDER BY name ASC")
    fun getByDistrict(district: String): Flow<List<FarmerEntity>>

    @Query("SELECT * FROM farmer WHERE primary_crop = :crop ORDER BY name ASC")
    fun getByCrop(crop: String): Flow<List<FarmerEntity>>
}