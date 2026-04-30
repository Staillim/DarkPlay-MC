package com.darkplaymc.app.data.local.dao

import androidx.room.*
import com.darkplaymc.app.data.local.entity.PlaylistEntity
import com.darkplaymc.app.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

data class PlaylistWithRefs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val refs: List<PlaylistSongCrossRef>
)

@Dao
interface PlaylistDao {

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsWithRefs(): Flow<List<PlaylistWithRefs>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) > 0 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun songExistsInPlaylist(playlistId: Long, songId: Long): Boolean
}
