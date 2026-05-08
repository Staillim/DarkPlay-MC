package com.darkplaymc.app.presentation.artists

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.data.model.Artist
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

private enum class ArtistViewMode { GRID, LIST }

@Composable
fun ArtistsScreen(vm: PlayerViewModel, navController: NavController) {
    val artists by vm.artists.collectAsState()
    val sorted = remember(artists) { artists.sortedByDescending { it.songCount } }
    var viewMode by rememberSaveable { mutableStateOf(ArtistViewMode.GRID) }

    if (sorted.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.empty_artists),
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sorted.size} ${stringResource(R.string.tab_artists)}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewToggleBtn(
                    icon = Icons.Default.GridView,
                    label = stringResource(R.string.view_grid),
                    selected = viewMode == ArtistViewMode.GRID,
                    onClick = { viewMode = ArtistViewMode.GRID }
                )
                ViewToggleBtn(
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    label = stringResource(R.string.view_list),
                    selected = viewMode == ArtistViewMode.LIST,
                    onClick = { viewMode = ArtistViewMode.LIST }
                )
            }
        }

        if (viewMode == ArtistViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sorted, key = { it.name }) { artist ->
                    ArtistCard(
                        artist = artist,
                        onClick = { navController.navigate("artist/${Uri.encode(artist.name)}") }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sorted, key = { it.name }) { artist ->
                    ArtistListRow(
                        artist = artist,
                        onClick = { navController.navigate("artist/${Uri.encode(artist.name)}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewToggleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ArtistCard(artist: Artist, onClick: () -> Unit) {
    val collageUris = remember(artist) {
        artist.songs.mapNotNull { it.albumArtUri }
            .distinctBy { it.toString() }
            .take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            if (collageUris.size >= 4) {
                // 2×2 collage clipped to circle
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ArtCollageCell(collageUris[0], Modifier.weight(1f).fillMaxHeight())
                        ArtCollageCell(collageUris[1], Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ArtCollageCell(collageUris[2], Modifier.weight(1f).fillMaxHeight())
                        ArtCollageCell(collageUris[3], Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else if (collageUris.isNotEmpty()) {
                AsyncImage(
                    model = collageUris.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
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
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${artist.songCount} ${stringResource(R.string.label_songs)}",
            fontSize = 12.sp,
            letterSpacing = (-0.1f).sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ArtistListRow(artist: Artist, onClick: () -> Unit) {
    val coverUri = remember(artist) { artist.songs.firstOrNull { it.albumArtUri != null }?.albumArtUri }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2f).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.songCount} ${stringResource(R.string.label_songs)}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ArtCollageCell(uri: android.net.Uri, modifier: Modifier) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
