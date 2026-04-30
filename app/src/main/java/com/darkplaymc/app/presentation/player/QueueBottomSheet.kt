package com.darkplaymc.app.presentation.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkplaymc.app.R
import com.darkplaymc.app.data.model.Song
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel
import com.darkplaymc.app.ui.theme.AccentViolet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    vm: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val queue by vm.queue.collectAsState()
    val currentQueueIndex by vm.currentQueueIndex.collectAsState()

    // Local copy for immediate visual feedback during drag
    var localQueue by remember { mutableStateOf(queue) }
    var isDraggingActive by remember { mutableStateOf(false) }
    var dragFromIndex by remember { mutableStateOf(-1) }
    var dragToIndex by remember { mutableStateOf(-1) }
    var pendingCommitJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // Sync from VM only when not actively dragging
    LaunchedEffect(queue) {
        if (!isDraggingActive) localQueue = queue
    }

    val lazyListState = rememberLazyListState()

    // Auto-scroll to currently playing song when sheet opens
    LaunchedEffect(Unit) {
        if (currentQueueIndex >= 0) lazyListState.animateScrollToItem(currentQueueIndex)
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            if (!isDraggingActive) {
                isDraggingActive = true
                dragFromIndex = from.index
            }
            dragToIndex = to.index
            localQueue = localQueue.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            // Debounce: commit to ExoPlayer 300ms after last onMove
            pendingCommitJob?.cancel()
            pendingCommitJob = scope.launch {
                delay(300)
                vm.moveQueueItem(dragFromIndex, dragToIndex)
                isDraggingActive = false
                dragFromIndex = -1
                dragToIndex = -1
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.65f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_queue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    // Remove all items except the currently playing one
                    val size = localQueue.size
                    for (i in size - 1 downTo 0) {
                        if (i != currentQueueIndex) vm.removeFromQueue(i)
                    }
                }) {
                    Text(stringResource(R.string.action_clear_queue), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            if (localQueue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.empty_queue),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = localQueue,
                    key = { _, song -> song.id }
                    ) { index, song ->
                        val isCurrentItem = index == currentQueueIndex
                        ReorderableItem(
                            state = reorderableState,
                            key = song.id
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 8.dp else 0.dp,
                                label = "drag_elevation"
                            )
                            QueueSongItem(
                                song = song,
                                isCurrentItem = isCurrentItem,
                                isDragging = isDragging,
                                elevation = elevation,
                                onRemove = { vm.removeFromQueue(index) },
                                dragHandleModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongItem(
    song: Song,
    isCurrentItem: Boolean,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = elevation,
        color = if (isCurrentItem)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.action_drag),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier.padding(end = 8.dp)
            )

            // Album art
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentItem) AccentViolet else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCurrentItem) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            // "Now playing" badge
            if (isCurrentItem) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = AccentViolet,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
            }

            // Remove from queue
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_remove_from_queue),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
