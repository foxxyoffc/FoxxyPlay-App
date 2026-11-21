from pathlib import Path
import shutil

path = Path("app/src/main/java/com/foxxy/play/feature/home/HomeScreen.kt")
text = path.read_text(encoding="utf-8")

old = '''    val albumPicks = remember(discover) {
        discover
            .filter { it.album.isNotBlank() }
            .distinctBy { "${it.album.lowercase()}-${it.artist.lowercase()}" }
    }'''

new = '''    // Bagi rekomendasi secara acak agar kedua section tidak berisi lagu sama.
    val homePicks = remember(discover) {
        discover.shuffled()
    }

    val discoverPicks = remember(homePicks) {
        homePicks.take(8)
    }

    val discoverAlbumKeys = remember(discoverPicks) {
        discoverPicks.map {
            "${it.album.lowercase()}-${it.artist.lowercase()}"
        }.toSet()
    }

    val albumPicks = remember(homePicks, discoverAlbumKeys) {
        homePicks.drop(8)
            .filter { it.album.isNotBlank() }
            .filter {
                "${it.album.lowercase()}-${it.artist.lowercase()}" !in discoverAlbumKeys
            }
            .distinctBy { "${it.album.lowercase()}-${it.artist.lowercase()}" }
    }'''

if old not in text:
    raise SystemExit("Blok albumPicks tidak ditemukan. File tidak diubah.")

shutil.copy2(path, str(path) + ".before-random-sections")
text = text.replace(old, new, 1)

text = text.replace(
    "discover.take(12),",
    "discoverPicks,",
    1
)

path.write_text(text, encoding="utf-8")
print("Discover Picks dan Album berhasil dipisahkan.")
