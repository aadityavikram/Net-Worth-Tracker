package com.networth.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetTransactionDao {
    @Query("SELECT * FROM asset_transactions ORDER BY dateMillis ASC, id ASC")
    fun getAll(): Flow<List<AssetTransactionEntity>>

    @Query("SELECT * FROM asset_transactions ORDER BY dateMillis ASC, id ASC")
    suspend fun getAllOnce(): List<AssetTransactionEntity>

    @Query(
        """
        SELECT * FROM asset_transactions
        WHERE assetId = :assetId
        ORDER BY dateMillis DESC, id DESC
        """
    )
    fun getForAsset(assetId: Long): Flow<List<AssetTransactionEntity>>

    @Query(
        """
        SELECT * FROM asset_transactions
        WHERE assetId = :assetId
        ORDER BY dateMillis ASC, id ASC
        """
    )
    suspend fun getForAssetOnce(assetId: Long): List<AssetTransactionEntity>

    @Query("SELECT * FROM asset_transactions WHERE id = :id")
    suspend fun getById(id: Long): AssetTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AssetTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AssetTransactionEntity>)

    @Update
    suspend fun update(entity: AssetTransactionEntity)

    @Delete
    suspend fun delete(entity: AssetTransactionEntity)

    @Query("DELETE FROM asset_transactions WHERE assetId = :assetId")
    suspend fun deleteForAsset(assetId: Long)

    @Query("DELETE FROM asset_transactions WHERE assetId IN (:assetIds)")
    suspend fun deleteForAssets(assetIds: List<Long>)

    @Query("DELETE FROM asset_transactions")
    suspend fun deleteAll()
}
