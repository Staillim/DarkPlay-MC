package com.darkplaymc.app.data.local.dao

import androidx.room.*
import com.darkplaymc.app.data.local.entity.FavoriteSong
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun getFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSong)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE songId = :songId")
    fun isFavorite(songId: Long): Flow<Boolean>
}
