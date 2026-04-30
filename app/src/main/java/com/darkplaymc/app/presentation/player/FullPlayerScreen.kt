package com.darkplaymc.app.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
import com.darkplaymc.app.data.model.PlaybackMode
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel

@Composable
fun FullPlayerScreen(vm: PlayerViewModel) {
    val song by vm.currentSong.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val progress by vm.progress.collectAsState()
    val currentPosition by vm.currentPosition.collectAsState()
    val duration by vm.duration.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val playbackMode by vm.playbackMode.collectAsState()

    var showQueue  by remember { mutableStateOf(false) }
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    val isFavorite  = song?.id?.let { favoriteIds.contains(it) } ?: false
    val isShuffling = playbackMode == PlaybackMode.SHUFFLE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // iOS drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            )

            Spacer(Modifier.height(12.dp))

            // Top bar: chevron-down | Now Playing | queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.closeFullPlayer() }) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_close),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.label_now_playing),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.6f)
                )
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = stringResource(R.string.action_show_queue),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Album art — full width, cinematic shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(elevation = 40.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                if (song?.albumArtUri != null) {
                    AsyncImage(
                        model = song!!.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Song title + artist + heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = song?.title ?: "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3f).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "—",
                        fontSize = 16.sp,
                        letterSpacing = (-0.2f).sp,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { song?.id?.let { vm.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress bar with white thumb
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val f = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            vm.seekTo(f)
                            drag(down.id) { ch ->
                                ch.consume()
                                val ff = (ch.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                vm.seekTo(ff)
                            }
                        }
                    }
            ) {
                val pct        = progress.coerceIn(0f, 1f)
                val thumbSize  = 14.dp
                val thumbOffset = (maxWidth * pct - thumbSize / 2).coerceIn(0.dp, maxWidth - thumbSize)

                // Track background
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct)
                            .background(Color.White)
                    )
                }

                // Thumb with soft glow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbOffset)
                        .size(thumbSize)
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f),
                                radius = size.maxDimension * 0.85f
                            )
                        }
                        .background(Color.White, CircleShape)
                )
            }

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatMillis(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f))
                Text(text = formatMillis(duration),        fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f))
            }

            Spacer(Modifier.height(28.dp))

            // Transport: Shuffle | Prev | ● Play/Pause | Next | Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle toggle
                IconButton(
                    onClick = {
                        vm.setPlaybackMode(
                            if (isShuffling) PlaybackMode.LIST else PlaybackMode.SHUFFLE
                        )
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Shuffle, null,
                        tint = if (isShuffling) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Skip Previous
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.action_previous),
                        tint = Color.White, modifier = Modifier.size(34.dp)
                    )
                }

                // Large circular Play/Pause — iOS style
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.10f),
                                radius = size.maxDimension * 0.72f
                            )
                        }
                        .background(Color.White, CircleShape)
                        .clickable { vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying)
                            stringResource(R.string.action_pause)
                        else
                            stringResource(R.string.action_play),
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Skip Next
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.action_next),
                        tint = Color.White, modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat / Loop cycle
                IconButton(
                    onClick = {
                        vm.setPlaybackMode(
                            when (playbackMode) {
                                PlaybackMode.LIST    -> PlaybackMode.LOOP
                                PlaybackMode.LOOP    -> PlaybackMode.LIST
                                PlaybackMode.SHUFFLE -> PlaybackMode.LOOP
                            }
                        )
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (playbackMode == PlaybackMode.LOOP)
                            Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (playbackMode != PlaybackMode.SHUFFLE) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showQueue) {
        QueueBottomSheet(vm = vm, onDismiss = { showQueue = false })
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
