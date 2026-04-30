package com.darkplaymc.app.presentation.artists

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
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel
import com.darkplaymc.app.ui.theme.AccentViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    vm: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val artists by vm.artists.collectAsState()
    val currentSong by vm.currentSong.collectAsState()

    val artist = remember(artists, artistName) { artists.find { it.name == artistName } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (artist != null && artist.songs.isNotEmpty()) {
                        IconButton(onClick = {
                            vm.playSongs(artist.songs.shuffled(), 0)
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = AccentViolet)
                        }
                        IconButton(onClick = { vm.playSongs(artist.songs, 0) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (artist == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_artists), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Albums section header
            if (artist.albums.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_albums),
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentViolet,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                artist.albums.forEach { album ->
                    item(key = "album_${album.id}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = album.coverUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(album.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${album.songCount} ${stringResource(R.string.label_songs)}${if (album.year > 0) " • ${album.year}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.playSongs(album.songs, 0) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentViolet)
                            }
                        }
                    }
                }
            }

            // Songs section header
            item {
                Text(
                    text = stringResource(R.string.label_songs),
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentViolet,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(artist.songs, key = { _, s -> s.id }) { index, song ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (song.id == currentSong?.id) AccentViolet else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.8f)
                    )
                    IconButton(onClick = { vm.playSongs(artist.songs, index) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentViolet)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            }
        }
    }
}
