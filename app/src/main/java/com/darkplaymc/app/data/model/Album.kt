package com.darkplaymc.app.data.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songs: List<Song> = emptyList(),
    val year: Int = 0
) {
    val songCount: Int get() = songs.size
    val coverUri: Uri? get() = songs.firstOrNull()?.albumArtUri
}
