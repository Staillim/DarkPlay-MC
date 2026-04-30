package com.darkplaymc.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.darkplaymc.app.data.local.dao.FavoriteDao
import com.darkplaymc.app.data.local.dao.PlaylistDao
import com.darkplaymc.app.data.local.entity.FavoriteSong
import com.darkplaymc.app.data.local.entity.PlaylistEntity
import com.darkplaymc.app.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteSong::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "darkplay_db"
    }
}
