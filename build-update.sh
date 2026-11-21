#!/data/data/com.termux/files/usr/bin/bash

set -Eeuo pipefail
cd "$HOME/FoxxyPlay"

GRADLE_FILE="app/build.gradle.kts"
PUBLIC_APK="app/build/outputs/apk/public/debug/app-public-debug.apk"
DEVELOPER_APK="app/build/outputs/apk/developer/debug/app-developer-debug.apk"

DOWNLOAD="/sdcard/Download"
PUBLIC_DEST="$DOWNLOAD/FoxxyPlay-Public-latest.apk"
DEVELOPER_DEST="$DOWNLOAD/FoxxyPlay-Developer-latest.apk"

BACKUP="$(mktemp)"
cp "$GRADLE_FILE" "$BACKUP"

BUMPED=0

cleanup() {
    STATUS=$?
    if [ "$STATUS" -ne 0 ] && [ "$BUMPED" -eq 1 ]; then
        cp "$BACKUP" "$GRADLE_FILE"
        echo "Build gagal. Versi dikembalikan."
    fi
    rm -f "$BACKUP"
}

trap cleanup EXIT

echo "======================================"
echo "       FOXXYPLAY AUTO UPDATE"
echo "======================================"

echo "[1/4] Menaikkan versi aplikasi..."

BUMPED=1

python - <<'PY'
from pathlib import Path
import re

p = Path("app/build.gradle.kts")
s = p.read_text()

s, count = re.subn(
    r'versionCode\s*=\s*(\d+)',
    lambda m: f'versionCode = {int(m.group(1)) + 1}',
    s,
    count=1
)

if count != 1:
    raise SystemExit("versionCode tidak ditemukan")

def bump(m):
    parts = [int(x) for x in m.group(1).split(".")]
    parts[-1] += 1
    return 'versionName = "' + ".".join(map(str, parts)) + '"'

s, count = re.subn(
    r'versionName\s*=\s*"(\d+(?:\.\d+)+)"',
    bump,
    s,
    count=1
)

if count != 1:
    raise SystemExit("versionName tidak ditemukan")

p.write_text(s)
print("Versi berhasil diperbarui.")
PY

grep -nE 'versionCode|versionName' "$GRADLE_FILE"

echo
echo "[2/4] Build Public dan Developer APK..."

./gradlew :app:assemblePublicDebug :app:assembleDeveloperDebug

if [ ! -f "$PUBLIC_APK" ]; then
    echo "ERROR: Public APK tidak ditemukan."
    exit 1
fi

if [ ! -f "$DEVELOPER_APK" ]; then
    echo "ERROR: Developer APK tidak ditemukan."
    exit 1
fi

BUMPED=0

echo
echo "[3/4] Menyalin kedua APK..."

mkdir -p "$DOWNLOAD"

cp "$PUBLIC_APK" "$PUBLIC_DEST"
cp "$DEVELOPER_APK" "$DEVELOPER_DEST"

echo "Public APK    : $PUBLIC_DEST"
echo "Developer APK : $DEVELOPER_DEST"

echo
echo "[4/4] Pilih APK yang ingin dipasang:"
echo "1. Public APK"
echo "2. Developer APK"
echo "3. Tidak install sekarang"
echo "4. Install keduanya"

read -r -p "Pilihan [1-4]: " choice

case "$choice" in
    1)
        termux-open "$PUBLIC_DEST"
        ;;
    2)
        termux-open "$DEVELOPER_DEST"
        ;;
    3)
        echo "APK tersimpan di folder Download."
        ;;
    4)
        echo "Membuka installer Public APK..."
        termux-open "$PUBLIC_DEST"
        echo
        read -r -p "Selesaikan instalasi Public, lalu tekan Enter untuk Developer..."
        termux-open "$DEVELOPER_DEST"
        ;;
    *)
        echo "Pilihan tidak valid. Kedua APK tetap tersimpan."
        ;;
esac

echo
echo "======================================"
echo "       BUILD UPDATE BERHASIL"
echo "======================================"
