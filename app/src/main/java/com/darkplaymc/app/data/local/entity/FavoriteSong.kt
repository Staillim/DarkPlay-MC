package com.darkplaymc.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteSong(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
