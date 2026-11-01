package com.flick.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * FlickList (https://flicklist.tv/dev) is a Trakt-shaped tracking backend.
 * Its API is additive-only, and its device flow answers HTTP 200 for every
 * state with the state carried in the body, so every field here is nullable
 * with a default: a renamed or missing key degrades to "unknown" instead of
 * failing the whole parse. Always check the payload, never the status code.
 */

@JsonClass(generateAdapter = true)
data class FlickListDeviceCodeRequest(
    @Json(name = "client_id") val clientId: String
)

@JsonClass(generateAdapter = true)
data class FlickListDeviceCodeResponse(
    @Json(name = "device_code") val deviceCode: String? = null,
    @Json(name = "user_code") val userCode: String? = null,
    // FlickList spells this "verification_uri"; Trakt spells it "verification_url".
    @Json(name = "verification_uri") val verificationUri: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null,
    @Json(name = "interval") val interval: Int? = null
)

/** The token request carries only the device code; there is no client id/secret here. */
@JsonClass(generateAdapter = true)
data class FlickListDeviceTokenRequest(
    @Json(name = "device_code") val deviceCode: String
)

/**
 * Success and failure share one body: `access_token`/`token` are set on success,
 * and `error` is `authorization_pending`, `access_denied` or `expired_token`
 * otherwise. `expires_at` is null when the credential is a permanent API key.
 */
@JsonClass(generateAdapter = true)
data class FlickListTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "user") val user: FlickListUser? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class FlickListUser(
    @Json(name = "id") val id: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

/**
 * Cross-source identity block. `fldb` (always present) needs no `media_type`;
 * every other key requires one, where "tv" is accepted as a synonym of "show".
 */
@JsonClass(generateAdapter = true)
data class FlickListIds(
    @Json(name = "fldb") val fldb: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "tmdb") val tmdb: Int? = null,
    @Json(name = "imdb") val imdb: String? = null,
    @Json(name = "tvdb") val tvdb: Int? = null,
    @Json(name = "anilist") val anilist: Int? = null
)

@JsonClass(generateAdapter = true)
data class FlickListSyncItem(
    @Json(name = "ids") val ids: FlickListIds,
    @Json(name = "media_type") val mediaType: String? = null
)

@JsonClass(generateAdapter = true)
data class FlickListSyncContainer(
    @Json(name = "items") val items: List<FlickListSyncItem>
)

/** Items FlickList could not resolve land in `not_found` rather than failing the call. */
@JsonClass(generateAdapter = true)
data class FlickListSyncResult(
    @Json(name = "added") val added: Int? = null,
    @Json(name = "removed") val removed: Int? = null,
    @Json(name = "not_found") val notFound: List<FlickListSyncItem>? = null
)

@JsonClass(generateAdapter = true)
data class FlickListWatchlistItem(
    @Json(name = "status") val status: String? = null,
    @Json(name = "progress") val progress: Float? = null,
    @Json(name = "added_at") val addedAt: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    // Null when the row is not linked to any external id.
    @Json(name = "ids") val ids: FlickListIds? = null
)

@JsonClass(generateAdapter = true)
data class FlickListPlaybackItem(
    // "episode" for show rows, "movie" otherwise.
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "season_number") val seasonNumber: Int? = null,
    @Json(name = "episode_number") val episodeNumber: Int? = null,
    @Json(name = "episode_name") val episodeName: String? = null,
    @Json(name = "progress") val progress: Float? = null,
    @Json(name = "paused") val paused: Boolean? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "ids") val ids: FlickListIds? = null
)

@JsonClass(generateAdapter = true)
data class FlickListScrobbleRequest(
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "ids") val ids: FlickListIds,
    @Json(name = "progress") val progress: Float,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "episode") val episode: Int? = null
)
