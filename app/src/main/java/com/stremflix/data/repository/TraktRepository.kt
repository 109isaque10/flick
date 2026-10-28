package com.stremflix.data.repository

import com.stremflix.data.local.SettingsDataStore
import com.stremflix.data.local.WatchProgressDao
import com.stremflix.data.local.WatchProgressEntity
import com.stremflix.data.local.WatchlistDao
import com.stremflix.data.local.WatchlistEntity
import com.stremflix.data.model.MediaItem
import com.stremflix.data.model.TraktDeviceCodeRequest
import com.stremflix.data.model.TraktDeviceCodeResponse
import com.stremflix.data.model.TraktDeviceTokenRequest
import com.stremflix.data.model.TraktEpisode
import com.stremflix.data.model.TraktIds
import com.stremflix.data.model.TraktMediaItem
import com.stremflix.data.model.TraktScrobbleRequest
import com.stremflix.data.model.TraktSyncContainer
import com.stremflix.data.model.TraktUser
import com.stremflix.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TraktRepository(
    private val settingsDataStore: SettingsDataStore,
    private val watchlistDao: WatchlistDao? = null,
    private val watchProgressDao: WatchProgressDao? = null
) {
    // Trakt app credentials are user-supplied in Settings; there are no usable built-in values.
    private suspend fun clientId(): String =
        settingsDataStore.traktApiKey.first().trim()

    private suspend fun clientSecret(): String =
        settingsDataStore.traktClientSecret.first().trim()

    /** True when both credentials required by the device flow are present. */
    suspend fun isConfigured(): Boolean =
        clientId().isNotBlank() && clientSecret().isNotBlank()

    val traktUsername: Flow<String?> = settingsDataStore.traktUsername
    val traktAccessToken: Flow<String?> = settingsDataStore.traktAccessToken

    // Device-flow polling must outlive the composition that started it.
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    suspend fun requestDeviceCode(): TraktDeviceCodeResponse? {
        val id = clientId()
        if (id.isBlank()) return null
        return try {
            ApiClient.traktApi.getDeviceCode(TraktDeviceCodeRequest(clientId = id))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Starts the device-flow polling in a repository-owned scope so navigation or
     * recomposition cannot cancel a login that is already in progress.
     */
    fun startDeviceAuthPolling(
        deviceCode: String,
        intervalSeconds: Int = 5,
        timeoutSeconds: Int = 600
    ) {
        if (pollingJob?.isActive == true) return
        pollingJob = authScope.launch {
            pollForToken(deviceCode, intervalSeconds, timeoutSeconds)
        }
    }

    /**
     * Trakt answers HTTP 400 (`authorization_pending`) until the user approves the
     * code, so that status is a retry rather than a failure. Every other status is
     * terminal (410 expired, 418 denied, 409 already used, 404 bad code).
     */
    suspend fun pollForToken(
        deviceCode: String,
        intervalSeconds: Int = 5,
        timeoutSeconds: Int = 600
    ): Boolean {
        val id = clientId()
        val secret = clientSecret()
        if (id.isBlank() || secret.isBlank()) return false

        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        val interval = intervalSeconds.coerceAtLeast(1) * 1000L

        while (System.currentTimeMillis() < deadline) {
            try {
                val tokenResponse = ApiClient.traktApi.getToken(
                    TraktDeviceTokenRequest(
                        code = deviceCode,
                        clientId = id,
                        clientSecret = secret
                    )
                )
                val username = fetchUserProfile(tokenResponse.accessToken)?.username
                    ?: "Trakt User"

                settingsDataStore.saveTraktAuth(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    username = username
                )
                syncWatchlist()
                syncPlayback()
                return true
            } catch (e: HttpException) {
                if (e.code() != 400) {
                    e.printStackTrace()
                    return false
                }
                // authorization_pending - keep waiting for the user.
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            delay(interval)
        }
        return false
    }

    suspend fun fetchUserProfile(accessToken: String): TraktUser? {
        return try {
            ApiClient.traktApi.getUserProfile(
                authorization = "Bearer $accessToken",
                clientId = clientId()
            )
        } catch (e: Exception) {
            null
        }
    }

    // Pull Trakt Watchlist into local Room database
    suspend fun syncWatchlist() {
        val token = traktAccessToken.firstOrNull()
        if (token.isNullOrBlank() || watchlistDao == null) return

        try {
            val items = ApiClient.traktApi.getWatchlist(
                authorization = "Bearer $token",
                clientId = clientId()
            )

            val now = System.currentTimeMillis()
            for (item in items) {
                val traktItem = item.show ?: item.movie ?: continue
                val tmdbId = traktItem.ids.tmdb ?: continue
                // Trakt says "show"; the rest of the app uses "tv".
                val type = if (item.show != null) "tv" else "movie"
                val id = "${type}_$tmdbId"

                val entity = WatchlistEntity(
                    id = id,
                    tmdbId = tmdbId,
                    mediaType = type,
                    title = traktItem.title ?: "Untitled",
                    posterPath = null,
                    backdropPath = null,
                    voteAverage = 8.0,
                    releaseDate = traktItem.year?.toString(),
                    addedAt = now
                )
                watchlistDao.addToWatchlist(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sync local MyList addition/removal to Trakt
    suspend fun syncWatchlistToTrakt(item: MediaItem, isAdd: Boolean) {
        val token = traktAccessToken.firstOrNull()
        if (token.isNullOrBlank()) return

        try {
            val mediaItem = TraktMediaItem(
                ids = TraktIds(tmdb = item.id),
                title = item.title,
                year = null
            )

            val payload = if (item.mediaType == "tv") {
                TraktSyncContainer(shows = listOf(mediaItem))
            } else {
                TraktSyncContainer(movies = listOf(mediaItem))
            }

            val id = clientId()
            if (isAdd) {
                ApiClient.traktApi.addToWatchlist("Bearer $token", id, payload)
            } else {
                ApiClient.traktApi.removeFromWatchlist("Bearer $token", id, payload)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Pulls Trakt playback progress into Room. Trakt only reports a percentage, not a
     * runtime, so no absolute resume position is invented here: the row is stored with
     * zero position and duration (percent unknown) instead of seeking users to a
     * fabricated timestamp.
     */
    suspend fun syncPlayback() {
        val token = traktAccessToken.firstOrNull()
        if (token.isNullOrBlank() || watchProgressDao == null) return

        try {
            val playbackList = ApiClient.traktApi.getPlayback(
                authorization = "Bearer $token",
                clientId = clientId()
            )

            val now = System.currentTimeMillis()
            for (p in playbackList) {
                val isShow = p.show != null || p.type == "episode"
                val traktItem = if (isShow) p.show else p.movie
                val tmdbId = traktItem?.ids?.tmdb ?: continue
                val type = if (isShow) "tv" else "movie"

                val season = p.episode?.season
                val episodeNumber = p.episode?.number
                // Must match MediaRepository's key or the row is unreachable.
                val id = if (isShow && season != null && episodeNumber != null) {
                    "tv_${tmdbId}_s${season}_e${episodeNumber}"
                } else {
                    "${type}_$tmdbId"
                }

                val entity = WatchProgressEntity(
                    id = id,
                    tmdbId = tmdbId,
                    mediaType = type,
                    title = traktItem.title ?: "Untitled",
                    posterPath = null,
                    backdropPath = null,
                    positionMs = 0L,
                    durationMs = 0L,
                    seasonNumber = if (isShow) season else null,
                    episodeNumber = if (isShow) episodeNumber else null,
                    episodeTitle = null,
                    lastWatchedAt = now,
                    lastFetchedAt = now
                )
                watchProgressDao.upsertWatchProgress(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Scrobble watch progress to Trakt
    suspend fun scrobbleProgressToTrakt(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int? = null,
        episode: Int? = null
    ) {
        val token = traktAccessToken.firstOrNull()
        if (token.isNullOrBlank()) return

        try {
            val req = if (mediaType == "tv") {
                // An episode scrobble needs the parent show plus season/number;
                // sending the show ids as the episode is rejected by Trakt.
                if (season == null || episode == null) return
                TraktScrobbleRequest(
                    show = TraktMediaItem(ids = TraktIds(tmdb = tmdbId)),
                    episode = TraktEpisode(season = season, number = episode),
                    progress = progressPct
                )
            } else {
                TraktScrobbleRequest(
                    movie = TraktMediaItem(ids = TraktIds(tmdb = tmdbId)),
                    progress = progressPct
                )
            }
            ApiClient.traktApi.scrobblePause(
                "Bearer $token",
                clientId(),
                req
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun disconnectTrakt() {
        settingsDataStore.clearTraktAuth()
    }
}
