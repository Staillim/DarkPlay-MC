package com.darkplaymc.app.data.repository

import com.darkplaymc.app.data.local.dao.FavoriteDao
import com.darkplaymc.app.data.local.entity.FavoriteSong
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    fun getFavoriteIds(): Flow<List<Long>> = favoriteDao.getFavoriteIds()

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun addFavorite(songId: Long) =
        favoriteDao.addFavorite(FavoriteSong(songId = songId))

    suspend fun removeFavorite(songId: Long) = favoriteDao.removeFavorite(songId)

    suspend fun toggleFavorite(songId: Long, currentlyFavorite: Boolean) {
        if (currentlyFavorite) removeFavorite(songId) else addFavorite(songId)
    }
}
