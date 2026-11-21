from pathlib import Path
import re
import shutil

base = Path("app/src/main/java/com/foxxy/play/feature/player")
vm = base / "FoxxyPlayerViewModel.kt"
ui = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")

s = vm.read_text()
u = ui.read_text()

def replace_once(text, pattern, replacement, label, flags=0):
    result, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"{label}: pola tidak ditemukan atau tidak unik. Tidak ada perubahan disimpan.")
    print(f"[OK] {label}")
    return result

s = replace_once(
    s,
    r'override fun onPlaybackStateChanged\(playbackState: Int\)\s*\{\s*updateDuration\(\)\s*\}',
    '''override fun onPlaybackStateChanged(playbackState: Int) {
            updateDuration()
            if (playbackState == Player.STATE_ENDED && isPlaylistQueueActive) {
                advancePlaylistQueue()
            }
        }''',
    "Nomor 2 - Listener"
)

s = replace_once(
    s,
    r'fun playOnlineTrack\(track: OnlineMusicTrack\)\s*\{\s*val activePlayer = player',
    '''fun playOnlineTrack(
        track: OnlineMusicTrack,
        fromPlaylist: Boolean = false
    ) {
        if (!fromPlaylist) {
            isPlaylistQueueActive = false
        }
        val activePlayer = player''',
    "Nomor 3 - Spotify"
)

s = replace_once(
    s,
    r'(fun playTrack\(track: MusicTrack\)\s*\{)',
    r'\1\n        isPlaylistQueueActive = false',
    "Nomor 4 - Lokal"
)

s = replace_once(
    s,
    r'fun nextTrack\(\)\s*\{.*?\n\s*fun seekTo\(position: Long\)',
    '''fun nextTrack() {
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

    fun seekTo(position: Long)''',
    "Nomor 5 - Next Previous",
    flags=re.S
)

u = replace_once(
    u,
    r'viewModel\.loadSelectedPlaylistQueue\(\)',
    'viewModel.loadSelectedPlaylistQueue(playImmediately = true)',
    "Nomor 6 - Tombol"
)

shutil.copy2(vm, str(vm) + ".bak")
shutil.copy2(ui, str(ui) + ".bak")

vm.write_text(s)
ui.write_text(u)

print("\\nPATCH BERHASIL DITERAPKAN.")
print("Backup kedua file berhasil dibuat.")
