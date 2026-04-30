package com.darkplaymc.app.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

@Composable
fun MiniPlayerBar(vm: PlayerViewModel) {
    val song        by vm.currentSong.collectAsState()
    val isPlaying   by vm.isPlaying.collectAsState()
    val progress    by vm.progress.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()

    song ?: return

    val isFavorite = song?.id?.let { favoriteIds.contains(it) } ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDrag = 0f
                        var tapped = true
                        drag(down.id) { change ->
                            totalDrag += change.position.y - change.previousPosition.y
                            tapped = false
                        }
                        if (tapped || totalDrag < -80f) vm.openFullPlayer()
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square rounded album art — iOS style
            AsyncImage(
                model = song?.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2E))
            )

            Spacer(Modifier.width(14.dp))

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2f).sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artist ?: "",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Heart / Favorite
            IconButton(onClick = { song?.id?.let { vm.toggleFavorite(it) } }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play / Pause
            IconButton(onClick = { vm.togglePlayPause() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying)
                        stringResource(R.string.action_pause)
                    else
                        stringResource(R.string.action_play),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Thin progress line at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color.White.copy(alpha = 0.75f))
            )
        }
    }
}
