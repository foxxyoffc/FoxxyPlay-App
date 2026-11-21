package com.foxxy.play.feature.library

import java.io.File

import androidx.compose.foundation.background

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.animateFloatAsState

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.foxxy.play.data.local.PlaylistEntity
import com.foxxy.play.data.local.DownloadEntity
import com.foxxy.play.data.local.OnlineFavoriteEntity
import com.foxxy.play.data.local.OnlinePlaylistSongEntity
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import com.foxxy.play.feature.player.MusicTrack
import com.foxxy.play.feature.player.PlaylistQueueItem

import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack


private fun playlistQueueKey(item: PlaylistQueueItem): String =
    when (item) {
        is PlaylistQueueItem.Local -> "local:${item.track.id}"
        is PlaylistQueueItem.Online ->
            "online:${item.track.source}:${item.track.id}"
    }

@Composable
fun LibraryScreen(viewModel: FoxxyPlayerViewModel, onOpenPlayer: () -> Unit) {
    val context = LocalContext.current
    val libraryScope = rememberCoroutineScope()

    val tracks by viewModel.tracks.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val currentDownload by viewModel.currentDownload.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val error by viewModel.error.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val onlineFavorites by viewModel.onlineFavorites.collectAsState()
    val favoriteQueueItems by viewModel.favoriteQueueItems.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val selectedPlaylistName by viewModel.selectedPlaylistName.collectAsState()
    val selectedPlaylistTrackIds by
        viewModel.selectedPlaylistTrackIds.collectAsState()
    val selectedOnlinePlaylistSongs by
        viewModel.selectedOnlinePlaylistSongs.collectAsState()
    val selectedPlaylistQueueItems by
        viewModel.selectedPlaylistQueueItems.collectAsState()


    val favoriteListState = rememberLazyListState()
    var displayedFavorites by remember {
        mutableStateOf<List<PlaylistQueueItem>>(emptyList())
    }
    val latestFavorites by rememberUpdatedState(displayedFavorites)
    var draggedFavoriteKey by remember { mutableStateOf<String?>(null) }
    var favoriteDragDistance by remember { mutableFloatStateOf(0f) }

    val playlistListState = rememberLazyListState()
    var displayedPlaylistItems by remember {
        mutableStateOf<List<PlaylistQueueItem>>(emptyList())
    }
    val latestPlaylistItems by rememberUpdatedState(displayedPlaylistItems)
    var draggedPlaylistKey by remember { mutableStateOf<String?>(null) }
    var playlistDragDistance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedPlaylistQueueItems) {
        if (draggedPlaylistKey == null) {
            displayedPlaylistItems = selectedPlaylistQueueItems
        }
    }


    LaunchedEffect(favoriteQueueItems) {
        if (draggedFavoriteKey == null) {
            displayedFavorites = favoriteQueueItems
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.scanLocalMusic()
    }

    fun scan() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) viewModel.scanLocalMusic()
        else launcher.launch(permission)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(20.dp))


        if (selectedPlaylistId != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closePlaylist() }) {
                    Icon(Icons.Rounded.ArrowBack, "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedPlaylistName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${selectedPlaylistQueueItems.size} songs • Hold ☰ to reorder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.deletePlaylist(selectedPlaylistId!!) }
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete playlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.loadSelectedPlaylistQueue(playImmediately = true)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPlaylistQueueItems.isNotEmpty()
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play Playlist")
            }

            Spacer(Modifier.height(8.dp))

            val availableLocalTracks = tracks.filterNot {
                it.id in selectedPlaylistTrackIds
            }
            val availableOnlineFavorites = onlineFavorites.filterNot { favorite ->
                selectedOnlinePlaylistSongs.any {
                    it.source == favorite.source && it.trackId == favorite.trackId
                }
            }

            if (
                selectedPlaylistQueueItems.isEmpty() &&
                availableLocalTracks.isEmpty() &&
                availableOnlineFavorites.isEmpty()
            ) {
                EmptyLibrary("No songs yet. Add local music or online favorites.")
            } else {
                LazyColumn(
                    state = playlistListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (displayedPlaylistItems.isNotEmpty()) {
                        item {
                            Text(
                                "In This Playlist",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        itemsIndexed(
                            displayedPlaylistItems,
                            key = { _, item -> playlistQueueKey(item) }
                        ) { _, item ->
                            val itemKey = playlistQueueKey(item)
                            val isDragged = draggedPlaylistKey == itemKey
                            val scale by animateFloatAsState(
                                targetValue = if (isDragged) 1.035f else 1f,
                                label = "playlist-drag-scale"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .shadow(
                                        if (isDragged) 12.dp else 0.dp,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(14.dp)
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .pointerInput(itemKey) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedPlaylistKey = itemKey
                                                    playlistDragDistance = 0f
                                                },
                                                onDragEnd = {
                                                    if (draggedPlaylistKey != null) {
                                                        viewModel.reorderSelectedPlaylist(latestPlaylistItems)
                                                    }
                                                    draggedPlaylistKey = null
                                                    playlistDragDistance = 0f
                                                },
                                                onDragCancel = {
                                                    if (draggedPlaylistKey != null) {
                                                        viewModel.reorderSelectedPlaylist(latestPlaylistItems)
                                                    }
                                                    draggedPlaylistKey = null
                                                    playlistDragDistance = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggedPlaylistKey != itemKey) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }

                                                    playlistDragDistance += dragAmount.y
                                                    val current = latestPlaylistItems
                                                    val currentIndex = current.indexOfFirst {
                                                        playlistQueueKey(it) == itemKey
                                                    }
                                                    if (currentIndex < 0) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }

                                                    val draggedInfo =
                                                        playlistListState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == itemKey }
                                                            ?: return@detectDragGesturesAfterLongPress

                                                    val centerY = draggedInfo.offset +
                                                        draggedInfo.size / 2 +
                                                        playlistDragDistance.toInt()

                                                    val layoutInfo = playlistListState.layoutInfo
                                                    val edgeZone = 96
                                                    when {
                                                        centerY < layoutInfo.viewportStartOffset + edgeZone ->
                                                            libraryScope.launch { playlistListState.scrollBy(-18f) }
                                                        centerY > layoutInfo.viewportEndOffset - edgeZone ->
                                                            libraryScope.launch { playlistListState.scrollBy(18f) }
                                                    }

                                                    val targetInfo =
                                                        playlistListState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull {
                                                                centerY >= it.offset &&
                                                                    centerY < it.offset + it.size
                                                            }
                                                            ?: return@detectDragGesturesAfterLongPress

                                                    val targetIndex = current.indexOfFirst {
                                                        playlistQueueKey(it) == targetInfo.key.toString()
                                                    }

                                                    if (targetIndex >= 0 && targetIndex != currentIndex) {
                                                        displayedPlaylistItems =
                                                            current.toMutableList().apply {
                                                                add(targetIndex, removeAt(currentIndex))
                                                            }
                                                        playlistDragDistance = 0f
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "☰",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isDragged) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (item) {
                                        is PlaylistQueueItem.Local -> {
                                            TrackItem(
                                                track = item.track,
                                                isFavorite = item.track.id in favoriteIds,
                                                onClick = { viewModel.playTrack(item.track) },
                                                onFavorite = { viewModel.toggleFavorite(item.track) },
                                                trailingIcon = Icons.Rounded.Delete,
                                                trailingDescription = "Remove from playlist",
                                                onTrailing = {
                                                    viewModel.toggleTrackInSelectedPlaylist(item.track)
                                                }
                                            )
                                        }
                                        is PlaylistQueueItem.Online -> {
                                            val song = selectedOnlinePlaylistSongs.firstOrNull {
                                                it.source == item.track.source &&
                                                    it.trackId == item.track.id
                                            }
                                            if (song != null) {
                                                OnlinePlaylistSongItem(
                                                    item = song,
                                                    onClick = {
                                                        viewModel.playOnlineTrack(item.track)
                                                    },
                                                    onRemove = {
                                                        viewModel.toggleOnlineTrackInSelectedPlaylist(item.track)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (availableLocalTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Add Local Music",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            availableLocalTracks,
                            key = { "available-local:${it.id}" }
                        ) { track ->
                            TrackItem(
                                track = track,
                                isFavorite = track.id in favoriteIds,
                                onClick = { viewModel.playTrack(track) },
                                onFavorite = { viewModel.toggleFavorite(track) },
                                trailingIcon = Icons.Rounded.PlaylistAdd,
                                onTrailing = {
                                    viewModel.toggleTrackInSelectedPlaylist(track)
                                }
                            )
                        }
                    }

                    if (availableOnlineFavorites.isNotEmpty()) {
                        item {
                            Text(
                                "Add Online Favorites",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            availableOnlineFavorites,
                            key = { "available:${it.source}:${it.trackId}" }
                        ) { item ->
                            OnlinePlaylistCandidateItem(
                                item = item,
                                onClick = {
                                    viewModel.playOnlineTrack(item.toOnlineMusicTrack())
                                },
                                onAdd = {
                                    viewModel.toggleOnlineTrackInSelectedPlaylist(
                                        item.toOnlineMusicTrack()
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "YOUR COLLECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your Library",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${tracks.size} local songs • ${favoriteIds.size} favorites",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { scan() },
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Rounded.Refresh, "Scan music")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { scan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isScanning,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Scanning music...")
                } else {
                    Icon(Icons.Rounded.LibraryMusic, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Local Music")
                }
            }

            Spacer(Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                listOf("Songs", "Favorites", "Playlists", "Downloads")
                    .forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
            }

            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (tracks.isEmpty()) {
                        EmptyLibrary("Scan your device to find local music.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                TrackItem(
                                    track = track,
                                    isFavorite = track.id in favoriteIds,
                                    onClick = { viewModel.playTrack(track) },
                                    onFavorite = {
                                        viewModel.toggleFavorite(track)
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (favoriteQueueItems.isEmpty()) {
                        EmptyLibrary("Your favorite songs will appear here.")
                    } else {
                        Button(
                            onClick = { viewModel.playFavorites() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play Favorites")
                        }

                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            state = favoriteListState,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(
                                displayedFavorites,
                                key = { _, item ->
                                    when (item) {
                                        is PlaylistQueueItem.Local ->
                                            "local:${item.track.id}"
                                        is PlaylistQueueItem.Online ->
                                            "online:${item.track.source}:${item.track.id}"
                                    }
                                }
                            ) { _, item ->
                                val itemKey = when (item) {
                                    is PlaylistQueueItem.Local ->
                                        "local:${item.track.id}"
                                    is PlaylistQueueItem.Online ->
                                        "online:${item.track.source}:${item.track.id}"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                        
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (item) {
                                            is PlaylistQueueItem.Local -> {
                                                TrackItem(
                                                    track = item.track,
                                                    isFavorite = true,
                                                    onClick = {
                                                        viewModel.playTrack(item.track)
                                                    },
                                                    onFavorite = {
                                                        viewModel.toggleFavorite(item.track)
                                                    }
                                                )
                                            }

                                            is PlaylistQueueItem.Online -> {
                                                val favorite = onlineFavorites.firstOrNull {
                                                    it.source == item.track.source &&
                                                    it.trackId == item.track.id
                                                }

                                                if (favorite != null) {
                                                    OnlineFavoriteItem(
                                                        item = favorite,
                                                        onClick = {
                                                            viewModel.playOnlineTrack(item.track)
                                                        },
                                                        onFavorite = {
                                                            viewModel.toggleOnlineFavorite(item.track)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .pointerInput(itemKey) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedFavoriteKey = itemKey
                                                    favoriteDragDistance = 0f
                                                },
                                                onDragEnd = {
                                                    if (draggedFavoriteKey != null) {
                                                        viewModel.reorderFavorites(
                                                            latestFavorites
                                                        )
                                                    }
                                                    draggedFavoriteKey = null
                                                    favoriteDragDistance = 0f
                                                },
                                                onDragCancel = {
                                                    if (draggedFavoriteKey != null) {
                                                        viewModel.reorderFavorites(
                                                            latestFavorites
                                                        )
                                                    }
                                                    draggedFavoriteKey = null
                                                    favoriteDragDistance = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggedFavoriteKey != itemKey) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }

                                                    favoriteDragDistance += dragAmount.y

                                                    val current = latestFavorites
                                                    val currentIndex =
                                                        current.indexOfFirst { candidate ->
                                                            when (candidate) {
                                                                is PlaylistQueueItem.Local ->
                                                                    "local:${candidate.track.id}" == itemKey
                                                                is PlaylistQueueItem.Online ->
                                                                    "online:${candidate.track.source}:${candidate.track.id}" == itemKey
                                                            }
                                                        }

                                                    if (currentIndex < 0) return@detectDragGesturesAfterLongPress

                                                    val draggedInfo =
                                                        favoriteListState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == itemKey }
                                                            ?: return@detectDragGesturesAfterLongPress

                                                    val centerY =
                                                        draggedInfo.offset +
                                                        draggedInfo.size / 2 +
                                                        favoriteDragDistance.toInt()

                                                    val layoutInfo = favoriteListState.layoutInfo
                                                    val edgeZone = 96
                                                    when {
                                                        centerY < layoutInfo.viewportStartOffset + edgeZone ->
                                                            libraryScope.launch { favoriteListState.scrollBy(-18f) }
                                                        centerY > layoutInfo.viewportEndOffset - edgeZone ->
                                                            libraryScope.launch { favoriteListState.scrollBy(18f) }
                                                    }

                                                    val targetInfo =
                                                        favoriteListState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull {
                                                                centerY >= it.offset &&
                                                                centerY < it.offset + it.size
                                                            }
                                                            ?: return@detectDragGesturesAfterLongPress

                                                    val targetIndex =
                                                        current.indexOfFirst { candidate ->
                                                            when (candidate) {
                                                                is PlaylistQueueItem.Local ->
                                                                    "local:${candidate.track.id}" == targetInfo.key
                                                                is PlaylistQueueItem.Online ->
                                                                    "online:${candidate.track.source}:${candidate.track.id}" == targetInfo.key
                                                            }
                                                        }

                                                    if (
                                                        targetIndex >= 0 &&
                                                        targetIndex != currentIndex
                                                    ) {
                                                        displayedFavorites =
                                                            current.toMutableList().apply {
                                                                add(
                                                                    targetIndex,
                                                                    removeAt(currentIndex)
                                                                )
                                                            }
                                                        favoriteDragDistance = 0f
                                                    }
                                                }
                                            )
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "☰",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Column {
                        Button(
                            onClick = {
                                playlistName = ""
                                showCreateDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Playlist")
                        }

                        Spacer(Modifier.height(12.dp))

                        if (playlists.isEmpty()) {
                            EmptyLibrary("Create your first playlist.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    PlaylistItem(playlist) {
                                        viewModel.selectPlaylist(playlist)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    val playingDownload = currentDownload

                    if (playingDownload != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clickable { onOpenPlayer() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 3.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    "NOW PLAYING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(9.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val coverFile = playingDownload.coverPath
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { File(it) }
                                        ?.takeIf { it.exists() }

                                    if (coverFile != null) {
                                        AsyncImage(
                                            model = coverFile,
                                            contentDescription = playingDownload.title,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(9.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(54.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            playingDownload.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            playingDownload.artist,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() }
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause
                                            else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play"
                                        )
                                    }
                                }

                                val progress = if (duration > 0L) {
                                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }

                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )

                                TextButton(
                                    onClick = onOpenPlayer,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Open Player")
                                }
                            }
                        }
                    }

                    if (downloads.isEmpty()) {
                        EmptyLibrary("Your downloaded songs will appear here.")
                    } else {
                        Text(
                            "${downloads.size} downloaded songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                downloads,
                                key = { "${it.source}:${it.trackId}" }
                            ) { download ->
                                DownloadItem(
                                    download = download,
                                    onClick = {
                                        viewModel.playDownloadedTrack(download)
                                    },
                                    onDelete = {
                                        viewModel.deleteDownload(download)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(playlistName)
                        showCreateDialog = false
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Open playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

private fun OnlinePlaylistSongEntity.toOnlineMusicTrack() =
    OnlineMusicTrack(
        id = trackId,
        source = source,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        previewUrl = null,
        isrc = null,
        duration = duration,
        externalUrl = externalUrl
    )

private fun OnlineFavoriteEntity.toOnlineMusicTrack() =
    OnlineMusicTrack(
        id = trackId,
        source = source,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        previewUrl = null,
        isrc = null,
        duration = duration,
        externalUrl = externalUrl
    )

@Composable
private fun OnlinePlaylistSongItem(
    item: OnlinePlaylistSongEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = "Album cover",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Online",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Remove from playlist",
                tint = MaterialTheme.colorScheme.error
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play online song",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OnlinePlaylistCandidateItem(
    item: OnlineFavoriteEntity,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = "Album cover",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Online Favorite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onAdd) {
            Icon(
                Icons.Rounded.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play online song",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OnlineFavoriteItem(
    item: OnlineFavoriteEntity,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = "Album cover",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Online",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onFavorite) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = "Remove online favorite",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play online song",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TrackItem(
    track: MusicTrack,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingDescription: String = "Add to playlist",
    onTrailing: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite
                    else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onTrailing != null && trailingIcon != null) {
                IconButton(onClick = onTrailing) {
                    Icon(
                        trailingIcon,
                        contentDescription = trailingDescription,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun DownloadItem(
    download: DownloadEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!download.coverPath.isNullOrBlank() &&
            File(download.coverPath).exists()
        ) {
            AsyncImage(
                model = File(download.coverPath),
                contentDescription = download.title,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                download.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                download.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                String.format(
                    java.util.Locale.getDefault(),
                    "%.1f MB",
                    download.fileSize / (1024.0 * 1024.0)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Delete download",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
