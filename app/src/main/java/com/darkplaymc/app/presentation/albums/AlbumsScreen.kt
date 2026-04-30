package com.darkplaymc.app.presentation.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.darkplaymc.app.data.model.Album
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

@Composable
fun AlbumsScreen(vm: PlayerViewModel, navController: NavController) {
    val albums by vm.albums.collectAsState()
    val sorted = remember(albums) { albums.sortedByDescending { it.songCount } }

    if (sorted.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.empty_albums),
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        items(sorted, key = { it.id }) { album ->
            AlbumCard(
                album = album,
                onClick = { navController.navigate("album/${album.id}") }
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val thumbUris = remember(album) {
        album.songs.mapNotNull { it.albumArtUri }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Cover art box with mini thumbnail inlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
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
                    modifier = Modifier.size(56.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
            }

            // 2×2 mini thumbnail inlay — bottom-right corner
            if (thumbUris.size >= 4) {
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
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            MiniThumb(thumbUris[0], Modifier.weight(1f).fillMaxHeight().padding(1.dp))
                            MiniThumb(thumbUris[1], Modifier.weight(1f).fillMaxHeight().padding(1.dp))
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            MiniThumb(thumbUris[2], Modifier.weight(1f).fillMaxHeight().padding(1.dp))
                            MiniThumb(thumbUris[3], Modifier.weight(1f).fillMaxHeight().padding(1.dp))
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
    }
}

@Composable
private fun MiniThumb(uri: android.net.Uri, modifier: Modifier) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(3.dp))
    )
}

