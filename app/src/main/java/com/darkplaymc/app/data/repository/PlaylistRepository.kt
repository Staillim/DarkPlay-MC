package com.darkplaymc.app.data.repository

import com.darkplaymc.app.data.local.dao.PlaylistDao
import com.darkplaymc.app.data.local.dao.PlaylistWithRefs
import com.darkplaymc.app.data.local.entity.PlaylistEntity
import com.darkplaymc.app.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getAllPlaylistsWithRefs(): Flow<List<PlaylistWithRefs>> =
        playlistDao.getAllPlaylistsWithRefs()

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)

    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.renamePlaylist(id, name)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int = 0) {
        if (!playlistDao.songExistsInPlaylist(playlistId, songId)) {
            playlistDao.insertCrossRef(PlaylistSongCrossRef(playlistId, songId, position))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    suspend fun clearPlaylist(playlistId: Long) = playlistDao.clearPlaylist(playlistId)
}
