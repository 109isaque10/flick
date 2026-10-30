package com.stremflix.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/** TMDB serves fixed image buckets; the app selects a set once per process. */
private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"

private fun String?.tmdbImage(bucket: String): String? =
    this?.let { if (it.startsWith("http")) it else "$TMDB_IMAGE_BASE$bucket$it" }

/**
 * Image bucket sizes chosen once per process from the device class.
 *
 * The Android TV memory guidance is explicit that an app must not decode images
 * larger than the device UI needs, and low-RAM boxes (1 GB / 720p) report
 * [android.app.ActivityManager.isLowRamDevice] == true. Every value stays within
 * the bucket list TMDB documents for that image type.
 */
object TmdbImageSizes {
    var poster: String = "w185"
    var posterLarge: String = "w342"
    var backdrop: String = "w500"
    var backdropLarge: String = "w780"
    var logo: String = "w500"
    var still: String = "w185"

    fun configureForDevice(lowRam: Boolean, uiWidthPx: Int) {
        if (lowRam || uiWidthPx <= 1280) {
            poster = "w154"
            posterLarge = "w185"
            backdrop = "w300"
            backdropLarge = "w500"
            logo = "w300"
            still = "w92"
        } else {
            poster = "w185"
            posterLarge = "w342"
            backdrop = "w500"
            backdropLarge = "w780"
            logo = "w500"
            still = "w185"
        }
    }
}

@Immutable
data class MediaItem(
    val id: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseYear: String,
    val voteAverage: Double,
    val matchPercentage: Int = ((voteAverage * 10).toInt().coerceIn(65, 99)),
    val contentRating: String = "",
    val durationOrSeasons: String = "",
    val qualityBadges: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: String = "",
    // Showrunners/creators, only filled for TV titles.
    val creators: List<String> = emptyList(),
    // YouTube video key from TMDB (/videos). Null when the title has no trailer.
    val trailerKey: String? = null,
    val trailerName: String = "",
    val imdbId: String? = null,
    val logoPath: String? = null,
    val ratingsCluster: RatingsCluster = RatingsCluster()
) {
    val cardPosterUrl: String?
        get() = posterPath.tmdbImage(TmdbImageSizes.poster)

    val cardBackdropUrl: String?
        get() = backdropPath.tmdbImage(TmdbImageSizes.backdrop)

    val fullPosterUrl: String?
        get() = posterPath.tmdbImage(TmdbImageSizes.posterLarge)

    val fullBackdropUrl: String?
        get() = backdropPath.tmdbImage(TmdbImageSizes.backdropLarge)

    /**
     * Transparent title artwork ("logo") served by TMDB. Netflix 2020 never set a
     * plain text title over the hero artwork, so every hero renders this when the
     * title has one and only falls back to [title] when TMDB has no logo.
     */
    val logoUrl: String?
        get() = logoPath.tmdbImage(TmdbImageSizes.logo)
}

// Unknown scores are null so the UI can omit them instead of inventing values.
data class RatingsCluster(
    val imdbScore: String? = null,
    val tmdbScore: String? = null,
    val rottenTomatoesScore: String? = null,
    val metacriticScore: String? = null,
    val netflixMatch: String? = null
)

enum class SpotlightType {
    NONE,
    N_SERIES,
    BIG_HOME_TRAILER,
    TOP_250
}

@Immutable
data class ContentRow(
    val id: String,
    val title: String,
    @StringRes val titleRes: Int? = null,
    val items: List<MediaItem>,
    val spotlightType: SpotlightType = SpotlightType.NONE
)

data class SeasonModel(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterPath: String?
)

data class EpisodeModel(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val stillPath: String?,
    val durationMinutes: Int,
    val airDate: String?
) {
    val fullStillUrl: String?
        get() = stillPath.tmdbImage(TmdbImageSizes.still)
}
