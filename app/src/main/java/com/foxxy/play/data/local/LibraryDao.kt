package com.foxxy.play.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM local_music_cache ORDER BY title COLLATE NOCASE ASC")
    fun observeLocalMusicCache(): Flow<List<LocalMusicCacheEntity>>

    @Query("DELETE FROM local_music_cache")
    suspend fun clearLocalMusicCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalMusicCache(items: List<LocalMusicCacheEntity>)


    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC")
    fun observeRecentPlayed(): Flow<List<RecentPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentlyPlayed(item: RecentPlayedEntity)

    @Query("DELETE FROM recently_played")
    suspend fun clearRecentlyPlayed()

    @Query("SELECT * FROM favorite_order ORDER BY position ASC")
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

    @Query("SELECT * FROM favorites ORDER BY trackId DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)

    @Query("SELECT * FROM online_favorites ORDER BY title COLLATE NOCASE")
    fun observeOnlineFavorites(): Flow<List<OnlineFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOnlineFavorite(item: OnlineFavoriteEntity)

    @Query("DELETE FROM online_favorites WHERE source = :source AND trackId = :trackId")
    suspend fun removeOnlineFavorite(source: String, trackId: String)


    @Query(
        "SELECT * FROM playlist_online_songs WHERE playlistId = :playlistId ORDER BY title COLLATE NOCASE"
    )
    suspend fun getOnlinePlaylistSongs(
        playlistId: Long
    ): List<OnlinePlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOnlineSongToPlaylist(item: OnlinePlaylistSongEntity)

    @Query(
        "DELETE FROM playlist_online_songs WHERE playlistId = :playlistId AND source = :source AND trackId = :trackId"
    )
    suspend fun removeOnlineSongFromPlaylist(
        playlistId: Long,
        source: String,
        trackId: String
    )

    
    @Query("SELECT * FROM playlist_order WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistOrder(playlistId: Long): List<PlaylistOrderEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_order WHERE playlistId = :playlistId")
    suspend fun getNextPlaylistPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylistOrder(item: PlaylistOrderEntity)

    @Query("DELETE FROM playlist_order WHERE playlistId = :playlistId AND source = :source AND trackId = :trackId")
    suspend fun removePlaylistOrder(playlistId: Long, source: String, trackId: String)

    @Query("DELETE FROM playlist_order WHERE playlistId = :playlistId")
    suspend fun clearPlaylistOrder(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistOrders(items: List<PlaylistOrderEntity>)

@Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query(
        "SELECT trackId FROM playlist_songs WHERE playlistId = :playlistId"
    )
    suspend fun getPlaylistTrackIds(playlistId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(item: PlaylistSongEntity)

    @Query(
        "DELETE FROM playlist_songs WHERE playlistId = :playlistId AND trackId = :trackId"
    )
    suspend fun removeSongFromPlaylist(
        playlistId: Long,
        trackId: Long
    )


    // Downloads
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE source = :source AND trackId = :trackId LIMIT 1")
    suspend fun getDownload(source: String, trackId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownload(item: DownloadEntity)

    @Query("DELETE FROM downloads WHERE source = :source AND trackId = :trackId")
    suspend fun removeDownload(source: String, trackId: String)

    @Query("SELECT * FROM downloads WHERE audioPath = :audioPath LIMIT 1")
    suspend fun getDownloadByPath(audioPath: String): DownloadEntity?

}
