from pathlib import Path
import shutil

root = Path("app/src/main/java/com/foxxy/play")

files = {
    "entities": root / "data/local/LibraryEntities.kt",
    "dao": root / "data/local/LibraryDao.kt",
    "database": root / "data/local/FoxxyDatabase.kt",
    "viewmodel": root / "feature/player/FoxxyPlayerViewModel.kt",
}

src = {k: p.read_text() for k, p in files.items()}

# Jangan menulis apa pun jika struktur tidak cocok.
checks = [
    ("entities", "data class RecentPlayedEntity("),
    ("dao", "interface LibraryDao {"),
    ("database", "version = 6"),
    ("database", "private val MIGRATION_5_6"),
    ("viewmodel", "private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())"),
    ("viewmodel", "fun scanLocalMusic() {"),
    ("viewmodel", "_tracks.value = result"),
    ("viewmodel", "init {\n        viewModelScope.launch {"),
]
for key, anchor in checks:
    if anchor not in src[key]:
        raise SystemExit(f"ANCHOR TIDAK DITEMUKAN: {key}: {anchor}")

if "LocalMusicCacheEntity" in src["entities"]:
    raise SystemExit("Entity cache sudah ada. Patch dibatalkan agar tidak duplikat.")

# 1. Entity cache musik lokal
src["entities"] = src["entities"].replace(
    "@Entity(tableName = \"recently_played\")",
    """@Entity(tableName = "local_music_cache")
data class LocalMusicCacheEntity(
    @PrimaryKey val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)

@Entity(tableName = "recently_played")""",
    1
)

# 2. DAO
src["dao"] = src["dao"].replace(
    "interface LibraryDao {",
    """interface LibraryDao {

    @Query("SELECT * FROM local_music_cache ORDER BY title COLLATE NOCASE ASC")
    fun observeLocalMusicCache(): Flow<List<LocalMusicCacheEntity>>

    @Query("DELETE FROM local_music_cache")
    suspend fun clearLocalMusicCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalMusicCache(items: List<LocalMusicCacheEntity>)
""",
    1
)

# 3. Database version 7 + migration 6 -> 7
src["database"] = src["database"].replace(
    "        FavoriteOrderEntity::class\n",
    "        FavoriteOrderEntity::class,\n        LocalMusicCacheEntity::class\n",
    1
).replace(
    "version = 6,",
    "version = 7,",
    1
)

migration = """
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    \"\"\"
                    CREATE TABLE IF NOT EXISTS local_music_cache (
                        trackId INTEGER NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        duration INTEGER NOT NULL
                    )
                    \"\"\".trimIndent()
                )
            }
        }

"""

src["database"] = src["database"].replace(
    "        @Volatile\n        private var INSTANCE",
    migration + "        @Volatile\n        private var INSTANCE",
    1
)

src["database"] = src["database"].replace(
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)",
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)",
    1
)

if "MIGRATION_6_7" not in src["database"]:
    raise SystemExit("Migrasi 6 ke 7 gagal disiapkan.")

# 4. ViewModel imports
src["viewmodel"] = src["viewmodel"].replace(
    "import com.foxxy.play.data.local.FavoriteEntity",
    "import com.foxxy.play.data.local.LocalMusicCacheEntity\nimport com.foxxy.play.data.local.FavoriteEntity",
    1
)

# Muat cache saat ViewModel dibuat.
src["viewmodel"] = src["viewmodel"].replace(
    "init {\n        viewModelScope.launch {",
    """init {
        // Tampilkan cache musik lokal segera setelah aplikasi dibuka.
        viewModelScope.launch {
            libraryDao.observeLocalMusicCache().collect { cached ->
                if (cached.isNotEmpty()) {
                    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    _tracks.value = cached.map { item ->
                        MusicTrack(
                            id = item.trackId,
                            title = item.title,
                            artist = item.artist,
                            album = item.album,
                            duration = item.duration,
                            uri = ContentUris.withAppendedId(
                                collection,
                                item.trackId
                            )
                        )
                    }
                }
            }
        }

        viewModelScope.launch {""",
    1
)

# Simpan hasil scan ke database.
src["viewmodel"] = src["viewmodel"].replace(
    "_tracks.value = result",
    """_tracks.value = result

                // Perbarui cache permanen setelah pemindaian berhasil.
                libraryDao.clearLocalMusicCache()
                if (result.isNotEmpty()) {
                    libraryDao.insertLocalMusicCache(
                        result.map { track ->
                            LocalMusicCacheEntity(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                duration = track.duration
                            )
                        }
                    )
                }""",
    1
)

# Backup semua file sebelum menulis.
for key, path in files.items():
    backup = Path(str(path) + ".before-local-cache")
    shutil.copy2(path, backup)

for key, path in files.items():
    path.write_text(src[key])

print("PATCH CACHE LOCAL MUSIC BERHASIL")
print("Database: versi 7")
print("Cache dimuat otomatis saat ViewModel dibuat.")
print("Hasil scan disimpan ke Room.")
print("Backup file dibuat dengan akhiran .before-local-cache")
