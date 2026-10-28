package com.stremflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String, // "movie_{tmdbId}" or "tv_{tmdbId}"
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "external_id_map")
data class ExternalIdEntity(
    @PrimaryKey val tmdbId: Int,
    val imdbId: String?,
    val tvdbId: Int? = null,
    val lastFetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey val cacheKey: String, // e.g. "details_movie_550" or "omdb_tt0137523"
    val jsonContent: String,
    val isCacheForever: Boolean = false,
    val lastFetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_metadata")
data class MediaMetadataEntity(
    @PrimaryKey val idKey: String, // "movie_550" or "tv_1399"
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseYear: String,
    val voteAverage: Double,
    val imdbId: String? = null,
    val contentRating: String? = null,
    val status: String? = null, // "Ended", "Canceled", "Released", etc.
    val isCacheForever: Boolean = false,
    val jsonContent: String? = null,
    val lastFetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val id: String, // format: "movie_{tmdbId}" or "tv_{tmdbId}_s{season}_e{episode}"
    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val positionMs: Long,
    val durationMs: Long,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()) else 0f
}

