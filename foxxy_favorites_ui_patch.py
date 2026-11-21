from pathlib import Path
import re

p = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")
s = p.read_text()

def add_import(line, after):
    global s
    if line not in s:
        s = s.replace(after, after + line + "\n", 1)

add_import(
    "import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress",
    "import androidx.compose.foundation.clickable\n"
)
add_import(
    "import androidx.compose.foundation.lazy.itemsIndexed",
    "import androidx.compose.foundation.lazy.items\n"
)
add_import(
    "import androidx.compose.foundation.lazy.rememberLazyListState",
    "import androidx.compose.foundation.lazy.itemsIndexed\n"
)
add_import(
    "import androidx.compose.ui.input.pointer.pointerInput",
    "import androidx.compose.ui.Modifier\n"
)
add_import(
    "import androidx.compose.runtime.rememberUpdatedState",
    "import androidx.compose.runtime.*"
)
add_import(
    "import com.foxxy.play.feature.player.PlaylistQueueItem",
    "import com.foxxy.play.feature.player.MusicTrack"
)

# State koleksi
needle = "    val onlineFavorites by viewModel.onlineFavorites.collectAsState()\n"
if "val favoriteQueueItems by viewModel.favoriteQueueItems.collectAsState()" not in s:
    if needle not in s:
        raise SystemExit("GAGAL: deklarasi onlineFavorites tidak ditemukan.")
    s = s.replace(
        needle,
        needle + "    val favoriteQueueItems by viewModel.favoriteQueueItems.collectAsState()\n",
        1
    )

# State drag untuk daftar favorit
needle = "    var selectedTab by remember { mutableIntStateOf(0) }\n"
drag_state = """    val favoriteListState = rememberLazyListState()
    var displayedFavorites by remember {
        mutableStateOf<List<PlaylistQueueItem>>(emptyList())
    }
    val latestFavorites by rememberUpdatedState(displayedFavorites)
    var draggedFavoriteKey by remember { mutableStateOf<String?>(null) }
    var favoriteDragDistance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(favoriteQueueItems) {
        if (draggedFavoriteKey == null) {
            displayedFavorites = favoriteQueueItems
        }
    }

"""
if "val favoriteListState = rememberLazyListState()" not in s:
    if needle not in s:
        raise SystemExit("GAGAL: posisi state tab tidak ditemukan.")
    s = s.replace(needle, drag_state + needle, 1)

# Ganti tab Favorites
pattern = r"                1 -> \{\s*val favorites = tracks\.filter \{ it\.id in favoriteIds \}.*?(?=                2 -> \{)"
replacement = r'''                1 -> {
                    if (favoriteQueueItems.isEmpty()) {
                        EmptyLibrary("Your favorite songs will appear here.")
                    } else {
                        Button(
                            onClick = { viewModel.playFavorites() },
                            modifier = Modifier.fillMaxWidth()
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
                                        .fillMaxWidth()
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

                                    Text(
                                        text = "☰",
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

'''
new, count = re.subn(pattern, replacement, s, count=1, flags=re.S)
if count != 1:
    raise SystemExit("GAGAL: blok tab Favorites tidak ditemukan; file belum ditulis.")

s = new
p.write_text(s)
print("OK: Favorites UI memakai favoriteQueueItems dan drag-and-drop.")
