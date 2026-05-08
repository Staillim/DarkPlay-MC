package com.darkplaymc.app.presentation.viewmodel

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.darkplaymc.app.data.model.*
import com.darkplaymc.app.data.repository.FavoritesRepository
import com.darkplaymc.app.data.repository.MediaStoreRepository
import com.darkplaymc.app.data.repository.PlaylistRepository
import com.darkplaymc.app.service.MusicPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PROGRESS_UPDATE_MS = 500L

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository,
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    // ─── Songs from MediaStore ───────────────────────────────────────────────
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    /** Fast ID → Song lookup, rebuilt whenever allSongs changes. */
    private val songMap = mutableMapOf<Long, Song>()

    // ─── MediaController state ───────────────────────────────────────────────
    private var controller: MediaController? = null
    private var playbackSource: List<Song> = emptyList()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(1L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    /** Progress from 0.0 to 1.0 */
    val progress: StateFlow<Float> = combine(_currentPosition, _duration) { pos, dur ->
        if (dur > 0) pos.toFloat() / dur.toFloat() else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.LIST)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    // ─── Playlists ────────────────────────────────────────────────────────────
    /** All user playlists with songs resolved from allSongs. */
    val playlists: StateFlow<List<Playlist>> = combine(
        playlistRepository.getAllPlaylistsWithRefs(),
        allSongs
    ) { refs, songs ->
        val map = songs.associateBy { it.id }
        refs.map { pwr ->
            Playlist(
                id = pwr.playlist.id,
                name = pwr.playlist.name,
                songs = pwr.refs.sortedBy { it.position }.mapNotNull { map[it.songId] }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ─── Favorites ────────────────────────────────────────────────────────────
    val favoriteIds: StateFlow<Set<Long>> = favoritesRepository.getFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Favorites as a synthetic Playlist. */
    val favoritesPlaylist: StateFlow<Playlist> = combine(favoriteIds, allSongs) { ids, songs ->
        val map = songs.associateBy { it.id }
        Playlist(
            id = -1L,
            name = "Favoritos",
            songs = ids.mapNotNull { map[it] },
            isFavorites = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Playlist(-1L, "Favoritos", isFavorites = true))

    // ─── Derived: Artists & Albums (computed from allSongs) ──────────────────
    val artists: StateFlow<List<Artist>> = allSongs
        .map { songs -> mediaStoreRepository.buildArtists(songs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = allSongs
        .map { songs -> mediaStoreRepository.buildAlbums(songs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ─── Full player visibility ───────────────────────────────────────────────
    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    // ─── Player listener ──────────────────────────────────────────────────────
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSong()
            updateQueueIndex()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateQueue()
            updateQueueIndex()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncPlaybackMode()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncPlaybackMode()
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    init {
        loadSongs()
        connectToService()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            var knownIds = emptySet<Long>()
            mediaStoreRepository.getAllSongs().collect { songs ->
                val newIds = songs.map { it.id }.toSet()
                if (knownIds.isNotEmpty()) {
                    val addedCount = (newIds - knownIds).size
                    if (addedCount > 0) showNewSongsNotification(addedCount)
                }
                knownIds = newIds
                _allSongs.value = songs
                songMap.clear()
                songMap.putAll(songs.associateBy { it.id })
            }
        }
    }

    private fun showNewSongsNotification(count: Int) {
        val title = if (count == 1) "Nueva canción agregada" else "$count nuevas canciones"
        val notification = NotificationCompat.Builder(context, "new_songs")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Toca para explorar tu biblioteca")
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1001, notification)
    }

    private fun connectToService() {
        val sessionToken =
            SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                updateCurrentSong()
                updateQueue()
                startProgressTracking()
            } catch (e: Exception) {
                // Service not yet started — will retry when user triggers playback
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _currentPosition.value = c.currentPosition.coerceAtLeast(0L)
                    val dur = c.duration
                    _duration.value = if (dur > 0) dur else 1L
                    _currentQueueIndex.value = c.currentMediaItemIndex
                }
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    // ─── Internal state helpers ────────────────────────────────────────────────
    private fun updateCurrentSong() {
        val id = controller?.currentMediaItem?.mediaId?.toLongOrNull()
        _currentSong.value = id?.let { songMap[it] }
    }

    private fun updateQueue() {
        val c = controller ?: return
        _queue.value = (0 until c.mediaItemCount).mapNotNull { i ->
            c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { id -> songMap[id] }
        }
    }

    private fun updateQueueIndex() {
        _currentQueueIndex.value = controller?.currentMediaItemIndex ?: 0
    }

    private fun syncPlaybackMode() {
        val c = controller ?: return
        _playbackMode.value = when {
            c.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.LOOP
            _playbackMode.value == PlaybackMode.SHUFFLE -> PlaybackMode.SHUFFLE
            else -> PlaybackMode.LIST
        }
    }

    // ─── Public playback API ──────────────────────────────────────────────────

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        if (songs.isEmpty()) return

        playbackSource = songs
        val (orderedSongs, queueStartIndex) = buildPlaybackOrder(
            source = songs,
            startIndex = startIndex,
            mode = _playbackMode.value
        )

        c.shuffleModeEnabled = false
        c.setMediaItems(
            orderedSongs.map { it.toMediaItem() },
            queueStartIndex,
            androidx.media3.common.C.TIME_UNSET
        )
        c.prepare()
        c.play()
        updateQueue()
        updateCurrentSong()
        updateQueueIndex()
        _showFullPlayer.value = true
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()

    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(fraction: Float) {
        val dur = _duration.value
        controller?.seekTo((fraction * dur).toLong())
    }

    fun seekToMs(positionMs: Long) = controller?.seekTo(positionMs)

    fun setPlaybackMode(mode: PlaybackMode) {
        val c = controller ?: return
        _playbackMode.value = mode
        when (mode) {
            PlaybackMode.LOOP -> {
                c.repeatMode = Player.REPEAT_MODE_ONE
                c.shuffleModeEnabled = false
            }
            PlaybackMode.LIST -> {
                c.repeatMode = Player.REPEAT_MODE_ALL
                c.shuffleModeEnabled = false
                restoreSourceOrder(c)
            }
            PlaybackMode.SHUFFLE -> {
                c.repeatMode = Player.REPEAT_MODE_ALL
                c.shuffleModeEnabled = false
                shuffleQueueFromCurrentSong(c)
            }
        }
    }

    // ─── Queue management ─────────────────────────────────────────────────────

    /**
     * Move an item in the queue without interrupting playback.
     * ExoPlayer natively handles the currently-playing item being moved.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        if (fromIndex !in 0 until c.mediaItemCount || toIndex !in 0 until c.mediaItemCount) return
        c.moveMediaItem(fromIndex, toIndex)
        updateQueue()
        playbackSource = _queue.value
    }

    /**
     * Remove an item from the queue.
     * If it's the current item, ExoPlayer advances to the next one automatically.
     */
    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.removeMediaItem(index)
        updateQueue()
        playbackSource = _queue.value
    }

    fun addToQueueAtEnd(song: Song) {
        val c = controller ?: return
        c.addMediaItem(song.toMediaItem())
        updateQueue()
        playbackSource = _queue.value
    }

    fun addToQueueNext(song: Song) {
        val c = controller ?: return
        val insertAt = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertAt, song.toMediaItem())
        updateQueue()
        playbackSource = _queue.value
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(songId)
            favoritesRepository.toggleFavorite(songId, isFav)
        }
    }

    // ─── Playlists ────────────────────────────────────────────────────────────

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.createPlaylist(name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistRepository.deletePlaylist(id) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { playlistRepository.renamePlaylist(id, name) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val pos = playlists.value.find { it.id == playlistId }?.songCount ?: 0
            playlistRepository.addSongToPlaylist(playlistId, songId, pos)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    // ─── Full player visibility ───────────────────────────────────────────────
    fun openFullPlayer() { _showFullPlayer.value = true }
    fun closeFullPlayer() { _showFullPlayer.value = false }

    // ─── Cleanup ──────────────────────────────────────────────────────────────
    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }

    private fun buildPlaybackOrder(
        source: List<Song>,
        startIndex: Int,
        mode: PlaybackMode
    ): Pair<List<Song>, Int> {
        if (source.isEmpty()) return emptyList<Song>() to 0
        val safeStartIndex = startIndex.coerceIn(source.indices)
        if (mode != PlaybackMode.SHUFFLE) return source to safeStartIndex

        val currentSong = source[safeStartIndex]
        val shuffledRest = source
            .filterIndexed { index, _ -> index != safeStartIndex }
            .shuffled()
        return listOf(currentSong) + shuffledRest to 0
    }

    private fun restoreSourceOrder(c: MediaController) {
        val source = playbackSource.ifEmpty { _queue.value }
        if (source.isEmpty()) return
        val currentId = c.currentMediaItem?.mediaId?.toLongOrNull()
        val startIndex = source.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        replaceQueueKeepingPosition(c, source, startIndex)
    }

    private fun shuffleQueueFromCurrentSong(c: MediaController) {
        val source = playbackSource.ifEmpty { _queue.value }
        if (source.isEmpty()) return
        val currentId = c.currentMediaItem?.mediaId?.toLongOrNull()
        val startIndex = source.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        val (shuffledSongs, queueStartIndex) = buildPlaybackOrder(source, startIndex, PlaybackMode.SHUFFLE)
        replaceQueueKeepingPosition(c, shuffledSongs, queueStartIndex)
    }

    private fun replaceQueueKeepingPosition(
        c: MediaController,
        songs: List<Song>,
        startIndex: Int
    ) {
        val wasPlaying = c.isPlaying
        val position = c.currentPosition.coerceAtLeast(0L)
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, position)
        c.prepare()
        if (wasPlaying) c.play()
        updateQueue()
        updateCurrentSong()
        updateQueueIndex()
    }
}

// ─── Extension: Song → MediaItem ─────────────────────────────────────────────
private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()
    )
    .build()
