package com.emir.oructakibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastRecordDao {
    @Query("SELECT * FROM fast_records")
    fun getAll(): Flow<List<FastRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: FastRecord)

    @Query("DELETE FROM fast_records WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

