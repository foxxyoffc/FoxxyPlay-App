from pathlib import Path
import shutil

dao = Path("app/src/main/java/com/foxxy/play/data/local/LibraryDao.kt")
vm = Path("app/src/main/java/com/foxxy/play/feature/player/FoxxyPlayerViewModel.kt")

def save(path, content):
    shutil.copy2(path, str(path) + ".playlist-backup")
    path.write_text(content)

s = dao.read_text()
if "suspend fun clearPlaylistOrder" not in s:
    anchor = "suspend fun removePlaylistOrder(playlistId: Long, source: String, trackId: String)"
    if anchor not in s:
        raise SystemExit("DAO anchor tidak ditemukan.")
    s = s.replace(anchor, anchor + '''

    @Query("DELETE FROM playlist_order WHERE playlistId = :playlistId")
    suspend fun clearPlaylistOrder(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistOrders(items: List<PlaylistOrderEntity>)''')
    save(dao, s)

s = vm.read_text()

anchor = '''    val selectedOnlinePlaylistSongs: StateFlow<List<OnlinePlaylistSongEntity>> =
        _selectedOnlinePlaylistSongs'''
if "selectedPlaylistQueueItems" not in s:
    if anchor not in s:
        raise SystemExit("State playlist tidak ditemukan.")
    s = s.replace(anchor, anchor + '''

    private val _selectedPlaylistQueueItems =
        MutableStateFlow<List<PlaylistQueueItem>>(emptyList())
    val selectedPlaylistQueueItems: StateFlow<List<PlaylistQueueItem>> =
        _selectedPlaylistQueueItems''')

anchor = "    private suspend fun refreshSelectedPlaylist() {"
if "fun reorderSelectedPlaylist(" not in s:
    if anchor not in s:
        raise SystemExit("refreshSelectedPlaylist tidak ditemukan.")
    s = s.replace(anchor, '''    fun reorderSelectedPlaylist(items: List<PlaylistQueueItem>) {
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

''' + anchor)

old = '''    private suspend fun refreshSelectedPlaylist() {
        val id = _selectedPlaylistId.value ?: return
        _selectedPlaylistTrackIds.value =
            libraryDao.getPlaylistTrackIds(id).toSet()
        _selectedOnlinePlaylistSongs.value =
            libraryDao.getOnlinePlaylistSongs(id)
    }'''

new = '''    private suspend fun refreshSelectedPlaylist() {
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
    }'''

if old not in s:
    raise SystemExit("refreshSelectedPlaylist tidak cocok. ViewModel tidak ditulis.")
s = s.replace(old, new)

s = s.replace(
    "_selectedOnlinePlaylistSongs.value = emptyList()",
    "_selectedOnlinePlaylistSongs.value = emptyList()\n        _selectedPlaylistQueueItems.value = emptyList()",
    1
)

save(vm, s)
print("Patch DAO + ViewModel berhasil.")
