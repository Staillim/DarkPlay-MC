package com.darkplaymc.app.presentation.albums

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.data.model.formattedDuration
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel
import com.darkplaymc.app.ui.theme.AccentViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    vm: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val albums by vm.albums.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    val album = remember(albums, albumId) { albums.find { it.id == albumId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (album != null && album.songs.isNotEmpty()) {
                        IconButton(onClick = { vm.playSongs(album.songs.shuffled(), 0) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = AccentViolet)
                        }
                        IconButton(onClick = { vm.playSongs(album.songs, 0) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (album == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_albums), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header with album art
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = album.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(album.name, style = MaterialTheme.typography.titleMedium)
                        Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = AccentViolet)
                        if (album.year > 0) {
                            Text("${album.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${album.songCount} ${stringResource(R.string.label_songs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }

            itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Track number
                    Text(
                        text = if (song.trackNumber > 0) "${song.trackNumber}" else "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (song.id == currentSong?.id) AccentViolet else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.formattedDuration(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { vm.playSongs(album.songs, index) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentViolet)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            }
        }
    }
}
