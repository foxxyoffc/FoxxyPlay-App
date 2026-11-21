package com.foxxy.play.feature.player

import com.foxxy.play.core.network.NetworkStatus

import android.app.Application
import java.io.ByteArrayOutputStream
import com.foxxy.play.data.remote.resolve.AxaiResolveClient
import com.foxxy.play.data.download.FoxxyDownloadManager
import com.foxxy.play.data.local.DownloadEntity
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import coil.request.ImageRequest
import coil.ImageLoader
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.content.ComponentName
import android.content.ContentUris
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.foxxy.play.data.local.LocalMusicCacheEntity
import com.foxxy.play.data.local.FavoriteEntity
import com.foxxy.play.data.local.FavoriteOrderEntity
import com.foxxy.play.data.local.OnlineFavoriteEntity
import com.foxxy.play.data.local.OnlinePlaylistSongEntity
import com.foxxy.play.data.local.FoxxyDatabase
import com.foxxy.play.data.local.PlaylistEntity
import com.foxxy.play.data.local.PlaylistSongEntity
import com.foxxy.play.data.local.PlaylistOrderEntity
import com.foxxy.play.data.local.RecentPlayedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor

@UnstableApi
class FoxxyPlayerViewModel(application: Application) :
    AndroidViewModel(application) {

    private var player: MediaController? = null

    private var sleepTimerJob: Job? = null

    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs



    private val database = FoxxyDatabase.getInstance(application)
    private val libraryDao = database.libraryDao()
    private val downloadManager = FoxxyDownloadManager(application, libraryDao)

    private val playbackPreferences =
        application.getSharedPreferences("foxxy_playback_settings", 0)

    private val controllerFuture = MediaController.Builder(
        application,
        SessionToken(
            application,
            ComponentName(application, FoxxyPlaybackService::class.java)
        )
    ).buildAsync()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command ->
        mainHandler.post(command)
    }

    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack


    private val _currentOnlineTrack =
        MutableStateFlow<OnlineMusicTrack?>(null)
    val currentOnlineTrack: StateFlow<OnlineMusicTrack?> =
        _currentOnlineTrack

    private val _currentDownload = MutableStateFlow<DownloadEntity?>(null)
    val currentDownload: StateFlow<DownloadEntity?> = _currentDownload

    private val _isResolvingOnline = MutableStateFlow(false)
    val isResolvingOnline: StateFlow<Boolean> = _isResolvingOnline

    private val _onlinePlaybackError = MutableStateFlow<String?>(null)
    val onlinePlaybackError: StateFlow<String?> = _onlinePlaybackError

    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _queue = MutableStateFlow<List<MusicTrack>>(emptyList())
    val queue: StateFlow<List<MusicTrack>> = _queue

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex

    private val _playlistQueue =
        MutableStateFlow<List<PlaylistQueueItem>>(emptyList())
    val playlistQueue: StateFlow<List<PlaylistQueueItem>> = _playlistQueue

    private val _playlistQueueIndex = MutableStateFlow(-1)
    val playlistQueueIndex: StateFlow<Int> = _playlistQueueIndex

    private var isPlaylistQueueActive = false

    private val _recentTrackIds = MutableStateFlow<List<Long>>(emptyList())
    val recentTrackIds: StateFlow<List<Long>> = _recentTrackIds

    private val _onlineFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineFavoriteIds: StateFlow<Set<String>> = _onlineFavoriteIds

    private val _onlineFavorites =
        MutableStateFlow<List<OnlineFavoriteEntity>>(emptyList())
    val onlineFavorites: StateFlow<List<OnlineFavoriteEntity>> = _onlineFavorites

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    private val _favoriteQueueItems =
        MutableStateFlow<List<PlaylistQueueItem>>(emptyList())
    val favoriteQueueItems: StateFlow<List<PlaylistQueueItem>> =
        _favoriteQueueItems

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId

    private val _selectedPlaylistName = MutableStateFlow("")
    val selectedPlaylistName: StateFlow<String> = _selectedPlaylistName

    private val _selectedPlaylistTrackIds =
        MutableStateFlow<Set<Long>>(emptySet())
    val selectedPlaylistTrackIds: StateFlow<Set<Long>> =
        _selectedPlaylistTrackIds

    private val _selectedOnlinePlaylistSongs =
        MutableStateFlow<List<OnlinePlaylistSongEntity>>(emptyList())
    val selectedOnlinePlaylistSongs: StateFlow<List<OnlinePlaylistSongEntity>> =
        _selectedOnlinePlaylistSongs

    private val _selectedPlaylistQueueItems =
        MutableStateFlow<List<PlaylistQueueItem>>(emptyList())
    val selectedPlaylistQueueItems: StateFlow<List<PlaylistQueueItem>> =
        _selectedPlaylistQueueItems

    init {
        viewModelScope.launch {
            libraryDao.observeDownloads().collect { items ->
                _downloads.value = items
            }
        }

        // Tampilkan cache musik lokal segera setelah aplikasi dibuka.
        viewModelScope.launch {
            libraryDao.observeLocalMusicCache().collect { cached ->
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                _tracks.value = cached.map { item ->
                    MusicTrack(
                        id = item.trackId,
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        duration = item.duration,
                        uri = ContentUris.withAppendedId(
                            collection,
                            item.trackId
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            libraryDao.observeOnlineFavorites().collect { favorites ->
                _onlineFavorites.value = favorites
                _onlineFavoriteIds.value = favorites
                    .map { "${it.source}:${it.trackId}" }
                    .toSet()
                refreshFavoriteQueue()
            }
        }

        viewModelScope.launch {
            libraryDao.observeRecentPlayed().collect { recent ->
                _recentTrackIds.value = recent.map { it.trackId }
            }
        }
    }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            val activePlayer = player ?: return
            val mediaId = mediaItem?.mediaId.orEmpty()

            when {
                mediaId.startsWith("spotify:") -> {
                    _currentTrack.value = null
                    _currentDownload.value = null
                }

                mediaId.startsWith("download:") -> {
                    val audioPath = mediaId.removePrefix("download:")
                    _currentTrack.value = null
                    _currentOnlineTrack.value = null
                    _currentDownload.value = _downloads.value.firstOrNull {
                        it.audioPath == audioPath
                    }
                }

                else -> {
                    val id = mediaId.toLongOrNull()
                    val track = _tracks.value.firstOrNull { it.id == id }

                    _currentTrack.value = track
                    _currentOnlineTrack.value = null
                    _currentDownload.value = null

                    if (track != null) {
                        recordRecentlyPlayed(track)
                    }
                }
            }

            _queueIndex.value = activePlayer.currentMediaItemIndex
            updateQueue()
            updateDuration()
            _position.value = 0L
        }

        override fun onTimelineChanged(
            timeline: androidx.media3.common.Timeline,
            reason: Int
        ) {
            updateQueue()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateDuration()
            if (playbackState == Player.STATE_ENDED && isPlaylistQueueActive) {
                advancePlaylistQueue()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }
    }

    init {
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                player = controller
                controller.addListener(playerListener)

                val savedShuffle =
                    playbackPreferences.getBoolean("shuffle_enabled", false)

                val savedRepeat = playbackPreferences.getInt(
                    "repeat_mode",
                    Player.REPEAT_MODE_OFF
                ).let { mode ->
                    if (mode == Player.REPEAT_MODE_OFF ||
                        mode == Player.REPEAT_MODE_ALL ||
                        mode == Player.REPEAT_MODE_ONE
                    ) mode else Player.REPEAT_MODE_OFF
                }

                controller.shuffleModeEnabled = savedShuffle
                controller.repeatMode = savedRepeat

                _isPlaying.value = controller.isPlaying
                _shuffleEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode
                _currentTrack.value =
                    _tracks.value.getOrNull(controller.currentMediaItemIndex)

                updateDuration()
            } catch (e: Exception) {
                _error.value =
                    e.localizedMessage ?: "Gagal menghubungkan pemutar."
            }
        }, mainExecutor)

        viewModelScope.launch {
            libraryDao.observeFavorites().collect { favorites ->
                _favoriteIds.value = favorites.map { it.trackId }.toSet()
                refreshFavoriteQueue()
            }
        }

        viewModelScope.launch {
            libraryDao.observePlaylists().collect { items ->
                _playlists.value = items
            }
        }

        viewModelScope.launch {
            while (true) {
                val activePlayer = player
                if (activePlayer != null &&
                    activePlayer.currentMediaItem != null
                ) {
                    _position.value =
                        activePlayer.currentPosition.coerceAtLeast(0L)
                    updateDuration()
                }
                delay(500)
            }
        }
    }

    private fun updateDuration() {
        val value = player?.duration ?: 0L
        _duration.value = if (value > 0) {
            value
        } else {
            _currentOnlineTrack.value?.duration?.coerceAtLeast(0L) ?: 0L
        }
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val collection =
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION
                    )

                    val selection =
                        "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                    val sortOrder =
                        "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

                    val songs = mutableListOf<MusicTrack>()

                    context.contentResolver.query(
                        collection,
                        projection,
                        selection,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                        )
                        val titleColumn = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                        )
                        val artistColumn = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                        )
                        val albumColumn = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM
                        )
                        val durationColumn = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                        )

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val uri = ContentUris.withAppendedId(
                                collection,
                                id
                            )

                            songs.add(
                                MusicTrack(
                                    id = id,
                                    title = cursor.getString(titleColumn)
                                        ?: "Unknown Title",
                                    artist = cursor.getString(artistColumn)
                                        ?.takeUnless { it == "<unknown>" }
                                        ?: "Unknown Artist",
                                    album = cursor.getString(albumColumn)
                                        ?: "Unknown Album",
                                    duration = cursor.getLong(durationColumn),
                                    uri = uri
                                )
                            )
                        }
                    }
                    songs
                }

                _tracks.value = result

                // Perbarui cache permanen setelah pemindaian berhasil.
                libraryDao.clearLocalMusicCache()
                if (result.isNotEmpty()) {
                    libraryDao.insertLocalMusicCache(
                        result.map { track ->
                            LocalMusicCacheEntity(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                duration = track.duration
                            )
                        }
                    )
                }

                if (result.isEmpty()) {
                    _error.value = "Belum ada musik lokal yang ditemukan."
                }
            } catch (e: Exception) {
                _error.value =
                    e.localizedMessage ?: "Gagal membaca musik."
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun recordRecentlyPlayed(track: MusicTrack) {
        viewModelScope.launch {
            libraryDao.addRecentlyPlayed(
                RecentPlayedEntity(trackId = track.id)
            )
        }
    }

    fun playTrack(track: MusicTrack) {
        isPlaylistQueueActive = false
        val activePlayer = player
        if (activePlayer == null) {
            _error.value = "Pemutar musik sedang disiapkan."
            return
        }

        val list = _tracks.value
        val index = list.indexOfFirst { it.id == track.id }
        if (index < 0) return

        val mediaItems = list.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }

        _queue.value = list
        _queueIndex.value = index
        activePlayer.setMediaItems(mediaItems)
        activePlayer.shuffleModeEnabled = _shuffleEnabled.value
        activePlayer.repeatMode = _repeatMode.value
        activePlayer.seekTo(index, 0L)
        activePlayer.prepare()
        activePlayer.play()

        _currentTrack.value = track
        _currentDownload.value = null
        _currentOnlineTrack.value = null
        recordRecentlyPlayed(track)
        _position.value = 0L
    }


    fun playDownloadedTrack(track: DownloadEntity) {
        val activePlayer = player
        if (activePlayer == null) {
            _error.value = "Pemutar musik sedang disiapkan."
            return
        }

        val audioFile = File(track.audioPath)
        if (!audioFile.isFile || !audioFile.canRead()) {
            _downloadError.value = "File unduhan tidak ditemukan."
            return
        }

        isPlaylistQueueActive = false
        _currentTrack.value = null
        _currentOnlineTrack.value = null
        _currentDownload.value = track
        _downloadError.value = null

        val mediaItem = MediaItem.Builder()
            .setUri(android.net.Uri.fromFile(audioFile))
            .setMediaId("download:${track.audioPath}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()

        activePlayer.setMediaItem(mediaItem)
        activePlayer.shuffleModeEnabled = _shuffleEnabled.value
        activePlayer.repeatMode = _repeatMode.value
        activePlayer.prepare()
        activePlayer.play()
        _position.value = 0L
    }

    fun playOnlineTrack(
        track: OnlineMusicTrack,
        fromPlaylist: Boolean = false
    ) {
        if (!fromPlaylist) {
            isPlaylistQueueActive = false
        }
        val activePlayer = player
        if (activePlayer == null) {
            _onlinePlaybackError.value = "Pemutar musik sedang disiapkan."
            return
        }

        val spotifyUrl = track.externalUrl
        if (spotifyUrl.isNullOrBlank() ||
            !spotifyUrl.startsWith("https://open.spotify.com/track/")
        ) {
            _onlinePlaybackError.value = "Tautan lagu online tidak valid."
            return
        }

        _currentTrack.value = null
        _currentDownload.value = null
        _currentOnlineTrack.value = track

        viewModelScope.launch {
            _isResolvingOnline.value = true
            _onlinePlaybackError.value = null

            try {
                val response = AxaiResolveClient.service.resolve(spotifyUrl)

                if (!response.success ||
                    response.audioUrl.isNullOrBlank() ||
                    !response.audioUrl.startsWith("https://")
                ) {
                    throw IllegalStateException(
                        "Provider tidak memberikan URL audio yang valid."
                    )
                }

                val artworkBytes = withContext(Dispatchers.IO) {
                    val imageLoader = ImageLoader(getApplication())
                    val request = ImageRequest.Builder(getApplication())
                        .data(track.coverUrl ?: response.thumbnail)
                        .allowHardware(false)
                        .build()

                    val result = imageLoader.execute(request)
                    val drawable = result.drawable

                    if (drawable == null) {
                        null
                    } else {
                        val bitmap = drawable.toBitmap(
                            width = 512,
                            height = 512,
                            config = Bitmap.Config.ARGB_8888
                        )
                        ByteArrayOutputStream().use { output ->
                            bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                88,
                                output
                            )
                            output.toByteArray()
                        }
                    }
                }

                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)

                val artworkUrl = track.coverUrl ?: response.thumbnail
                if (!artworkUrl.isNullOrBlank()) {
                    metadataBuilder.setArtworkUri(
                        android.net.Uri.parse(artworkUrl)
                    )
                }

                if (artworkBytes != null) {
                    metadataBuilder.setArtworkData(
                        artworkBytes,
                        3
                    )
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId("spotify:${track.id}")
                    .setUri(response.audioUrl)
                    .setMediaMetadata(metadataBuilder.build())
                    .build()

                activePlayer.setMediaItem(mediaItem)
                activePlayer.prepare()
                activePlayer.play()

                _position.value = 0L
                updateDuration()
            } catch (error: Exception) {
                _onlinePlaybackError.value =
                    if (!NetworkStatus.isOnline(getApplication())) {
                        NetworkStatus.OFFLINE_MESSAGE
                    } else {
                        error.localizedMessage ?: "Gagal memutar lagu online."
                    }
            } finally {
                _isResolvingOnline.value = false
            }
        }
    }

    fun deleteDownload(track: DownloadEntity) {
        viewModelScope.launch {
            try {
                if (_currentDownload.value?.audioPath == track.audioPath) {
                    player?.stop()
                    player?.clearMediaItems()
                    _currentDownload.value = null
                    _position.value = 0L
                }

                File(track.audioPath).delete()
                track.coverPath?.let { File(it).delete() }

                libraryDao.removeDownload(track.source, track.trackId)
                _downloadError.value = null
            } catch (error: Exception) {
                _downloadError.value =
                    error.localizedMessage ?: "Gagal menghapus unduhan."
            }
        }
    }

    fun downloadOnlineTrack(track: OnlineMusicTrack) {
        val spotifyUrl = track.externalUrl

        if (spotifyUrl.isNullOrBlank() ||
            !spotifyUrl.startsWith("https://open.spotify.com/track/")
        ) {
            _downloadError.value = "Tautan lagu online tidak valid."
            return
        }

        val key = "${track.source}:${track.id}"
        if (key in _downloadingTrackIds.value) return

        viewModelScope.launch {
            _downloadingTrackIds.value = _downloadingTrackIds.value + key
            _downloadError.value = null

            try {
                val response = AxaiResolveClient.service.resolve(spotifyUrl)

                if (!response.success ||
                    response.audioUrl.isNullOrBlank() ||
                    !response.audioUrl.startsWith("https://")
                ) {
                    throw IllegalStateException(
                        "Provider tidak memberikan URL audio yang valid."
                    )
                }

                downloadManager.download(track, response.audioUrl)
            } catch (error: Exception) {
                _downloadError.value =
                    error.localizedMessage ?: "Gagal mengunduh lagu."
            } finally {
                _downloadingTrackIds.value =
                    _downloadingTrackIds.value - key
            }
        }
    }

    fun startSleepTimer(hours: Int, minutes: Int) {
        val totalMinutes = hours.coerceAtLeast(0) * 60L +
            minutes.coerceAtLeast(0)

        if (totalMinutes <= 0L) return

        val durationMs = totalMinutes * 60_000L
        val deadline = SystemClock.elapsedRealtime() + durationMs

        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = durationMs

        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = (
                    deadline - SystemClock.elapsedRealtime()
                ).coerceAtLeast(0L)

                _sleepTimerRemainingMs.value = remaining

                if (remaining <= 0L) break

                delay(1000L)
            }

            player?.pause()
            _sleepTimerRemainingMs.value = 0L
            sleepTimerJob = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = 0L
    }

    fun togglePlayPause() {
        val activePlayer = player ?: return
        if (activePlayer.currentMediaItem == null) return

        if (activePlayer.isPlaying) activePlayer.pause()
        else activePlayer.play()
    }

    fun nextTrack() {
        if (isPlaylistQueueActive) {
            advancePlaylistQueue()
            return
        }

        val activePlayer = player ?: return
        if (activePlayer.hasNextMediaItem()) {
            activePlayer.seekToNextMediaItem()
            activePlayer.play()
        }
    }

    fun previousTrack() {
        val activePlayer = player ?: return

        if (isPlaylistQueueActive) {
            if (activePlayer.currentPosition > 3000L) {
                activePlayer.seekTo(0L)
                return
            }

            val previous = _playlistQueueIndex.value - 1
            if (previous >= 0) {
                startPlaylistQueueItem(previous)
            } else if (activePlayer.repeatMode == Player.REPEAT_MODE_ALL) {
                startPlaylistQueueItem(_playlistQueue.value.lastIndex)
            } else {
                activePlayer.seekTo(0L)
            }
            return
        }

        if (activePlayer.currentPosition > 3000L) {
            activePlayer.seekTo(0L)
        } else if (activePlayer.hasPreviousMediaItem()) {
            activePlayer.seekToPreviousMediaItem()
        }
        activePlayer.play()
    }

    fun seekTo(position: Long) {
        val safePosition = position.coerceAtLeast(0L)
        player?.seekTo(safePosition)
        _position.value = safePosition
    }

    fun toggleShuffle() {
        val activePlayer = player ?: return
        activePlayer.shuffleModeEnabled = !activePlayer.shuffleModeEnabled
        _shuffleEnabled.value = activePlayer.shuffleModeEnabled

        playbackPreferences.edit()
            .putBoolean("shuffle_enabled", activePlayer.shuffleModeEnabled)
            .apply()
    }

    fun cycleRepeatMode() {
        val activePlayer = player ?: return

        val next = when (activePlayer.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }

        activePlayer.repeatMode = next
        _repeatMode.value = next

        playbackPreferences.edit()
            .putInt("repeat_mode", next)
            .apply()
    }

    // FAVORITES

    fun toggleFavorite(track: MusicTrack) {
        viewModelScope.launch {
            if (track.id in _favoriteIds.value) {
                libraryDao.removeFavorite(track.id)
                libraryDao.removeFavoriteOrder("local", track.id.toString())
            } else {
                libraryDao.addFavorite(FavoriteEntity(track.id))
                libraryDao.addFavoriteOrder(
                    FavoriteOrderEntity(
                        source = "local",
                        trackId = track.id.toString(),
                        position = libraryDao.getNextFavoritePosition()
                    )
                )
            }
            refreshFavoriteQueue()
        }
    }

    fun toggleOnlineFavorite(track: OnlineMusicTrack) {
        viewModelScope.launch {
            val key = "${track.source}:${track.id}"

            if (key in _onlineFavoriteIds.value) {
                libraryDao.removeOnlineFavorite(track.source, track.id)
                libraryDao.removeFavoriteOrder(track.source, track.id)
            } else {
                libraryDao.addOnlineFavorite(
                    OnlineFavoriteEntity(
                        source = track.source,
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        coverUrl = track.coverUrl,
                        externalUrl = track.externalUrl,
                        duration = track.duration
                    )
                )
                libraryDao.addFavoriteOrder(
                    FavoriteOrderEntity(
                        source = track.source,
                        trackId = track.id,
                        position = libraryDao.getNextFavoritePosition()
                    )
                )
            }
            refreshFavoriteQueue()
        }
    }

    // PLAYLISTS


    fun addCurrentTrackToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            addCurrentTrackToPlaylistInternal(playlistId)
        }
    }

    fun createPlaylistWithCurrentTrack(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val currentLocal = _currentTrack.value
            val currentOnline = _currentOnlineTrack.value
            if (currentLocal == null && currentOnline == null) return@launch

            val id = libraryDao.createPlaylist(
                PlaylistEntity(name = cleanName)
            )
            addCurrentTrackToPlaylistInternal(id)
        }
    }

    private suspend fun addCurrentTrackToPlaylistInternal(playlistId: Long) {
        val local = _currentTrack.value
        val online = _currentOnlineTrack.value

        if (online != null) {
            val existing = libraryDao.getOnlinePlaylistSongs(playlistId).any {
                it.source == online.source && it.trackId == online.id
            }
            if (existing) return

            libraryDao.addOnlineSongToPlaylist(
                OnlinePlaylistSongEntity(
                    playlistId = playlistId,
                    source = online.source,
                    trackId = online.id,
                    title = online.title,
                    artist = online.artist,
                    album = online.album,
                    coverUrl = online.coverUrl,
                    externalUrl = online.externalUrl,
                    duration = online.duration
                )
            )
            libraryDao.addPlaylistOrder(
                PlaylistOrderEntity(
                    playlistId = playlistId,
                    source = online.source,
                    trackId = online.id,
                    position = libraryDao.getNextPlaylistPosition(playlistId)
                )
            )
        } else if (local != null) {
            val existing = local.id in libraryDao.getPlaylistTrackIds(playlistId)
            if (existing) return

            libraryDao.addSongToPlaylist(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    trackId = local.id
                )
            )
            libraryDao.addPlaylistOrder(
                PlaylistOrderEntity(
                    playlistId = playlistId,
                    source = "local",
                    trackId = local.id.toString(),
                    position = libraryDao.getNextPlaylistPosition(playlistId)
                )
            )
        }
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val id = libraryDao.createPlaylist(
                PlaylistEntity(name = cleanName)
            )
            selectPlaylist(
                PlaylistEntity(id = id, name = cleanName)
            )
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            _selectedPlaylistId.value = playlist.id
            _selectedPlaylistName.value = playlist.name
            refreshSelectedPlaylist()
        }
    }

    fun closePlaylist() {
        _selectedPlaylistId.value = null
        _selectedPlaylistName.value = ""
        _selectedPlaylistTrackIds.value = emptySet()
        _selectedOnlinePlaylistSongs.value = emptyList()
        _selectedPlaylistQueueItems.value = emptyList()
    }


    fun toggleTrackInSelectedPlaylist(track: MusicTrack) {
        val playlistId = _selectedPlaylistId.value ?: return

        viewModelScope.launch {
            val exists = track.id in _selectedPlaylistTrackIds.value

            if (exists) {
                libraryDao.removeSongFromPlaylist(playlistId, track.id)
                libraryDao.removePlaylistOrder(
                    playlistId,
                    "local",
                    track.id.toString()
                )
            } else {
                libraryDao.addSongToPlaylist(
                    PlaylistSongEntity(
                        playlistId = playlistId,
                        trackId = track.id
                    )
                )
                libraryDao.addPlaylistOrder(
                    PlaylistOrderEntity(
                        playlistId = playlistId,
                        source = "local",
                        trackId = track.id.toString(),
                        position = libraryDao.getNextPlaylistPosition(playlistId)
                    )
                )
            }

            refreshSelectedPlaylist()
        }
    }


    fun toggleOnlineTrackInSelectedPlaylist(track: OnlineMusicTrack) {
        val playlistId = _selectedPlaylistId.value ?: return

        viewModelScope.launch {
            val exists = _selectedOnlinePlaylistSongs.value.any {
                it.source == track.source && it.trackId == track.id
            }

            if (exists) {
                libraryDao.removeOnlineSongFromPlaylist(
                    playlistId,
                    track.source,
                    track.id
                )
                libraryDao.removePlaylistOrder(
                    playlistId,
                    track.source,
                    track.id
                )
            } else {
                libraryDao.addOnlineSongToPlaylist(
                    OnlinePlaylistSongEntity(
                        playlistId = playlistId,
                        source = track.source,
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        coverUrl = track.coverUrl,
                        externalUrl = track.externalUrl,
                        duration = track.duration
                    )
                )
                libraryDao.addPlaylistOrder(
                    PlaylistOrderEntity(
                        playlistId = playlistId,
                        source = track.source,
                        trackId = track.id,
                        position = libraryDao.getNextPlaylistPosition(playlistId)
                    )
                )
            }

            refreshSelectedPlaylist()
        }
    }


    private suspend fun refreshFavoriteQueue() {
        val localById = _tracks.value
            .filter { it.id in _favoriteIds.value }
            .associateBy { it.id.toString() }

        val onlineByKey = _onlineFavorites.value
            .associateBy { "${it.source}:${it.trackId}" }

        val storedOrder = libraryDao.getFavoriteOrder()
        val result = mutableListOf<PlaylistQueueItem>()
        val normalizedOrder = mutableListOf<FavoriteOrderEntity>()
        val addedKeys = mutableSetOf<String>()

        for (entry in storedOrder) {
            val key = "${entry.source}:${entry.trackId}"

            when (entry.source) {
                "local" -> {
                    val track = localById[entry.trackId]
                    if (track != null && addedKeys.add(key)) {
                        result.add(PlaylistQueueItem.Local(track))
                        normalizedOrder.add(
                            FavoriteOrderEntity("local", entry.trackId, normalizedOrder.size)
                        )
                    }
                }

                else -> {
                    val favorite = onlineByKey[key]
                    if (favorite != null && addedKeys.add(key)) {
                        result.add(
                            PlaylistQueueItem.Online(
                                OnlineMusicTrack(
                                    id = favorite.trackId,
                                    source = favorite.source,
                                    title = favorite.title,
                                    artist = favorite.artist,
                                    album = favorite.album,
                                    coverUrl = favorite.coverUrl,
                                    previewUrl = null,
                                    isrc = null,
                                    duration = favorite.duration,
                                    externalUrl = favorite.externalUrl
                                )
                            )
                        )
                        normalizedOrder.add(
                            FavoriteOrderEntity(
                                favorite.source,
                                favorite.trackId,
                                normalizedOrder.size
                            )
                        )
                    }
                }
            }
        }

        for (track in localById.values) {
            val key = "local:${track.id}"
            if (addedKeys.add(key)) {
                result.add(PlaylistQueueItem.Local(track))
                normalizedOrder.add(
                    FavoriteOrderEntity("local", track.id.toString(), normalizedOrder.size)
                )
            }
        }

        for (favorite in _onlineFavorites.value) {
            val key = "${favorite.source}:${favorite.trackId}"
            if (addedKeys.add(key)) {
                result.add(
                    PlaylistQueueItem.Online(
                        OnlineMusicTrack(
                            id = favorite.trackId,
                            source = favorite.source,
                            title = favorite.title,
                            artist = favorite.artist,
                            album = favorite.album,
                            coverUrl = favorite.coverUrl,
                            previewUrl = null,
                            isrc = null,
                            duration = favorite.duration,
                            externalUrl = favorite.externalUrl
                        )
                    )
                )
                normalizedOrder.add(
                    FavoriteOrderEntity(
                        favorite.source,
                        favorite.trackId,
                        normalizedOrder.size
                    )
                )
            }
        }

        libraryDao.clearFavoriteOrder()
        if (normalizedOrder.isNotEmpty()) {
            libraryDao.insertFavoriteOrders(normalizedOrder)
        }

        _favoriteQueueItems.value = result
    }

    fun reorderFavorites(items: List<PlaylistQueueItem>) {
        viewModelScope.launch {
            val orders = items.mapIndexed { index, item ->
                when (item) {
                    is PlaylistQueueItem.Local -> FavoriteOrderEntity(
                        source = "local",
                        trackId = item.track.id.toString(),
                        position = index
                    )

                    is PlaylistQueueItem.Online -> FavoriteOrderEntity(
                        source = item.track.source,
                        trackId = item.track.id,
                        position = index
                    )
                }
            }

            libraryDao.clearFavoriteOrder()
            if (orders.isNotEmpty()) {
                libraryDao.insertFavoriteOrders(orders)
            }
            _favoriteQueueItems.value = items
        }
    }

    fun playFavorites() {
        val items = _favoriteQueueItems.value
        if (items.isEmpty()) return

        _playlistQueue.value = items
        _playlistQueueIndex.value = -1
        isPlaylistQueueActive = true
        startPlaylistQueueItem(0)
    }

    fun loadSelectedPlaylistQueue(playImmediately: Boolean = false) {
        val playlistId = _selectedPlaylistId.value ?: return

        viewModelScope.launch {
            val order = libraryDao.getPlaylistOrder(playlistId)
            val localTracks = _tracks.value.associateBy { it.id }
            val onlineSongs = libraryDao.getOnlinePlaylistSongs(playlistId)
                .associateBy { "${it.source}:${it.trackId}" }

            val items = order.mapNotNull { entry ->
                when (entry.source) {
                    "local" -> {
                        val id = entry.trackId.toLongOrNull()
                        id?.let { localTracks[it] }?.let {
                            PlaylistQueueItem.Local(it)
                        }
                    }
                    else -> {
                        onlineSongs["${entry.source}:${entry.trackId}"]?.let {
                            PlaylistQueueItem.Online(
                                OnlineMusicTrack(
                                    id = it.trackId,
                                    source = it.source,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    coverUrl = it.coverUrl,
                                    previewUrl = null,
                                    isrc = null,
                                    duration = it.duration,
                                    externalUrl = it.externalUrl
                                )
                            )
                        }
                    }
                }
            }

            _playlistQueue.value = items
            _playlistQueueIndex.value = -1

            if (playImmediately && items.isNotEmpty()) {
                isPlaylistQueueActive = true
                startPlaylistQueueItem(0)
            }
        }
    }

    private fun startPlaylistQueueItem(index: Int) {
        val activePlayer = player ?: return
        val items = _playlistQueue.value

        if (index !in items.indices) return

        _playlistQueueIndex.value = index

        when (val item = items[index]) {
            is PlaylistQueueItem.Local -> {
                val track = item.track

                val mediaItem = MediaItem.Builder()
                    .setUri(track.uri)
                    .setMediaId(track.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .build()
                    )
                    .build()

                activePlayer.setMediaItem(mediaItem)
                activePlayer.prepare()
                activePlayer.play()

                _currentTrack.value = track
                _currentOnlineTrack.value = null
                recordRecentlyPlayed(track)
                _position.value = 0L
            }

            is PlaylistQueueItem.Online -> {
                playOnlineTrack(item.track, fromPlaylist = true)
            }
        }
    }

    private fun advancePlaylistQueue() {
        val items = _playlistQueue.value
        val current = _playlistQueueIndex.value

        if (!isPlaylistQueueActive || items.isEmpty()) return

        val next = current + 1

        if (next < items.size) {
            startPlaylistQueueItem(next)
        } else if (player?.repeatMode == Player.REPEAT_MODE_ALL) {
            startPlaylistQueueItem(0)
        } else {
            isPlaylistQueueActive = false
        }
    }

    fun reorderSelectedPlaylist(items: List<PlaylistQueueItem>) {
        val playlistId = _selectedPlaylistId.value ?: return
        viewModelScope.launch {
            val orders = items.mapIndexed { index, item ->
                when (item) {
                    is PlaylistQueueItem.Local -> PlaylistOrderEntity(
                        playlistId, "local", item.track.id.toString(), index
                    )
                    is PlaylistQueueItem.Online -> PlaylistOrderEntity(
                        playlistId, item.track.source, item.track.id, index
                    )
                }
            }
            libraryDao.clearPlaylistOrder(playlistId)
            if (orders.isNotEmpty()) libraryDao.insertPlaylistOrders(orders)
            _selectedPlaylistQueueItems.value = items
        }
    }

    private suspend fun refreshSelectedPlaylist() {
        val id = _selectedPlaylistId.value ?: return
        val localIds = libraryDao.getPlaylistTrackIds(id).toSet()
        val onlineSongs = libraryDao.getOnlinePlaylistSongs(id)
        val localTracks = _tracks.value.filter { it.id in localIds }.associateBy { it.id }

        _selectedPlaylistTrackIds.value = localIds
        _selectedOnlinePlaylistSongs.value = onlineSongs

        val onlineByKey = onlineSongs.associateBy { "${it.source}:${it.trackId}" }
        val savedOrder = libraryDao.getPlaylistOrder(id)

        val orderedItems = savedOrder.mapNotNull { entry ->
            when (entry.source) {
                "local" -> entry.trackId.toLongOrNull()
                    ?.let { localTracks[it] }
                    ?.let { PlaylistQueueItem.Local(it) }
                else -> onlineByKey["${entry.source}:${entry.trackId}"]?.let { song ->
                    PlaylistQueueItem.Online(
                        OnlineMusicTrack(
                            id = song.trackId,
                            source = song.source,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            coverUrl = song.coverUrl,
                            previewUrl = null,
                            isrc = null,
                            duration = song.duration,
                            externalUrl = song.externalUrl
                        )
                    )
                }
            }
        }.toMutableList()

        val keys = orderedItems.map {
            when (it) {
                is PlaylistQueueItem.Local -> "local:${it.track.id}"
                is PlaylistQueueItem.Online -> "${it.track.source}:${it.track.id}"
            }
        }.toMutableSet()

        localTracks.values.forEach {
            if (keys.add("local:${it.id}")) orderedItems.add(PlaylistQueueItem.Local(it))
        }
        onlineSongs.forEach { song ->
            if (keys.add("${song.source}:${song.trackId}")) {
                orderedItems.add(
                    PlaylistQueueItem.Online(
                        OnlineMusicTrack(
                            id = song.trackId,
                            source = song.source,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            coverUrl = song.coverUrl,
                            previewUrl = null,
                            isrc = null,
                            duration = song.duration,
                            externalUrl = song.externalUrl
                        )
                    )
                )
            }
        }

        _selectedPlaylistQueueItems.value = orderedItems

        val normalized = orderedItems.mapIndexed { index, item ->
            when (item) {
                is PlaylistQueueItem.Local -> PlaylistOrderEntity(
                    id, "local", item.track.id.toString(), index
                )
                is PlaylistQueueItem.Online -> PlaylistOrderEntity(
                    id, item.track.source, item.track.id, index
                )
            }
        }

        if (normalized.size != savedOrder.size ||
            normalized.map { Triple(it.source, it.trackId, it.position) } !=
            savedOrder.map { Triple(it.source, it.trackId, it.position) }
        ) {
            libraryDao.clearPlaylistOrder(id)
            if (normalized.isNotEmpty()) libraryDao.insertPlaylistOrders(normalized)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            libraryDao.deletePlaylist(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                closePlaylist()
            }
        }
    }

    private fun updateQueue() {
        val activePlayer = player ?: return
        val currentTracks = _tracks.value

        _queue.value = (0 until activePlayer.mediaItemCount).mapNotNull { index ->
            val id = activePlayer.getMediaItemAt(index).mediaId.toLongOrNull()
            currentTracks.firstOrNull { it.id == id }
        }

        if (activePlayer.currentMediaItemIndex >= 0) {
            _queueIndex.value = activePlayer.currentMediaItemIndex
        }
    }

    fun removeQueueItem(index: Int) {
        val activePlayer = player ?: return
        if (index !in 0 until activePlayer.mediaItemCount) return

        activePlayer.removeMediaItem(index)
        updateQueue()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val activePlayer = player ?: return
        if (from !in 0 until activePlayer.mediaItemCount) return
        if (to !in 0 until activePlayer.mediaItemCount) return
        if (from == to) return

        activePlayer.moveMediaItem(from, to)
        updateQueue()
    }

    fun playQueueItem(index: Int) {
        val activePlayer = player ?: return
        if (index !in 0 until activePlayer.mediaItemCount) return

        activePlayer.seekTo(index, 0L)
        activePlayer.play()
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            libraryDao.clearRecentlyPlayed()
        }
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        player = null
        super.onCleared()
    }
}
