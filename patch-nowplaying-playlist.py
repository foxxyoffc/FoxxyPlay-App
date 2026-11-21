from pathlib import Path
import shutil

base = Path("app/src/main/java/com/foxxy/play/feature")
vm = base / "player/FoxxyPlayerViewModel.kt"
ui = base / "player/NowPlayingScreen.kt"

for p in (vm, ui):
    if not p.exists():
        raise SystemExit(f"[GAGAL] File tidak ditemukan: {p}")
    shutil.copy2(p, str(p) + ".before-nowplaying-playlist")

v = vm.read_text()
u = ui.read_text()

# 1. ViewModel: add lagu ke playlist berdasarkan ID tujuan
marker = "    fun createPlaylist(name: String) {"
addition = r'''
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

'''
if marker not in v:
    raise SystemExit("[GAGAL] Titik penyisipan ViewModel tidak ditemukan.")
if "fun addCurrentTrackToPlaylist(" not in v:
    v = v.replace(marker, addition + marker, 1)

# 2. Now Playing: state dialog
state_marker = "    val onlineFavoriteIds by viewModel.onlineFavoriteIds.collectAsState()"
state_add = '''
    val playlists by viewModel.playlists.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
'''
if state_marker not in u:
    raise SystemExit("[GAGAL] State favorit Now Playing tidak ditemukan.")
if "var showPlaylistDialog" not in u:
    u = u.replace(state_marker, state_marker + "\n" + state_add, 1)

# 3. Tombol Playlist di samping favorit
favorite_marker = '''            IconButton(
                enabled = hasTrack,
                onClick = {
                    onlineTrack?.let { viewModel.toggleOnlineFavorite(it) }
                        ?: track?.let { viewModel.toggleFavorite(it) }
                }
            ) {'''
button_add = '''            TextButton(
                enabled = hasTrack,
                onClick = { showPlaylistDialog = true }
            ) {
                Text("Playlist")
            }

'''
if favorite_marker not in u:
    raise SystemExit("[GAGAL] Tombol favorit tidak ditemukan.")
if 'Text("Playlist")' not in u:
    u = u.replace(favorite_marker, button_add + favorite_marker, 1)

# 4. Dialog pemilihan / pembuatan playlist
tail = '''        Spacer(Modifier.weight(1f))
    }
}

private fun formatTime'''
dialog_tail = '''        Spacer(Modifier.weight(1f))
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("Belum ada playlist. Buat playlist baru di bawah.")
                    } else {
                        Text(
                            "Pilih playlist tujuan",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    viewModel.addCurrentTrackToPlaylist(playlist.id)
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    playlist.name,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Buat playlist baru",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        label = { Text("Nama playlist") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPlaylistName.isNotBlank() && hasTrack,
                    onClick = {
                        viewModel.createPlaylistWithCurrentTrack(newPlaylistName)
                        newPlaylistName = ""
                        showPlaylistDialog = false
                    }
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatTime'''
if tail not in u:
    raise SystemExit("[GAGAL] Akhir NowPlayingScreen tidak cocok; UI belum diubah.")
if "Add to Playlist" not in u:
    u = u.replace(tail, dialog_tail, 1)

vm.write_text(v)
ui.write_text(u)

print("[OK] ViewModel: tambah lagu ke playlist tujuan.")
print("[OK] ViewModel: buat playlist baru sekaligus masukkan lagu.")
print("[OK] Now Playing: tombol Playlist dan dialog ditambahkan.")
print("[OK] Backup kedua file dibuat.")
