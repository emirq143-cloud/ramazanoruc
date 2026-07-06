package com.emir.oructakibi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fast_records")
data class FastRecord(
    @PrimaryKey val date: String,
    val type: Int // 1 = tuttu, 2 = kaza
)

