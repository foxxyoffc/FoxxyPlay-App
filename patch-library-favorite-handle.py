from pathlib import Path

p = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")
s = p.read_text()

# Cari blok Favorites, bukan Playlist.
anchor = s.find("val itemKey = when (item) {", s.find("favoriteListState"))
if anchor < 0:
    raise SystemExit("Blok Favorites tidak ditemukan.")

start = s.find(".pointerInput(itemKey) {", anchor)
vertical = s.find("verticalAlignment = Alignment.CenterVertically", start)

if start < 0 or vertical < 0:
    raise SystemExit("Awal atau akhir gesture tidak ditemukan.")

# Ambil gesture lengkap sampai koma penutup modifier.
end = s.rfind("},", start, vertical)
if end < 0:
    raise SystemExit("Penutup gesture tidak ditemukan.")

gesture = s[start:end + 1]

# Hapus gesture dari modifier Row.
s = s[:start] + s[end + 2:]

# Cari ikon handle Favorites.
handle_anchor = s.find('text = "☰"', anchor)
if handle_anchor < 0:
    raise SystemExit("Teks handle tidak ditemukan.")

text_start = s.rfind("Text(", anchor, handle_anchor)
style_end = s.find("style = MaterialTheme.typography.titleMedium", handle_anchor)
text_end = s.find(")", style_end)

if text_start < 0 or style_end < 0 or text_end < 0:
    raise SystemExit("Blok Text handle tidak ditemukan.")

text_end += 1

new_handle = """Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            """ + gesture + """,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "☰",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }"""

s = s[:text_start] + new_handle + s[text_end:]

p.write_text(s)
print("SUKSES: Gesture Favorites sekarang hanya aktif pada handle ☰.")
