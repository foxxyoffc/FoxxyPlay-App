from pathlib import Path

vm = Path("app/src/main/java/com/foxxy/play/feature/player/FoxxyPlayerViewModel.kt")
db = Path("app/src/main/java/com/foxxy/play/data/local/FoxxyDatabase.kt")

def patch(path, old, new):
    s = path.read_text()
    if old not in s:
        raise SystemExit(f"MARKER TIDAK DITEMUKAN: {path}\n{old[:160]}")
    backup = path.with_suffix(path.suffix + ".bak")
    if not backup.exists():
        backup.write_text(s)
    path.write_text(s.replace(old, new, 1))
    print("OK:", path)

# Migrasi SQLite: hindari ROW_NUMBER()
s = db.read_text()
start = s.find('        private val MIGRATION_5_6 = object : Migration(5, 6)')
end = s.find('\n        @Volatile', start)
if start < 0 or end < 0:
    raise SystemExit("Blok MIGRATION_5_6 tidak ditemukan")

backup = db.with_suffix(db.suffix + ".bak")
if not backup.exists():
    backup.write_text(s)

migration = '''        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorite_order (
                        source TEXT NOT NULL,
                        trackId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(source, trackId)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO favorite_order(source, trackId, position)
                    SELECT 'local', CAST(current.trackId AS TEXT),
                        (
                            SELECT COUNT(*)
                            FROM favorites previous
                            WHERE previous.trackId > current.trackId
                        )
                    FROM favorites current
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO favorite_order(source, trackId, position)
                    SELECT 'spotify', current.trackId,
                        (SELECT COUNT(*) FROM favorites) +
                        (
                            SELECT COUNT(*)
                            FROM online_favorites previous
                            WHERE previous.title COLLATE NOCASE < current.title COLLATE NOCASE
                               OR (
                                   previous.title COLLATE NOCASE = current.title COLLATE NOCASE
                                   AND previous.trackId < current.trackId
                               )
                        )
                    FROM online_favorites current
                    """.trimIndent()
                )
            }
        }
'''
db.write_text(s[:start] + migration + s[end:])
print("OK: migrasi SQLite diperbarui")

# Import entity
patch(
    vm,
    'import com.foxxy.play.data.local.FavoriteEntity',
    'import com.foxxy.play.data.local.FavoriteEntity\nimport com.foxxy.play.data.local.FavoriteOrderEntity'
)

# State daftar favorit terurut
patch(
    vm,
    '    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds',
    '''    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    private val _favoriteQueueItems =
        MutableStateFlow<List<PlaylistQueueItem>>(emptyList())
    val favoriteQueueItems: StateFlow<List<PlaylistQueueItem>> =
        _favoriteQueueItems'''
)

# Refresh setelah perubahan favorit lokal dan online dari Flow
patch(
    vm,
    '''                _onlineFavoriteIds.value = favorites
                    .map { "${it.source}:${it.trackId}" }
                    .toSet()
            }''',
    '''                _onlineFavoriteIds.value = favorites
                    .map { "${it.source}:${it.trackId}" }
                    .toSet()
                refreshFavoriteQueue()
            }'''
)

patch(
    vm,
    '''            libraryDao.observeFavorites().collect { favorites ->
                _favoriteIds.value = favorites.map { it.trackId }.toSet()
            }''',
    '''            libraryDao.observeFavorites().collect { favorites ->
                _favoriteIds.value = favorites.map { it.trackId }.toSet()
                refreshFavoriteQueue()
            }'''
)

# Toggle lokal: tambah/hapus posisi favorit
patch(
    vm,
    '''            if (track.id in _favoriteIds.value) {
                libraryDao.removeFavorite(track.id)
            } else {
                libraryDao.addFavorite(FavoriteEntity(track.id))
            }''',
    '''            if (track.id in _favoriteIds.value) {
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
            refreshFavoriteQueue()'''
)

# Toggle online: tambah/hapus posisi favorit
patch(
    vm,
    '''            if (key in _onlineFavoriteIds.value) {
                libraryDao.removeOnlineFavorite(track.source, track.id)
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
            }''',
    '''            if (key in _onlineFavoriteIds.value) {
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
            refreshFavoriteQueue()'''
)

# Replace playback favorites with persisted order
start = s.find('    fun playFavorites() {')
end = s.find('\n    fun loadSelectedPlaylistQueue', start)
if start < 0 or end < 0:
    raise SystemExit("Fungsi playFavorites tidak ditemukan")

old_block = s[start:end]
if not backup.exists():
    backup.write_text(vm.read_text())

new_block = '''    suspend fun refreshFavoriteQueue() {
        val localTracks = _tracks.value.associateBy { it.id }
        val onlineTracks = _onlineFavorites.value.associateBy {
            "${it.source}:${it.trackId}"
        }

        val savedOrder = libraryDao.getFavoriteOrder().toMutableList()
        val knownKeys = savedOrder.map { "${it.source}:${it.trackId}" }.toMutableSet()

        _favoriteIds.value.forEach { id ->
            val key = "local:$id"
            if (knownKeys.add(key)) {
                savedOrder.add(
                    FavoriteOrderEntity(
                        source = "local",
                        trackId = id.toString(),
                        position = savedOrder.size
                    )
                )
            }
        }

        _onlineFavorites.value.forEach { item ->
            val key = "${item.source}:${item.trackId}"
            if (knownKeys.add(key)) {
                savedOrder.add(
                    FavoriteOrderEntity(
                        source = item.source,
                        trackId = item.trackId,
                        position = savedOrder.size
                    )
                )
            }
        }

        if (savedOrder.size != libraryDao.getFavoriteOrder().size) {
            libraryDao.clearFavoriteOrder()
            libraryDao.insertFavoriteOrders(
                savedOrder.mapIndexed { index, item ->
                    item.copy(position = index)
                }
            )
        }

        _favoriteQueueItems.value = savedOrder.mapNotNull { entry ->
            when (entry.source) {
                "local" -> entry.trackId.toLongOrNull()
                    ?.let { localTracks[it] }
                    ?.let { PlaylistQueueItem.Local(it) }

                else -> onlineTracks["${entry.source}:${entry.trackId}"]
                    ?.let { favorite ->
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
                    }
            }
        }
    }

    fun refreshFavoritesOrder() {
        viewModelScope.launch {
            refreshFavoriteQueue()
        }
    }

    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val current = _favoriteQueueItems.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        _favoriteQueueItems.value = current

        viewModelScope.launch {
            val order = current.mapIndexed { index, item ->
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
            libraryDao.insertFavoriteOrders(order)
        }
    }

    fun playFavorites() {
        viewModelScope.launch {
            refreshFavoriteQueue()
            val items = _favoriteQueueItems.value
            if (items.isEmpty()) return@launch

            _playlistQueue.value = items
            _playlistQueueIndex.value = -1
            isPlaylistQueueActive = true
            startPlaylistQueueItem(0)
        }
    }
'''
vm.write_text(vm.read_text()[:start] + new_block + vm.read_text()[end:])
print("OK: ViewModel favorit terurut diperbarui")
print("PATCH VIEWMODEL SELESAI")
