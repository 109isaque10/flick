package com.stremflix.data.model

import androidx.annotation.StringRes

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
        get() = posterPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w185$it" }

    val cardBackdropUrl: String?
        get() = backdropPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }

    val fullPosterUrl: String?
        get() = posterPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w342$it" }

    val fullBackdropUrl: String?
        get() = backdropPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it" }

    /**
     * Transparent title artwork ("logo") served by TMDB. Netflix 2020 never set a
     * plain text title over the hero artwork, so every hero renders this when the
     * title has one and only falls back to [title] when TMDB has no logo.
     */
    val logoUrl: String?
        get() = logoPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }
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
        get() = stillPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w185$it" }
}
