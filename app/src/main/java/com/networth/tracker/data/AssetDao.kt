package com.networth.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    suspend fun getAllAssetsOnce(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()

    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Long): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Delete
    suspend fun delete(asset: AssetEntity)

    @Delete
    suspend fun delete(assets: List<AssetEntity>)

    @Query("DELETE FROM assets WHERE category = :category")
    suspend fun deleteByCategory(category: AssetCategory)
}
