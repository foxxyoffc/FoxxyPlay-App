from pathlib import Path

base = Path("app/src/main/java/com/foxxy/play")
entities = base / "data/local/LibraryEntities.kt"
dao = base / "data/local/LibraryDao.kt"
db = base / "data/local/FoxxyDatabase.kt"

def patch(path, old, new):
    s = path.read_text()
    if old not in s:
        raise SystemExit(f"MARKER TIDAK DITEMUKAN: {path}")
    backup = path.with_suffix(path.suffix + ".bak")
    if not backup.exists():
        backup.write_text(s)
    path.write_text(s.replace(old, new, 1))
    print("OK:", path)

patch(
    entities,
    '@Entity(\n    tableName = "playlist_order",',
    '''@Entity(
    tableName = "favorite_order",
    primaryKeys = ["source", "trackId"]
)
data class FavoriteOrderEntity(
    val source: String,
    val trackId: String,
    val position: Int
)

@Entity(
    tableName = "playlist_order",'''
)

patch(
    dao,
    '    @Query("SELECT * FROM favorites ORDER BY trackId DESC")',
    '''    @Query("SELECT * FROM favorite_order ORDER BY position ASC")
    suspend fun getFavoriteOrder(): List<FavoriteOrderEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM favorite_order")
    suspend fun getNextFavoritePosition(): Int

    @Query("DELETE FROM favorite_order")
    suspend fun clearFavoriteOrder()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteOrders(items: List<FavoriteOrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteOrder(item: FavoriteOrderEntity)

    @Query("DELETE FROM favorite_order WHERE source = :source AND trackId = :trackId")
    suspend fun removeFavoriteOrder(source: String, trackId: String)

    @Query("SELECT * FROM favorites ORDER BY trackId DESC")'''
)

patch(
    db,
    '        PlaylistOrderEntity::class\n    ],\n    version = 5,',
    '        PlaylistOrderEntity::class,\n        FavoriteOrderEntity::class\n    ],\n    version = 6,'
)

patch(
    db,
    '        @Volatile\n        private var INSTANCE: FoxxyDatabase? = null',
    '''        private val MIGRATION_5_6 = object : Migration(5, 6) {
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
                    SELECT 'local', CAST(trackId AS TEXT),
                        ROW_NUMBER() OVER (ORDER BY trackId DESC) - 1
                    FROM favorites
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO favorite_order(source, trackId, position)
                    SELECT 'spotify', trackId,
                        (SELECT COUNT(*) FROM favorites) +
                        ROW_NUMBER() OVER (ORDER BY title COLLATE NOCASE, trackId) - 1
                    FROM online_favorites
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: FoxxyDatabase? = null'''
)

patch(
    db,
    '.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)',
    '.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)'
)

print("PATCH DATABASE SELESAI")
