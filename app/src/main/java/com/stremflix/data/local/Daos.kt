package com.stremflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :id LIMIT 1")
    suspend fun getWatchHistoryById(id: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE tmdbId = :tmdbId ORDER BY lastWatchedAt DESC LIMIT 1")
    suspend fun getLatestForMedia(tmdbId: Int): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteWatchHistory(id: String)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    fun isInWatchlistFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    suspend fun isInWatchlist(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun removeFromWatchlist(id: String)
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM external_id_map WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getExternalId(tmdbId: Int): ExternalIdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExternalId(entity: ExternalIdEntity)

    @Query("SELECT * FROM metadata_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getMetadata(key: String): MetadataCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(entity: MetadataCacheEntity)
}

@Dao
interface MediaMetadataDao {
    @Query("SELECT * FROM media_metadata WHERE idKey = :idKey LIMIT 1")
    suspend fun getMediaMetadata(idKey: String): MediaMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMediaMetadata(entity: MediaMetadataEntity)
}

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastWatchedAt DESC")
    fun getAllWatchProgress(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE id = :id LIMIT 1")
    suspend fun getWatchProgressById(id: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE tmdbId = :tmdbId AND mediaType = 'tv'")
    fun getWatchProgressForShow(tmdbId: Int): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE tmdbId = :tmdbId AND mediaType = :mediaType ORDER BY lastWatchedAt DESC LIMIT 1")
    suspend fun getLatestWatchProgressForMedia(tmdbId: Int, mediaType: String): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchProgress(item: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE id = :id")
    suspend fun deleteWatchProgress(id: String)
}

