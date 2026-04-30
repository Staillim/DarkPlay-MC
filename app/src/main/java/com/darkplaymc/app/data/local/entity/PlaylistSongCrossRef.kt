package com.darkplaymc.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,       // MediaStore song ID
    val position: Int = 0
)
