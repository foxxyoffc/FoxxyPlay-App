from pathlib import Path
import re

p = Path("app/src/main/java/com/foxxy/play/feature/player/FoxxyPlayerViewModel.kt")
s = p.read_text()

pattern = r"    fun playFavorites\(\)\s*\{.*?(?=    fun loadSelectedPlaylistQueue)"
replacement = r'''    private suspend fun refreshFavoriteQueue() {
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

'''
new, count = re.subn(pattern, replacement, s, count=1, flags=re.S)

if count != 1:
    raise SystemExit("GAGAL: blok playFavorites() tidak ditemukan; file tidak diubah.")

p.write_text(new)
print("OK: refreshFavoriteQueue(), reorderFavorites(), dan playFavorites() ditambahkan.")
