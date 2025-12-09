package com.example.clothsearch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_searches")
data class SavedSearch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String?,
    val category: String,
    val colorName: String,
    val queryText: String,
    val timestampMs: Long = System.currentTimeMillis()
)
