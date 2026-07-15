package com.networth.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthHistoryDao {
    @Query("SELECT * FROM net_worth_history ORDER BY recordedAt ASC")
    fun getAllHistory(): Flow<List<NetWorthHistoryEntity>>

    @Query("SELECT * FROM net_worth_history ORDER BY recordedAt ASC")
    suspend fun getAllHistoryOnce(): List<NetWorthHistoryEntity>

    @Query("SELECT * FROM net_worth_history ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatest(): NetWorthHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NetWorthHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<NetWorthHistoryEntity>)

    @Query("DELETE FROM net_worth_history")
    suspend fun deleteAll()
}
