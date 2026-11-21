from pathlib import Path
import shutil

p = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")
backup = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt.before-structure-fix")

if not backup.exists():
    raise SystemExit("[GAGAL] Backup pemulihan tidak ditemukan.")

shutil.copy2(p, p.with_suffix(".kt.damaged"))
shutil.copy2(backup, p)

lines = p.read_text().splitlines(keepends=True)

# Cari indentasi if utama selectedPlaylistId
outer_indent = None
for line in lines:
    if line.strip() == "if (selectedPlaylistId != null) {":
        outer_indent = len(line) - len(line.lstrip())
        break

if outer_indent is None:
    raise SystemExit("[GAGAL] if utama tidak ditemukan.")

def next_nonempty(i):
    for j in range(i + 1, len(lines)):
        if lines[j].strip():
            return j
    return -1

start = -1
end = -1

for i, line in enumerate(lines):
    indent = len(line) - len(line.lstrip())
    if indent != outer_indent or line.strip() != "} else {":
        continue

    n = next_nonempty(i)
    if n >= 0 and lines[n].strip().startswith("LazyColumn("):
        start = i
        break

if start < 0:
    raise SystemExit("[GAGAL] Blok duplikat utama tidak ditemukan.")

for i in range(start + 1, len(lines)):
    indent = len(lines[i]) - len(lines[i].lstrip())
    if indent != outer_indent or lines[i].strip() != "} else {":
        continue

    n = next_nonempty(i)
    if n >= 0 and lines[n].strip().startswith("Row("):
        end = i
        break

if end < 0:
    raise SystemExit("[GAGAL] Batas akhir blok duplikat tidak ditemukan.")

del lines[start:end]
p.write_text("".join(lines))

print(f"[OK] File dipulihkan dari backup.")
print(f"[OK] Blok duplikat dihapus: baris lama {start+1}-{end}.")
print("[OK] Tombol Play Playlist tetap dipertahankan.")
