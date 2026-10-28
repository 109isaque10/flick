package com.stremflix.data.repository

import com.stremflix.R
import com.stremflix.data.local.CacheDao
import com.stremflix.data.local.ExternalIdEntity
import com.stremflix.data.local.MediaMetadataDao
import com.stremflix.data.local.MediaMetadataEntity
import com.stremflix.data.local.SettingsDataStore
import com.stremflix.data.local.WatchHistoryDao
import com.stremflix.data.local.WatchHistoryEntity
import com.stremflix.data.local.WatchProgressDao
import com.stremflix.data.local.WatchProgressEntity
import com.stremflix.data.local.WatchlistDao
import com.stremflix.data.local.WatchlistEntity
import com.stremflix.data.model.ContentRow
import com.stremflix.data.model.EpisodeModel
import com.stremflix.data.model.MediaItem
import com.stremflix.data.model.RatingsCluster
import com.stremflix.data.model.SeasonModel
import com.stremflix.data.remote.ApiClient
import com.stremflix.data.remote.TmdbMediaDto
import com.stremflix.data.model.SpotlightType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MediaRepository(
    private val cacheDao: CacheDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao,
    private val settingsDataStore: SettingsDataStore,
    private val mediaMetadataDao: MediaMetadataDao? = null,
    private val watchProgressDao: WatchProgressDao? = null
) {
    private suspend fun getEffectiveTmdbApiKey(): String {
        return settingsDataStore.tmdbApiKey.first()
    }

    // TTL Constants
    companion object {
        private const val TTL_EXTERNAL_ID_MS = 72 * 60 * 60 * 1000L // 72h External ID Map TTL
        private const val TTL_METADATA_MS = 24 * 60 * 60 * 1000L    // 24h General Metadata TTL
    }

    val watchHistory: Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllWatchHistory()
    val watchlist: Flow<List<WatchlistEntity>> = watchlistDao.getWatchlist()

    suspend fun getHomeContentRows(): List<ContentRow> = coroutineScope {
        val rows = mutableListOf<ContentRow>()
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val language = settingsDataStore.tmdbLanguage.first()
        val region = settingsDataStore.tmdbRegion.first()
        val isTraktConnected =
            !settingsDataStore.traktAccessToken.first().isNullOrBlank() || settingsDataStore.traktApiKey.first()
                .isNotBlank()

        val historyList = watchHistoryDao.getAllWatchHistory().first()

        val trendingDeferred = async {
            try {
                ApiClient.tmdbApi.getTrending(
                    type = "all",
                    timeWindow = "day",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { it.toMediaItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val popularMoviesDeferred = async {
            try {
                ApiClient.tmdbApi.getPopular(
                    type = "movie",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.map { it.toMediaItem("movie") }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val popularTvDeferred = async {
            try {
                ApiClient.tmdbApi.getPopular(
                    type = "tv",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.map { it.toMediaItem("tv") }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val recsDeferred = async {
            if (historyList.isNotEmpty()) {
                val lastWatched = historyList.first()
                try {
                    ApiClient.tmdbApi.getRecommendations(
                        type = lastWatched.mediaType,
                        id = lastWatched.tmdbId,
                        apiKey = tmdbApiKey,
                        language = language
                    ).results.map { it.toMediaItem(lastWatched.mediaType) }
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        }

        val actionDeferred = async {
            try {
                ApiClient.tmdbApi.discoverByGenre(
                    type = "movie",
                    genreId = 28,
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.map { it.toMediaItem("movie") }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // 1. Continue Watching
        if (historyList.isNotEmpty()) {
            val historyItems = historyList.take(10).map { entity ->
                MediaItem(
                    id = entity.tmdbId,
                    mediaType = entity.mediaType,
                    title = entity.title,
                    overview = entity.episodeTitle ?: "",
                    posterPath = entity.posterPath,
                    backdropPath = entity.backdropPath,
                    releaseYear = "",
                    voteAverage = 8.5
                )
            }
            rows.add(
                ContentRow(
                    id = "continue_watching",
                    title = "Continue Watching for You",
                    titleRes = R.string.row_continue_watching,
                    items = historyItems
                )
            )

            val recsItems = recsDeferred.await()
            if (recsItems.isNotEmpty()) {
                val lastWatched = historyList.first()
                rows.add(
                    ContentRow(
                        id = "based_on_last_watched",
                        title = "Because you watched ${lastWatched.title}",
                        titleRes = R.string.row_trending,
                        items = recsItems
                    )
                )
            }
        }

        // 2. Trending Today (Top 250 / Top 10 spotlight style)
        val trendingItems = trendingDeferred.await()
        if (trendingItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "trending_today",
                    title = "Top 10 / Trending Today",
                    titleRes = R.string.row_trending,
                    items = trendingItems,
                    spotlightType = SpotlightType.TOP_250
                )
            )
        }

        // 3. Bingeworthy TV Shows (N Series style)
        val popularTv = popularTvDeferred.await()
        if (popularTv.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "bingeworthy_tv",
                    title = "N Series & Bingeworthy Shows",
                    titleRes = R.string.row_top_tv,
                    items = popularTv,
                    spotlightType = SpotlightType.N_SERIES
                )
            )
        }

        // 4. Popular Movies (Big Home Trailer Spotlight style)
        val popularMovies = popularMoviesDeferred.await()
        if (popularMovies.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "popular_movies",
                    title = "Popular Movies Spotlight",
                    titleRes = R.string.row_popular_movies,
                    items = popularMovies,
                    spotlightType = SpotlightType.BIG_HOME_TRAILER
                )
            )
        }

        // 5. Action & Adventure
        val actionItems = actionDeferred.await()
        if (actionItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "action_adventure",
                    title = "Action & Adventure",
                    titleRes = R.string.row_action,
                    items = actionItems
                )
            )
        }

        rows
    }

    suspend fun getNewAndPopularContentRows(): List<ContentRow> = coroutineScope {
        val rows = mutableListOf<ContentRow>()
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val language = settingsDataStore.tmdbLanguage.first()
        val region = settingsDataStore.tmdbRegion.first()
        val isTraktAvailable =
            !settingsDataStore.traktAccessToken.first().isNullOrBlank() || settingsDataStore.traktApiKey.first()
                .isNotBlank()

        val dayDeferred = async {
            try {
                ApiClient.tmdbApi.getTrending(
                    type = "all",
                    timeWindow = "day",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { it.toMediaItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val weekDeferred = async {
            try {
                ApiClient.tmdbApi.getTrending(
                    type = "all",
                    timeWindow = "week",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region
                ).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { it.toMediaItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val popularMovieDeferred = async {
            try {
                ApiClient.tmdbApi.getPopular(
                    type = "movie",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region,
                    page = 1
                ).results.map { it.toMediaItem("movie") }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val popularSeriesDeferred = async {
            try {
                ApiClient.tmdbApi.getPopular(
                    type = "tv",
                    apiKey = tmdbApiKey,
                    language = language,
                    region = region,
                    page = 1
                ).results.map { it.toMediaItem("tv") }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val dayItems = dayDeferred.await()
        if (dayItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "trending_today",
                    title = "Top Trending Today",
                    titleRes = R.string.row_trending,
                    items = dayItems,
                    spotlightType = SpotlightType.TOP_250
                )
            )
        }

        val weekItems = weekDeferred.await()
        if (weekItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "trending_week",
                    title = "Trending This Week",
                    titleRes = R.string.row_trending,
                    items = weekItems,
                    spotlightType = SpotlightType.BIG_HOME_TRAILER
                )
            )
        }

        val popularMovieItems = popularMovieDeferred.await()
        if (popularMovieItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "trakt_popular",
                    title = if (isTraktAvailable) "Trakt Popular Right Now" else "Popular Movies Right Now",
                    titleRes = R.string.row_popular_movies,
                    items = popularMovieItems
                )
            )
        }

        val popularSeriesItems = popularSeriesDeferred.await()
        if (popularSeriesItems.isNotEmpty()) {
            rows.add(
                ContentRow(
                    id = "popular_series",
                    title = "Popular Series Right Now",
                    titleRes = R.string.row_top_tv,
                    items = popularSeriesItems,
                    spotlightType = SpotlightType.N_SERIES
                )
            )
        }

        rows
    }

    suspend fun searchMedia(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val language = settingsDataStore.tmdbLanguage.first()
        val region = settingsDataStore.tmdbRegion.first()
        return try {
            val res = ApiClient.tmdbApi.searchMulti(
                query = query,
                apiKey = tmdbApiKey,
                language = language,
                region = region
            )
            res.results
                .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                .sortedByDescending { it.popularity ?: 0.0 }
                .map { it.toMediaItem() }
                .distinctBy { "${it.mediaType}_${it.id}" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMediaByGenre(genreId: Int): List<MediaItem> {
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val language = settingsDataStore.tmdbLanguage.first()
        val region = settingsDataStore.tmdbRegion.first()
        return try {
            val movieRes = ApiClient.tmdbApi.discoverByGenre(
                type = "movie",
                genreId = genreId,
                apiKey = tmdbApiKey,
                language = language,
                region = region
            )
            val tvRes = ApiClient.tmdbApi.discoverByGenre(
                type = "tv",
                genreId = genreId,
                apiKey = tmdbApiKey,
                language = language,
                region = region
            )
            val movies = movieRes.results.map { it.toMediaItem("movie") }
            val tvs = tvRes.results.map { it.toMediaItem("tv") }
            (movies + tvs).distinctBy { "${it.mediaType}_${it.id}" }.sortedByDescending { it.voteAverage }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getImdbId(tmdbId: Int, mediaType: String): String? {
        val cached = cacheDao.getExternalId(tmdbId)
        val now = System.currentTimeMillis()

        // 72h TTL check for external ID map
        if (cached?.imdbId != null && (now - cached.lastFetchedAt < TTL_EXTERNAL_ID_MS)) {
            return cached.imdbId
        }

        return try {
            val tmdbApiKey = getEffectiveTmdbApiKey()
            val typePath = if (mediaType == "tv") "tv" else "movie"
            val res = ApiClient.tmdbApi.getExternalIds(type = typePath, id = tmdbId, apiKey = tmdbApiKey)
            val imdbId = res.imdbId
            if (imdbId != null) {
                cacheDao.saveExternalId(
                    ExternalIdEntity(
                        tmdbId = tmdbId,
                        imdbId = imdbId,
                        tvdbId = res.tvdbId,
                        lastFetchedAt = now
                    )
                )
            }
            imdbId ?: cached?.imdbId
        } catch (e: Exception) {
            cached?.imdbId
        }
    }

    private suspend fun getUserRegion(): String {
        val region = settingsDataStore.tmdbRegion.first()
        if (region.isNotBlank()) return region
        val lang = settingsDataStore.tmdbLanguage.first()
        if (lang.contains("-")) {
            return lang.split("-").last().uppercase()
        }
        return "US"
    }

    private fun mapToBrazilRating(cert: String?): String? {
        if (cert.isNullOrBlank()) return null
        val clean = cert.trim().uppercase()
        return when {
            clean == "LIVRE" || clean == "L" || clean == "G" || clean == "TV-G" || clean == "TV-Y" -> "L"
            clean == "10" || clean == "PG" || clean == "TV-PG" || clean == "TV-Y7" -> "10"
            clean == "12" || clean == "PG-13" -> "12"
            clean == "14" || clean == "TV-14" -> "14"
            clean == "16" || clean == "R" -> "16"
            clean == "18" || clean == "NC-17" || clean == "TV-MA" -> "18"
            clean.toIntOrNull() != null -> clean
            else -> cert
        }
    }

    suspend fun fetchContentRating(tmdbId: Int, mediaType: String): String? {
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val userRegion = getUserRegion()
        return try {
            val rawRating = if (mediaType == "movie") {
                val datesRes = ApiClient.tmdbApi.getMovieReleaseDates(id = tmdbId, apiKey = tmdbApiKey)
                val targetEntry = datesRes.results?.find { it.isoCode.equals(userRegion, ignoreCase = true) }
                val targetCert =
                    targetEntry?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                if (!targetCert.isNullOrBlank()) {
                    targetCert
                } else {
                    val usEntry = datesRes.results?.find { it.isoCode.equals("US", ignoreCase = true) }
                    val usCert = usEntry?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                    if (!usCert.isNullOrBlank()) usCert
                    else datesRes.results?.flatMap { it.releaseDates ?: emptyList() }
                        ?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                }
            } else {
                val ratingsRes = ApiClient.tmdbApi.getTvContentRatings(id = tmdbId, apiKey = tmdbApiKey)
                val targetEntry = ratingsRes.results?.find { it.isoCode.equals(userRegion, ignoreCase = true) }
                if (!targetEntry?.rating.isNullOrBlank()) {
                    targetEntry?.rating
                } else {
                    val usEntry = ratingsRes.results?.find { it.isoCode.equals("US", ignoreCase = true) }
                    if (!usEntry?.rating.isNullOrBlank()) usEntry?.rating
                    else ratingsRes.results?.firstOrNull { !it.rating.isNullOrBlank() }?.rating
                }
            }

            if (userRegion.equals("BR", ignoreCase = true)) {
                mapToBrazilRating(rawRating) ?: rawRating
            } else {
                rawRating
            }
        } catch (e: Exception) {
            null
        }
    }

    // Title artwork is deliberately not stored in Room: adding a column would
    // force a destructive migration, so it is memoised for the session instead.
    private val logoCache = java.util.Collections.synchronizedMap(mutableMapOf<String, String>())

    /**
     * Picks the title logo for the requested language, then language-neutral art,
     * then the best voted logo. Returns null when TMDB has none, in which case the
     * UI falls back to the text title.
     */
    private suspend fun fetchLogoPath(tmdbId: Int, mediaType: String): String? {
        val cacheKey = "${mediaType}_$tmdbId"
        logoCache[cacheKey]?.let { return it }
        val languageCode = settingsDataStore.tmdbLanguage.first().substringBefore('-').ifBlank { "en" }
        val path = try {
            val logos = ApiClient.tmdbApi.getImages(
                type = mediaType,
                id = tmdbId,
                apiKey = getEffectiveTmdbApiKey(),
                includeImageLanguage = "$languageCode,en,null"
            ).logos.orEmpty().filter { !it.filePath.isNullOrBlank() }

            logos.firstOrNull { it.language == languageCode }?.filePath
                ?: logos.firstOrNull { it.language == null }?.filePath
                ?: logos.maxByOrNull { (it.voteAverage ?: 0.0) * (it.voteCount ?: 0) }?.filePath
        } catch (e: Exception) {
            null
        }
        path?.let { logoCache[cacheKey] = it }
        return path
    }

    suspend fun getMediaDetailsWithRatings(tmdbId: Int, mediaType: String, initialItem: MediaItem?): MediaItem {
        val idKey = "${mediaType}_$tmdbId"
        val cachedMetadata = mediaMetadataDao?.getMediaMetadata(idKey)
        val now = System.currentTimeMillis()

        val isExpired = if (cachedMetadata != null) {
            if (cachedMetadata.isCacheForever) {
                false // Details for ended series or released movie cached forever
            } else {
                (now - cachedMetadata.lastFetchedAt) > TTL_METADATA_MS
            }
        } else true

        if (!isExpired && cachedMetadata != null) {
            val userRegion = getUserRegion()
            val rawRating = cachedMetadata.contentRating ?: ""
            val cleanRating = if (userRegion.equals("BR", ignoreCase = true)) {
                mapToBrazilRating(rawRating) ?: rawRating
            } else rawRating

            val ratings = RatingsCluster(
                tmdbScore = String.format("%.1f", cachedMetadata.voteAverage),
                netflixMatch = "${((cachedMetadata.voteAverage) * 10).toInt().coerceIn(65, 99)}%"
            )
            return MediaItem(
                id = cachedMetadata.tmdbId,
                mediaType = cachedMetadata.mediaType,
                title = cachedMetadata.title,
                overview = cachedMetadata.overview,
                posterPath = cachedMetadata.posterPath,
                backdropPath = cachedMetadata.backdropPath,
                releaseYear = cachedMetadata.releaseYear,
                voteAverage = cachedMetadata.voteAverage,
                imdbId = cachedMetadata.imdbId,
                contentRating = cleanRating,
                ratingsCluster = ratings,
                matchPercentage = ((cachedMetadata.voteAverage) * 10).toInt().coerceIn(65, 99),
                logoPath = fetchLogoPath(tmdbId, mediaType)
            )
        }

        val base = initialItem ?: MediaItem(
            id = tmdbId,
            mediaType = mediaType,
            title = "Loading...",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseYear = "",
            voteAverage = 8.0
        )

        val imdbId = getImdbId(tmdbId, mediaType) ?: base.imdbId
        val dynamicRating = fetchContentRating(tmdbId, mediaType)
        var ratingsCluster = base.ratingsCluster

        val omdbKey = settingsDataStore.omdbApiKey.first()
        val omdbEnabled = omdbKey.isNotBlank()
        val tmdbApiKey = getEffectiveTmdbApiKey()
        val language = settingsDataStore.tmdbLanguage.first()

        var mediaStatus: String? = null
        var isEndedOrReleased = false
        var localizedTitle: String? = null
        var localizedOverview: String? = null

        if (mediaType == "tv") {
            try {
                val tvDetails = ApiClient.tmdbApi.getTvDetails(id = tmdbId, apiKey = tmdbApiKey, language = language)
                localizedTitle = tvDetails.name
                localizedOverview = tvDetails.overview
                mediaStatus = tvDetails.status
                if (mediaStatus.equals("Ended", ignoreCase = true) || mediaStatus.equals(
                        "Canceled",
                        ignoreCase = true
                    )
                ) {
                    isEndedOrReleased = true // Ended TV Series cached forever
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (mediaType == "movie") {
            try {
                val movieDetails =
                    ApiClient.tmdbApi.getMovieDetails(id = tmdbId, apiKey = tmdbApiKey, language = language)
                localizedTitle = movieDetails.title
                localizedOverview = movieDetails.overview
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Only cache forever once the title is actually out; upcoming films
            // change runtime, ratings and release status.
            val releaseYear = base.releaseYear.toIntOrNull()
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            isEndedOrReleased = releaseYear != null && releaseYear < currentYear
        }

        if (omdbEnabled && omdbKey.isNotBlank() && !imdbId.isNullOrBlank()) {
            try {
                val omdbRes = ApiClient.omdbApi.getRatingsByImdbId(imdbId = imdbId, apiKey = omdbKey)
                var rtScore: String? = null
                var metaScore: String? = null

                omdbRes.ratings?.forEach { rating ->
                    when (rating.source) {
                        "Rotten Tomatoes" -> rtScore = rating.value
                        "Metacritic" -> metaScore = rating.value
                    }
                }

                ratingsCluster = RatingsCluster(
                    imdbScore = omdbRes.imdbRating ?: ratingsCluster.imdbScore,
                    tmdbScore = String.format("%.1f", base.voteAverage),
                    rottenTomatoesScore = rtScore ?: ratingsCluster.rottenTomatoesScore,
                    metacriticScore = omdbRes.metascore ?: metaScore ?: ratingsCluster.metacriticScore,
                    netflixMatch = "${base.matchPercentage}%"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resultItem = base.copy(
            title = localizedTitle?.takeIf { it.isNotBlank() } ?: base.title,
            overview = localizedOverview?.takeIf { it.isNotBlank() } ?: base.overview,
            imdbId = imdbId,
            contentRating = dynamicRating ?: base.contentRating,
            ratingsCluster = ratingsCluster,
            logoPath = fetchLogoPath(tmdbId, mediaType)
        )

        // Cache in Room DB
        mediaMetadataDao?.saveMediaMetadata(
            MediaMetadataEntity(
                idKey = idKey,
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = resultItem.title,
                overview = resultItem.overview,
                posterPath = resultItem.posterPath,
                backdropPath = resultItem.backdropPath,
                releaseYear = resultItem.releaseYear,
                voteAverage = resultItem.voteAverage,
                imdbId = imdbId,
                contentRating = resultItem.contentRating,
                status = mediaStatus,
                isCacheForever = isEndedOrReleased,
                lastFetchedAt = now
            )
        )

        return resultItem
    }

    suspend fun getTvSeasons(tvShowId: Int): List<SeasonModel> {
        return try {
            val tmdbApiKey = getEffectiveTmdbApiKey()
            val language = settingsDataStore.tmdbLanguage.first()
            val res = ApiClient.tmdbApi.getTvDetails(id = tvShowId, apiKey = tmdbApiKey, language = language)
            res.seasons?.map {
                SeasonModel(
                    seasonNumber = it.seasonNumber,
                    name = it.name,
                    episodeCount = it.episodeCount,
                    posterPath = it.posterPath
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeasonEpisodes(tvShowId: Int, seasonNumber: Int): List<EpisodeModel> {
        return try {
            val tmdbApiKey = getEffectiveTmdbApiKey()
            val language = settingsDataStore.tmdbLanguage.first()
            val res = ApiClient.tmdbApi.getSeasonDetails(
                id = tvShowId,
                seasonNumber = seasonNumber,
                apiKey = tmdbApiKey,
                language = language
            )
            res.episodes?.map {
                EpisodeModel(
                    id = it.id,
                    seasonNumber = it.seasonNumber,
                    episodeNumber = it.episodeNumber,
                    title = it.name,
                    overview = it.overview,
                    stillPath = it.stillPath,
                    durationMinutes = it.runtime ?: 45,
                    airDate = it.airDate
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestWatchProgress(tmdbId: Int, mediaType: String): WatchProgressEntity? {
        return watchProgressDao?.getLatestWatchProgressForMedia(tmdbId, mediaType)
    }

    fun getWatchProgressForShow(tmdbId: Int): Flow<List<WatchProgressEntity>> {
        return watchProgressDao?.getWatchProgressForShow(tmdbId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun getSmartNextEpisode(tmdbId: Int, completionThresholdPct: Int = 90): Pair<Int, Int> {
        val progress = watchProgressDao?.getLatestWatchProgressForMedia(tmdbId, "tv")
        val seasons = getTvSeasons(tmdbId)
        val regularSeasons = seasons.filter { it.seasonNumber > 0 }
        val defaultSeasonNum = regularSeasons.firstOrNull()?.seasonNumber ?: seasons.firstOrNull()?.seasonNumber ?: 1

        if (progress == null) {
            return Pair(defaultSeasonNum, 1)
        }

        val currentPct = progress.progressPercentage * 100f
        var currentSeason = progress.seasonNumber ?: defaultSeasonNum
        if (currentSeason == 0 && regularSeasons.isNotEmpty()) {
            currentSeason = defaultSeasonNum
        }
        val currentEpisode = progress.episodeNumber ?: 1

        if (currentPct < completionThresholdPct) {
            return Pair(currentSeason, currentEpisode)
        }

        val seasonModel = seasons.find { it.seasonNumber == currentSeason } ?: regularSeasons.firstOrNull()

        if (seasonModel != null && currentEpisode < seasonModel.episodeCount) {
            return Pair(currentSeason, currentEpisode + 1)
        }

        val nextSeasonModel = seasons.find { it.seasonNumber == currentSeason + 1 }
        if (nextSeasonModel != null) {
            return Pair(currentSeason + 1, 1)
        }

        return Pair(currentSeason, currentEpisode)
    }

    suspend fun saveWatchProgress(
        tmdbId: Int,
        mediaType: String,
        title: String,
        posterPath: String?,
        backdropPath: String?,
        positionMs: Long,
        durationMs: Long,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        episodeTitle: String? = null
    ) {
        val idKey = if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
            "tv_${tmdbId}_s${seasonNumber}_e${episodeNumber}"
        } else {
            "${mediaType}_${tmdbId}"
        }

        val now = System.currentTimeMillis()

        watchHistoryDao.upsertWatchHistory(
            WatchHistoryEntity(
                id = idKey,
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                positionMs = positionMs,
                durationMs = durationMs,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                lastWatchedAt = now
            )
        )

        watchProgressDao?.upsertWatchProgress(
            WatchProgressEntity(
                id = idKey,
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                positionMs = positionMs,
                durationMs = durationMs,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                lastWatchedAt = now,
                lastFetchedAt = now
            )
        )
    }

    suspend fun getSavedPlaybackPosition(
        tmdbId: Int,
        mediaType: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        completionThreshold: Int = 90
    ): Long {
        val positionMs: Long
        val durationMs: Long

        if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
            val key = "tv_${tmdbId}_s${seasonNumber}_e${episodeNumber}"
            val progressEntity = watchProgressDao?.getWatchProgressById(key)
            if (progressEntity != null) {
                positionMs = progressEntity.positionMs
                durationMs = progressEntity.durationMs
            } else {
                val historyEntity = watchHistoryDao.getWatchHistoryById(key)
                positionMs = historyEntity?.positionMs ?: 0L
                durationMs = historyEntity?.durationMs ?: 0L
            }
        } else {
            val progressEntity = watchProgressDao?.getLatestWatchProgressForMedia(tmdbId, mediaType)
            if (progressEntity != null) {
                positionMs = progressEntity.positionMs
                durationMs = progressEntity.durationMs
            } else {
                val historyEntity = watchHistoryDao.getLatestForMedia(tmdbId)
                positionMs = historyEntity?.positionMs ?: 0L
                durationMs = historyEntity?.durationMs ?: 0L
            }
        }

        if (durationMs > 0L && positionMs > 0L) {
            val pct = (positionMs.toFloat() / durationMs.toFloat()) * 100f
            if (pct < completionThreshold.toFloat()) {
                return positionMs
            }
        }
        return 0L
    }

    fun isWatchlisted(idKey: String): Flow<Boolean> = watchlistDao.isInWatchlistFlow(idKey)

    fun getWatchlistItems(): Flow<List<MediaItem>> {
        return watchlistDao.getWatchlist().map { list ->
            list.map { entity ->
                val meta = mediaMetadataDao?.getMediaMetadata("${entity.mediaType}_${entity.tmdbId}")
                val userRegion = getUserRegion()
                val rawRating = meta?.contentRating ?: ""
                val cleanRating = if (userRegion.equals("BR", ignoreCase = true)) {
                    mapToBrazilRating(rawRating) ?: rawRating
                } else rawRating

                MediaItem(
                    id = entity.tmdbId,
                    mediaType = entity.mediaType,
                    title = meta?.title ?: entity.title,
                    overview = meta?.overview ?: "",
                    posterPath = entity.posterPath,
                    backdropPath = entity.backdropPath,
                    releaseYear = entity.releaseDate ?: "",
                    voteAverage = entity.voteAverage ?: 8.0,
                    contentRating = cleanRating,
                    matchPercentage = ((entity.voteAverage ?: 8.0) * 10).toInt().coerceIn(65, 99)
                )
            }
        }
    }

    suspend fun toggleWatchlist(item: MediaItem, traktRepository: TraktRepository? = null) {
        val idKey = "${item.mediaType}_${item.id}"
        val isCurrentlyInWatchlist = watchlistDao.isInWatchlist(idKey)
        if (isCurrentlyInWatchlist) {
            watchlistDao.removeFromWatchlist(idKey)
            traktRepository?.syncWatchlistToTrakt(item, isAdd = false)
        } else {
            watchlistDao.addToWatchlist(
                WatchlistEntity(
                    id = idKey,
                    tmdbId = item.id,
                    mediaType = item.mediaType,
                    title = item.title,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    releaseDate = item.releaseYear
                )
            )
            traktRepository?.syncWatchlistToTrakt(item, isAdd = true)
        }
    }

    private fun TmdbMediaDto.toMediaItem(fallbackType: String = "movie"): MediaItem {
        // Trending/search endpoints also return `person` entries, which have no
        // poster or overview of their own; they must never be rendered as titles.
        val type = when (mediaType) {
            "movie", "tv" -> mediaType
            else -> fallbackType
        }
        val nameStr = title ?: name ?: "Untitled"
        val dateStr = releaseDate ?: firstAirDate ?: ""
        val year = if (dateStr.length >= 4) dateStr.substring(0, 4) else ""
        val avg = voteAverage ?: 7.5

        return MediaItem(
            id = id,
            mediaType = type,
            title = nameStr,
            overview = overview ?: "",
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseYear = year,
            voteAverage = avg,
            matchPercentage = ((avg * 10).toInt().coerceIn(65, 99))
        )
    }
}

