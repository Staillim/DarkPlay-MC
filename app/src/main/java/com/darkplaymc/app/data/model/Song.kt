package com.darkplaymc.app.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val albumArtUri: Uri?,
    val duration: Long,
    val path: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = 0
)

fun Song.formattedDuration(): String {
    val totalSec = duration / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
