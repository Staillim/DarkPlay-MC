package com.darkplaymc.app.presentation.songs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.data.model.Song
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    vm: PlayerViewModel,
    navController: NavController,
    searchQuery: String = "",
    sortMode: SongSortMode = SongSortMode.RECENT
) {
    val allSongs by vm.allSongs.collectAsState()
    val currentSong by vm.currentSong.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()

    val displayedSongs = remember(allSongs, searchQuery, sortMode) {
        val filtered = if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
        when (sortMode) {
            SongSortMode.RECENT -> filtered.sortedByDescending { it.dateAdded }
            SongSortMode.TITLE  -> filtered.sortedBy { it.title }
            SongSortMode.ARTIST -> filtered.sortedBy { it.artist }
            SongSortMode.ALBUM  -> filtered.sortedBy { it.album }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (displayedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) stringResource(R.string.empty_songs)
                    else stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(displayedSongs, key = { _, s -> s.id }) { index, song ->
                    SongItem(
                        song = song,
                        isPlaying = song.id == currentSong?.id,
                        isFavorite = favoriteIds.contains(song.id),
                        playlists = playlists.map { it.id to it.name },
                        onPlay = { vm.playSongs(displayedSongs, index) },
                        onAddToQueue = { vm.addToQueueAtEnd(song) },
                        onAddToQueueNext = { vm.addToQueueNext(song) },
                        onToggleFavorite = { vm.toggleFavorite(song.id) },
                        onAddToPlaylist = { playlistId -> vm.addSongToPlaylist(playlistId, song.id) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(start = 76.dp, end = 16.dp)
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
            }
        }
    }
}

// Song item: click = play, long-press = context menu
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItem(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    playlists: List<Pair<Long, String>>,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToQueueNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onPlay() },
                    onLongClick = { showMenu = true }
                )
                .background(
                    if (isPlaying) Color.White.copy(alpha = 0.05f)
                    else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = if (isPlaying) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                    letterSpacing = (-0.2f).sp,
                    color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    letterSpacing = (-0.1f).sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_add_to_queue)) },
                leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                onClick = { onAddToQueue(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_play_next)) },
                leadingIcon = { Icon(Icons.Default.QueuePlayNext, null) },
                onClick = { onAddToQueueNext(); showMenu = false }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) stringResource(R.string.action_remove_favorite)
                        else stringResource(R.string.action_add_favorite)
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) Color.White else LocalContentColor.current
                    )
                },
                onClick = { onToggleFavorite(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_add_to_playlist)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                onClick = { showAddToPlaylist = true; showMenu = false }
            )
        }
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylist = false },
            onSelect = { id -> onAddToPlaylist(id); showAddToPlaylist = false }
        )
    }
}

// Add-to-playlist dialog
@Composable
fun AddToPlaylistDialog(
    playlists: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_to_playlist)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists))
            } else {
                Column {
                    playlists.forEach { (id, name) ->
                        TextButton(
                            onClick = { onSelect(id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

enum class SongSortMode(val label: String) {
    RECENT("Recientes"),
    TITLE("Titulo"),
    ARTIST("Artista"),
    ALBUM("Album")
}
