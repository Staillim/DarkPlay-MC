package com.darkplaymc.app.data.model

import android.net.Uri

data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song> = emptyList(),
    val isFavorites: Boolean = false
) {
    val songCount: Int get() = songs.size
    val coverUris: List<Uri?> get() = songs.take(4).map { it.albumArtUri }
    /** Album art of the last song added to this playlist (used as playlist cover). */
    val lastCoverUri: Uri? get() = songs.lastOrNull()?.albumArtUri
}
