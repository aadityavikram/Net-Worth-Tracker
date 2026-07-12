package com.networth.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY updatedAt DESC")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY updatedAt DESC")
    suspend fun getAllBankAccountsOnce(): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountById(id: Long): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bankAccount: BankAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bankAccounts: List<BankAccountEntity>)

    @Update
    suspend fun update(bankAccount: BankAccountEntity)

    @Delete
    suspend fun delete(bankAccount: BankAccountEntity)

    @Delete
    suspend fun delete(bankAccounts: List<BankAccountEntity>)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()
}
