from pathlib import Path
import shutil

p = Path("app/src/main/java/com/foxxy/play/feature/library/LibraryScreen.kt")
lines = p.read_text().splitlines(keepends=True)

def next_code_line(index):
    for k in range(index + 1, len(lines)):
        if lines[k].strip():
            return k
    return -1

start = -1
end = -1

for i, line in enumerate(lines):
    if line.strip() != "} else {":
        continue

    n = next_code_line(i)
    if n != -1 and lines[n].strip().startswith("LazyColumn("):
        for j in range(n + 1, len(lines)):
            if lines[j].strip() != "} else {":
                continue
            m = next_code_line(j)
            if m != -1 and lines[m].strip().startswith("Row("):
                start, end = i, j
                break
    if start != -1:
        break

if start == -1 or end == -1:
    raise SystemExit("[GAGAL] Blok duplikat tidak ditemukan. File tidak diubah.")

backup = p.with_suffix(".kt.before-structure-fix")
shutil.copy2(p, backup)

removed = lines[start:end]
del lines[start:end]
p.write_text("".join(lines))

print(f"[OK] Blok duplikat baris lama {start+1}-{end} dihapus.")
print(f"[OK] Backup: {backup}")
print("[OK] Struktur LibraryScreen diperbaiki.")
