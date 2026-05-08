package com.darkplaymc.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "darkplay_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref_new (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO playlist_song_cross_ref_new (playlistId, songId, position)
                    SELECT playlistId, songId, position
                    FROM playlist_song_cross_ref
                    WHERE EXISTS (
                        SELECT 1 FROM playlists WHERE playlists.id = playlist_song_cross_ref.playlistId
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE playlist_song_cross_ref")
                db.execSQL("ALTER TABLE playlist_song_cross_ref_new RENAME TO playlist_song_cross_ref")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_playlistId ON playlist_song_cross_ref(playlistId)")
            }
        }
    }
}
