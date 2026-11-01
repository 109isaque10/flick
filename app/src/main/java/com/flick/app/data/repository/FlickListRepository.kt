package com.flick.app.data.repository

import com.flick.app.data.local.SettingsDataStore
import com.flick.app.data.local.WatchProgressDao
import com.flick.app.data.local.WatchProgressEntity
import com.flick.app.data.local.WatchlistDao
import com.flick.app.data.local.WatchlistEntity
import com.flick.app.data.model.FlickListDeviceCodeRequest
import com.flick.app.data.model.FlickListDeviceCodeResponse
import com.flick.app.data.model.FlickListDeviceTokenRequest
import com.flick.app.data.model.FlickListIds
import com.flick.app.data.model.FlickListScrobbleRequest
import com.flick.app.data.model.FlickListSyncContainer
import com.flick.app.data.model.FlickListSyncItem
import com.flick.app.data.model.FlickListUser
import com.flick.app.data.model.MediaItem
import com.flick.app.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FlickList tracking backend (https://flicklist.tv/dev), the alternative to
 * [TraktRepository]. It mirrors Trakt's surface exactly: watchlist, playback
 * resume points and scrobbling. Catalog reads, ratings, favourites and Up Next
 * are deliberately out of scope.
 *
 * Two credential kinds are accepted. A stored `fs_live_...` API key is
 * permanent and replaces the device-flow session entirely; otherwise the user
 * pairs via the device flow and we keep the session token.
 */
class FlickListRepository(
    private val settingsDataStore: SettingsDataStore,
    private val watchlistDao: WatchlistDao? = null,
    private val watchProgressDao: WatchProgressDao? = null
) : TrackingRepository {

    // FlickList rejects unregistered client ids, so the user supplies their own
    // registered app id in Settings; there is no usable built-in value.
    private suspend fun clientId(): String =
        settingsDataStore.flickListClientId.first().trim()

    private suspend fun apiKey(): String =
        settingsDataStore.flickListApiKey.first().trim()

    private suspend fun sessionToken(): String? =
        settingsDataStore.flickListAccessToken.first()

    override val username: Flow<String?> = settingsDataStore.flickListUsername

    /** The credential to authenticate with, whichever kind is present. */
    private suspend fun authToken(): String? {
        val key = apiKey()
        if (key.isNotBlank()) return key
        return sessionToken()?.takeIf { it.isNotBlank() }
    }

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    suspend fun requestDeviceCode(): FlickListDeviceCodeResponse? {
        val id = clientId()
        if (id.isBlank()) return null
        return try {
            ApiClient.flickListApi.getDeviceCode(FlickListDeviceCodeRequest(clientId = id))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Polls in a repository-owned scope so navigation or recomposition cannot
     * abandon a login that is already in progress.
     */
    fun startDeviceAuthPolling(
        deviceCode: String,
        intervalSeconds: Int = 5,
        timeoutSeconds: Int = 900
    ) {
        if (pollingJob?.isActive == true) return
        pollingJob = authScope.launch {
            pollForToken(deviceCode, intervalSeconds, timeoutSeconds)
        }
    }

    /**
     * Unlike Trakt (which answers HTTP 400 while pending), FlickList answers
     * HTTP 200 for every state and puts the state in the body's `error` field,
     * so this branches on the payload rather than the status code.
     */
    suspend fun pollForToken(
        deviceCode: String,
        intervalSeconds: Int = 5,
        timeoutSeconds: Int = 900
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        val interval = intervalSeconds.coerceAtLeast(1) * 1000L

        while (System.currentTimeMillis() < deadline) {
            val response = try {
                ApiClient.flickListApi.getToken(
                    FlickListDeviceTokenRequest(deviceCode = deviceCode)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

            val token = response.accessToken ?: response.token
            if (!token.isNullOrBlank()) {
                val username = fetchUserProfile(token)?.username
                    ?: response.user?.username
                    ?: "FlickList User"
                settingsDataStore.saveFlickListAuth(username = username, accessToken = token)
                syncWatchlist()
                syncPlayback()
                return true
            }

            when (response.error) {
                // Keep waiting for the user to approve the code. A missing error
                // with no token is an unexpected shape: treat it as pending so the
                // deadline, not a parse quirk, ends the attempt.
                null, "authorization_pending" -> Unit
                // access_denied / expired_token and anything unknown are terminal.
                else -> return false
            }
            delay(interval)
        }
        return false
    }

    suspend fun fetchUserProfile(authorization: String): FlickListUser? =
        try {
            ApiClient.flickListApi.getMe("Bearer $authorization")
        } catch (e: Exception) {
            null
        }

    /**
     * Verifies a pasted API key, records it, and pulls the account's data right
     * away so the connected state is not left waiting for the next worker run.
     */
    suspend fun connectWithApiKey(key: String) {
        val trimmed = key.trim()
        settingsDataStore.saveFlickListApiKey(trimmed)
        if (trimmed.isEmpty()) return
        fetchUserProfile(trimmed)?.username?.let { settingsDataStore.saveFlickListUsername(it) }
        syncWatchlist()
        syncPlayback()
    }

    override suspend fun syncWatchlist() {
        val token = authToken() ?: return
        if (watchlistDao == null) return

        try {
            val items = ApiClient.flickListApi.getWatchlist("Bearer $token")
            val now = System.currentTimeMillis()
            for (item in items) {
                val tmdbId = item.ids?.tmdb ?: continue
                val type = normalizeType(item.mediaType, item.ids)
                watchlistDao.addToWatchlist(
                    WatchlistEntity(
                        id = "${type}_$tmdbId",
                        tmdbId = tmdbId,
                        mediaType = type,
                        title = item.title ?: "Untitled",
                        posterPath = null,
                        backdropPath = null,
                        voteAverage = 8.0,
                        releaseDate = item.year?.toString(),
                        addedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun syncWatchlistItem(item: MediaItem, isAdd: Boolean) {
        val token = authToken() ?: return

        try {
            val payload = FlickListSyncContainer(
                items = listOf(
                    FlickListSyncItem(
                        ids = FlickListIds(tmdb = item.id),
                        mediaType = if (item.mediaType == "tv") "tv" else "movie"
                    )
                )
            )
            if (isAdd) {
                ApiClient.flickListApi.addToWatchlist("Bearer $token", payload)
            } else {
                ApiClient.flickListApi.removeFromWatchlist("Bearer $token", payload)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Pulls resume points into Room. FlickList reports a percentage, not a
     * runtime, so no absolute position is invented: the row is stored with zero
     * position and duration rather than seeking the viewer to a fabricated time.
     */
    override suspend fun syncPlayback() {
        val token = authToken() ?: return
        if (watchProgressDao == null) return

        try {
            val playbackList = ApiClient.flickListApi.getPlayback("Bearer $token")
            val now = System.currentTimeMillis()
            for (p in playbackList) {
                val tmdbId = p.ids?.tmdb ?: continue
                val isEpisode = p.mediaType == "episode" || p.seasonNumber != null
                val type = if (isEpisode) "tv" else "movie"
                val season = p.seasonNumber
                val episodeNumber = p.episodeNumber
                // Must match MediaRepository's key or the row is unreachable.
                val id = if (isEpisode && season != null && episodeNumber != null) {
                    "tv_${tmdbId}_s${season}_e${episodeNumber}"
                } else {
                    "${type}_$tmdbId"
                }

                watchProgressDao.upsertWatchProgress(
                    WatchProgressEntity(
                        id = id,
                        tmdbId = tmdbId,
                        mediaType = type,
                        title = p.title ?: "Untitled",
                        posterPath = null,
                        backdropPath = null,
                        positionMs = 0L,
                        durationMs = 0L,
                        seasonNumber = if (isEpisode) season else null,
                        episodeNumber = if (isEpisode) episodeNumber else null,
                        episodeTitle = p.episodeName,
                        lastWatchedAt = now,
                        lastFetchedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Builds the scrobble body shared by start/pause/stop. Episode rows are
     * scrobbled as media_type "episode" with the show's tmdb id plus
     * season/episode, matching how /v3/sync/playback reports them. The item must
     * resolve to a TMDB-linked row or the API 400s, so an episode scrobble with
     * no season/episode is dropped rather than sent.
     */
    private fun scrobbleBody(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int?,
        episode: Int?
    ): FlickListScrobbleRequest? = if (mediaType == "tv") {
        if (season == null || episode == null) {
            null
        } else {
            FlickListScrobbleRequest(
                mediaType = "episode",
                ids = FlickListIds(tmdb = tmdbId),
                progress = progressPct,
                season = season,
                episode = episode
            )
        }
    } else {
        FlickListScrobbleRequest(
            mediaType = "movie",
            ids = FlickListIds(tmdb = tmdbId),
            progress = progressPct
        )
    }

    // "start" is the heartbeat: re-post it every 30-60s while playback runs,
    // otherwise the backend treats the session as abandoned.
    override suspend fun scrobbleStart(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int?,
        episode: Int?
    ) {
        val token = authToken() ?: return
        val req = scrobbleBody(tmdbId, mediaType, progressPct, season, episode) ?: return

        try {
            ApiClient.flickListApi.scrobbleStart("Bearer $token", req)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun scrobbleProgress(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int?,
        episode: Int?
    ) {
        val token = authToken() ?: return
        val req = scrobbleBody(tmdbId, mediaType, progressPct, season, episode) ?: return

        try {
            ApiClient.flickListApi.scrobblePause("Bearer $token", req)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // "stop" decides the outcome: >=90% with enough runtime marks it watched,
    // below that it leaves a partial resume point.
    override suspend fun scrobbleStop(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int?,
        episode: Int?
    ) {
        val token = authToken() ?: return
        val req = scrobbleBody(tmdbId, mediaType, progressPct, season, episode) ?: return

        try {
            ApiClient.flickListApi.scrobbleStop("Bearer $token", req)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun disconnect() {
        settingsDataStore.clearFlickListAuth()
    }

    // Writes accept "tv" as a synonym of "show" and reads may return either,
    // plus "episode" on playback rows; the app keys everything on "tv". When
    // media_type is absent, fall back to the stable fldb prefix (flm_ = movie,
    // flt_ = show) rather than defaulting a show to a movie.
    private fun normalizeType(mediaType: String?, ids: FlickListIds?): String = when (mediaType) {
        "show", "tv", "episode" -> "tv"
        "movie" -> "movie"
        else -> if (ids?.fldb?.startsWith("flt_") == true) "tv" else "movie"
    }
}
