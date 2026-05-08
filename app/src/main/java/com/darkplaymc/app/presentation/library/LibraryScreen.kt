package com.darkplaymc.app.presentation.library

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.data.model.Album
import com.darkplaymc.app.data.model.Artist
import com.darkplaymc.app.data.model.Playlist
import com.darkplaymc.app.data.model.Song
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

private enum class PlaylistViewMode { GRID, LIST }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(vm: PlayerViewModel, navController: NavController) {
    val allSongs by vm.allSongs.collectAsState()
    val currentSong by vm.currentSong.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favoritesPlaylist by vm.favoritesPlaylist.collectAsState()
    val artists by vm.artists.collectAsState()
    val albums by vm.albums.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()

    var playlistViewMode by rememberSaveable { mutableStateOf(PlaylistViewMode.GRID) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val sortedSongs = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }
    }

    val allPlaylists = remember(playlists, favoritesPlaylist) {
        buildList {
            add(favoritesPlaylist)
            addAll(playlists)
        }
    }

    val sortedAlbums = remember(albums) {
        albums.sortedByDescending { it.songCount }
    }

    val sortedArtists = remember(artists) {
        artists.sortedByDescending { it.songCount }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            // ── CANCIONES SECTION ──────────────────────────────────────────────
            item(key = "sec_songs") {
                LibrarySectionHeader(title = stringResource(R.string.tab_songs))
            }

            itemsIndexed(
                items = sortedSongs,
                key = { _, song -> "song_${song.id}" }
            ) { index, song ->
                LibrarySongRow(
                    song = song,
                    isPlaying = song.id == currentSong?.id,
                    onClick = { vm.playSongs(sortedSongs, index) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(start = 76.dp, end = 16.dp)
                        .background(Color.White.copy(alpha = 0.07f))
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            // ── PLAYLISTS SECTION ──────────────────────────────────────────────
            item {
                LibrarySectionHeader(title = stringResource(R.string.tab_playlists)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ViewModeToggleButton(
                            icon = Icons.Default.GridView,
                            label = stringResource(R.string.view_grid),
                            selected = playlistViewMode == PlaylistViewMode.GRID,
                            onClick = { playlistViewMode = PlaylistViewMode.GRID }
                        )
                        ViewModeToggleButton(
                            icon = Icons.AutoMirrored.Filled.ViewList,
                            label = stringResource(R.string.view_list),
                            selected = playlistViewMode == PlaylistViewMode.LIST,
                            onClick = { playlistViewMode = PlaylistViewMode.LIST }
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable { showCreateDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.action_new_playlist),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (playlistViewMode == PlaylistViewMode.GRID) {
                val rows = allPlaylists.chunked(2)
                items(rows, key = { row -> "pgrid_${row.first().id}" }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { playlist ->
                            PlaylistGridCard(
                                playlist = playlist,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("playlist/${playlist.id}") },
                                onDelete = if (playlist.isFavorites) null else ({ vm.deletePlaylist(playlist.id) })
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(allPlaylists, key = { "plist_${it.id}" }) { playlist ->
                    PlaylistListRow(
                        playlist = playlist,
                        onClick = { navController.navigate("playlist/${playlist.id}") },
                        onDelete = if (playlist.isFavorites) null else ({ vm.deletePlaylist(playlist.id) })
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // ── ÁLBUMES SECTION ────────────────────────────────────────────────
            item(key = "sec_albums") { LibrarySectionHeader(title = stringResource(R.string.tab_albums)) }

            val albumRows = sortedAlbums.chunked(2)
            items(albumRows, key = { row -> "albrow_${row.first().id}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { album ->
                        AlbumCard(
                            album = album,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("album/${album.id}") }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // ── ARTISTAS SECTION ───────────────────────────────────────────────
            item(key = "sec_artists") { LibrarySectionHeader(title = stringResource(R.string.tab_artists)) }

            val artistRows = sortedArtists.chunked(2)
            items(artistRows, key = { row -> "arow_${row.first().name}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { artist ->
                        ArtistCard(
                            artist = artist,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("artist/${Uri.encode(artist.name)}") }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }

        // Top gradient fade — depth / motion blur illusion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent)
                    )
                )
        )

        // Bottom gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                vm.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

// ─── Song Row ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .background(if (isPlaying) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square album art thumbnail
        Box(
            modifier = Modifier
                .size(55.dp)
                .shadow(4.dp, RoundedCornerShape(7.dp), spotColor = Color.Black)
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

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = (-0.2f).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 13.sp,
                letterSpacing = (-0.1f).sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun LibrarySectionHeader(
    title: String,
    actions: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 14.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4f).sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (actions != null) actions()
    }
}

// ─── View Mode Toggle Button ──────────────────────────────────────────────────

@Composable
private fun ViewModeToggleButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "vmbg_$label"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.35f),
        animationSpec = tween(200),
        label = "vmtint_$label"
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(17.dp)
        )
    }
}

// ─── Playlist Grid Card ───────────────────────────────────────────────────────

@Composable
private fun PlaylistGridCard(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            when {
                playlist.isFavorites && playlist.lastCoverUri != null -> AsyncImage(
                    model = playlist.lastCoverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                playlist.isFavorites -> Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
                playlist.lastCoverUri != null -> AsyncImage(
                    model = playlist.lastCoverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(42.dp)
                )
            }

            // Heart badge on Favorites when showing art cover
            if (playlist.isFavorites && playlist.lastCoverUri != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Delete menu trigger (top-right)
            if (onDelete != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = playlist.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2f).sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = "${playlist.songCount} ${stringResource(R.string.label_songs)}",
            fontSize = 12.sp,
            letterSpacing = (-0.1f).sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ─── Playlist List Row ────────────────────────────────────────────────────────

@Composable
private fun PlaylistListRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            when {
                playlist.isFavorites && playlist.lastCoverUri != null -> AsyncImage(
                    model = playlist.lastCoverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                playlist.isFavorites -> Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                playlist.lastCoverUri != null -> AsyncImage(
                    model = playlist.lastCoverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2f).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} ${stringResource(R.string.label_songs)}",
                fontSize = 13.sp,
                letterSpacing = (-0.1f).sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        if (onDelete != null) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Artist Card ──────────────────────────────────────────────────────────────

@Composable
private fun ArtistCard(
    artist: Artist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Collect distinct album art URIs for the 2×2 collage
    val artUris: List<Uri?> = remember(artist.songs) {
        artist.songs
            .mapNotNull { it.albumArtUri }
            .distinctBy { it.toString() }
            .take(4)
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(12.dp, CircleShape, spotColor = Color.Black)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            when {
                artUris.isEmpty() -> Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(52.dp)
                )
                artUris.size == 1 -> AsyncImage(
                    model = artUris[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    // 2×2 collage
                    val grid = (artUris + listOf<Uri?>(null, null, null, null)).take(4)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ArtCollageCell(grid[0], Modifier.weight(1f).fillMaxHeight())
                            ArtCollageCell(grid[1], Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ArtCollageCell(grid[2], Modifier.weight(1f).fillMaxHeight())
                            ArtCollageCell(grid[3], Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2f).sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${artist.songCount} ${stringResource(R.string.label_songs)}",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ArtCollageCell(uri: Uri?, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF2C2C2E))
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─── Album Card ───────────────────────────────────────────────────────────────

@Composable
private fun AlbumCard(
    album: Album,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val thumbUris: List<Uri?> = remember(album.songs) {
        album.songs
            .mapNotNull { it.albumArtUri }
            .take(4)
    }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1C1E))
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Album,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.Center)
                )
            }

            // Mini 2×2 thumbnail inlay — bottom-right corner
            if (thumbUris.isNotEmpty()) {
                val grid = (thumbUris + listOf<Uri?>(null, null, null, null)).take(4)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(50.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            MiniThumb(grid[0], Modifier.weight(1f).fillMaxHeight())
                            MiniThumb(grid[1], Modifier.weight(1f).fillMaxHeight())
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            MiniThumb(grid[2], Modifier.weight(1f).fillMaxHeight())
                            MiniThumb(grid[3], Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = album.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2f).sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = album.artist,
            fontSize = 12.sp,
            letterSpacing = (-0.1f).sp,
            color = Color.White.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MiniThumb(uri: Uri?, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2C2C2E))
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─── Create Playlist Dialog ───────────────────────────────────────────────────

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_playlist_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
