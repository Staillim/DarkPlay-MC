package com.darkplaymc.app.data.model

import android.net.Uri

data class Artist(
    val name: String,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList()
) {
    val songCount: Int get() = songs.size
    val albumCount: Int get() = albums.size
    val coverUri: Uri? get() = songs.firstOrNull()?.albumArtUri
}
