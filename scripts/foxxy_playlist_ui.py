from pathlib import Path
import shutil

p = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")
s = p.read_text()

backup = Path(str(p) + ".before-playlist-ui")
shutil.copy2(p, backup)

def add_import(value):
    global s
    if value not in s:
        s = s.replace(
            "package com.foxxy.play.feature.library\n",
            "package com.foxxy.play.feature.library\n\n" + value + "\n",
            1
        )

add_import("import androidx.compose.animation.core.animateFloatAsState")
add_import("import androidx.compose.ui.graphics.graphicsLayer")
add_import("import androidx.compose.ui.draw.shadow")
add_import("import androidx.compose.ui.draw.background")

anchor = "_selectedOnlinePlaylistSongs.collectAsState()"
if "selectedPlaylistQueueItems.collectAsState()" not in s:
    if anchor not in s:
        raise SystemExit("State playlist tidak ditemukan.")
    s = s.replace(
        anchor,
        anchor + '''
    val selectedPlaylistQueueItems by
        viewModel.selectedPlaylistQueueItems.collectAsState()''',
        1
    )

drag_anchor = "var favoriteDragDistance by remember { mutableFloatStateOf(0f) }"
if "var displayedPlaylistItems" not in s:
    if drag_anchor not in s:
        raise SystemExit("State Favorites drag tidak ditemukan.")
    s = s.replace(drag_anchor, drag_anchor + '''

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
    }''', 1)

helper = '''
private fun playlistQueueKey(item: PlaylistQueueItem): String =
    when (item) {
        is PlaylistQueueItem.Local -> "local:${item.track.id}"
        is PlaylistQueueItem.Online ->
            "online:${item.track.source}:${item.track.id}"
    }

'''
if "private fun playlistQueueKey(" not in s:
    marker = "@Composable\nfun LibraryScreen("
    if marker not in s:
        raise SystemExit("LibraryScreen tidak ditemukan.")
    s = s.replace(marker, helper + marker, 1)

start = s.find("        if (selectedPlaylistId != null) {")
end_marker = "\n        } else {\n            Row("
end = s.find(end_marker, start)

if start < 0 or end < 0:
    raise SystemExit("Blok UI playlist tidak ditemukan. File tidak diubah.")

end += len("\n        } else {")

new_ui = '''
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
                    it.source == favorite.source &&
                        it.trackId == favorite.trackId
                }
            }

            if (
                selectedPlaylistQueueItems.isEmpty() &&
                availableLocalTracks.isEmpty() &&
                availableOnlineFavorites.isEmpty()
            ) {
                EmptyLibrary("No songs yet. Add local music or Spotify favorites.")
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
                                                        viewModel.reorderSelectedPlaylist(
                                                            latestPlaylistItems
                                                        )
                                                    }
                                                    draggedPlaylistKey = null
                                                    playlistDragDistance = 0f
                                                },
                                                onDragCancel = {
                                                    if (draggedPlaylistKey != null) {
                                                        viewModel.reorderSelectedPlaylist(
                                                            latestPlaylistItems
                                                        )
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

                                                    val centerY =
                                                        draggedInfo.offset +
                                                            draggedInfo.size / 2 +
                                                            playlistDragDistance.toInt()

                                                    val targetInfo =
                                                        playlistListState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull {
                                                                centerY >= it.offset &&
                                                                    centerY < it.offset + it.size
                                                            }
                                                            ?: return@detectDragGesturesAfterLongPress

                                                    val targetIndex = current.indexOfFirst {
                                                        playlistQueueKey(it) ==
                                                            targetInfo.key.toString()
                                                    }

                                                    if (
                                                        targetIndex >= 0 &&
                                                        targetIndex != currentIndex
                                                    ) {
                                                        displayedPlaylistItems =
                                                            current.toMutableList().apply {
                                                                add(
                                                                    targetIndex,
                                                                    removeAt(currentIndex)
                                                                )
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
                                        color = if (isDragged) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (item) {
                                        is PlaylistQueueItem.Local -> {
                                            TrackItem(
                                                track = item.track,
                                                isFavorite = item.track.id in favoriteIds,
                                                onClick = {
                                                    viewModel.playTrack(item.track)
                                                },
                                                onFavorite = {
                                                    viewModel.toggleFavorite(item.track)
                                                },
                                                trailingIcon = Icons.Rounded.Delete,
                                                trailingDescription = "Remove from playlist",
                                                onTrailing = {
                                                    viewModel.toggleTrackInSelectedPlaylist(
                                                        item.track
                                                    )
                                                }
                                            )
                                        }

                                        is PlaylistQueueItem.Online -> {
                                            val song =
                                                selectedOnlinePlaylistSongs.firstOrNull {
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
                                                        viewModel.toggleOnlineTrackInSelectedPlaylist(
                                                            item.track
                                                        )
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
                                "Add Spotify Favorites",
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
        } else {'''

s = s[:start] + new_ui + s[end:]
p.write_text(s)

print("Patch UI playlist berhasil.")
print("Backup:", backup)
