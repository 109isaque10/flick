package com.flick.app.data.repository

import com.flick.app.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * The slice of a tracking backend the rest of the app needs: the identity the
 * Settings screen shows, two-way watchlist reconciliation, resume-point
 * reconciliation, and progress scrobbling.
 *
 * Pairing is deliberately *not* here. The two backends' device flows differ in
 * return type and in how they report "still pending", so each repository keeps
 * its own pairing surface and only the shared sync behaviour is abstracted.
 */
interface TrackingRepository {
    val username: Flow<String?>

    suspend fun syncWatchlist()
    suspend fun syncPlayback()
    suspend fun syncWatchlistItem(item: MediaItem, isAdd: Boolean)

    /** Opens a playback session and is also its heartbeat; re-post while playing. */
    suspend fun scrobbleStart(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int? = null,
        episode: Int? = null
    )

    /** Reports a paused session. Never creates a resume point on its own. */
    suspend fun scrobbleProgress(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int? = null,
        episode: Int? = null
    )

    /** Closes a session; the backend decides watched, preview or partial. */
    suspend fun scrobbleStop(
        tmdbId: Int,
        mediaType: String,
        progressPct: Float,
        season: Int? = null,
        episode: Int? = null
    )

    suspend fun disconnect()
}
