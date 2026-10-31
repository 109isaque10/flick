package com.flick.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TraktDeviceCodeResponse(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_url") val verificationUrl: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "interval") val interval: Int
)

@JsonClass(generateAdapter = true)
data class TraktDeviceCodeRequest(
    @Json(name = "client_id") val clientId: String
)

@JsonClass(generateAdapter = true)
data class TraktDeviceTokenRequest(
    @Json(name = "code") val code: String,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "client_secret") val clientSecret: String
)

@JsonClass(generateAdapter = true)
data class TraktEpisode(
    @Json(name = "season") val season: Int,
    @Json(name = "number") val number: Int
)

@JsonClass(generateAdapter = true)
data class TraktTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "scope") val scope: String,
    @Json(name = "created_at") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class TraktUser(
    @Json(name = "username") val username: String,
    @Json(name = "name") val name: String?,
    @Json(name = "vip") val vip: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class TraktIds(
    @Json(name = "tmdb") val tmdb: Int? = null,
    @Json(name = "imdb") val imdb: String? = null
)

@JsonClass(generateAdapter = true)
data class TraktMediaItem(
    @Json(name = "ids") val ids: TraktIds,
    @Json(name = "title") val title: String? = null,
    @Json(name = "year") val year: Int? = null
)

@JsonClass(generateAdapter = true)
data class TraktSyncContainer(
    @Json(name = "movies") val movies: List<TraktMediaItem>? = null,
    @Json(name = "shows") val shows: List<TraktMediaItem>? = null
)

@JsonClass(generateAdapter = true)
data class TraktWatchlistItem(
    @Json(name = "type") val type: String? = null,
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "show") val show: TraktMediaItem? = null
)

@JsonClass(generateAdapter = true)
data class TraktPlaybackItem(
    @Json(name = "progress") val progress: Float? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "show") val show: TraktMediaItem? = null,
    @Json(name = "episode") val episode: TraktEpisode? = null
)

@JsonClass(generateAdapter = true)
data class TraktScrobbleRequest(
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "show") val show: TraktMediaItem? = null,
    @Json(name = "episode") val episode: TraktEpisode? = null,
    @Json(name = "progress") val progress: Float
)

@JsonClass(generateAdapter = true)
data class OmdbResponse(
    @Json(name = "Title") val title: String? = null,
    @Json(name = "Year") val year: String? = null,
    @Json(name = "imdbRating") val imdbRating: String? = null,
    @Json(name = "Metascore") val metascore: String? = null,
    @Json(name = "Ratings") val ratings: List<OmdbRating>? = emptyList(),
    @Json(name = "Response") val response: String? = "True"
)

@JsonClass(generateAdapter = true)
data class OmdbRating(
    @Json(name = "Source") val source: String,
    @Json(name = "Value") val value: String
)
