package com.flick.app.data.remote

import com.flick.app.data.model.FlickListDeviceCodeRequest
import com.flick.app.data.model.FlickListDeviceCodeResponse
import com.flick.app.data.model.FlickListDeviceTokenRequest
import com.flick.app.data.model.FlickListPlaybackItem
import com.flick.app.data.model.FlickListScrobbleRequest
import com.flick.app.data.model.FlickListSyncContainer
import com.flick.app.data.model.FlickListSyncResult
import com.flick.app.data.model.FlickListTokenResponse
import com.flick.app.data.model.FlickListUser
import com.flick.app.data.model.FlickListWatchlistItem
import com.flick.app.data.model.OmdbResponse
import com.flick.app.data.model.StremioStreamResponse
import com.flick.app.data.model.TraktDeviceCodeRequest
import com.flick.app.data.model.TraktDeviceCodeResponse
import com.flick.app.data.model.TraktDeviceTokenRequest
import com.flick.app.data.model.TraktTokenResponse
import com.flick.app.data.model.TraktUser
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OmdbApi {
    @GET("/")
    suspend fun getRatingsByImdbId(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String
    ): OmdbResponse
}

interface StremioApi {
    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioStreamResponse
}

interface TraktApi {
    @POST("oauth/device/code")
    suspend fun getDeviceCode(
        @Body body: TraktDeviceCodeRequest
    ): TraktDeviceCodeResponse

    @POST("oauth/device/token")
    suspend fun getToken(
        @Body body: TraktDeviceTokenRequest
    ): TraktTokenResponse

    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String
    ): TraktUser

    @GET("sync/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String
    ): List<com.flick.app.data.model.TraktWatchlistItem>

    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.flick.app.data.model.TraktSyncContainer
    )

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.flick.app.data.model.TraktSyncContainer
    )

    @GET("sync/playback")
    suspend fun getPlayback(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String
    ): List<com.flick.app.data.model.TraktPlaybackItem>

    @POST("scrobble/start")
    suspend fun scrobbleStart(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.flick.app.data.model.TraktScrobbleRequest
    )

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.flick.app.data.model.TraktScrobbleRequest
    )

    @POST("scrobble/stop")
    suspend fun scrobbleStop(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.flick.app.data.model.TraktScrobbleRequest
    )
}

/**
 * FlickList tracking backend. Paths are relative to `https://flicklist.tv/api/`,
 * which is why the auth and `/v3` routes share one service.
 */
interface FlickListApi {
    @POST("auth/device/code")
    suspend fun getDeviceCode(
        @Body body: FlickListDeviceCodeRequest
    ): FlickListDeviceCodeResponse

    @POST("auth/device/token")
    suspend fun getToken(
        @Body body: FlickListDeviceTokenRequest
    ): FlickListTokenResponse

    @GET("v3/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String
    ): FlickListUser

    @GET("v3/sync/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") authorization: String
    ): List<FlickListWatchlistItem>

    @POST("v3/sync/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") authorization: String,
        @Body body: FlickListSyncContainer
    ): FlickListSyncResult

    // FlickList removes watchlist entries with a DELETE that still carries a body.
    @HTTP(method = "DELETE", path = "v3/sync/watchlist", hasBody = true)
    suspend fun removeFromWatchlist(
        @Header("Authorization") authorization: String,
        @Body body: FlickListSyncContainer
    ): FlickListSyncResult

    @GET("v3/sync/playback")
    suspend fun getPlayback(
        @Header("Authorization") authorization: String
    ): List<FlickListPlaybackItem>

    @POST("v3/scrobble/start")
    suspend fun scrobbleStart(
        @Header("Authorization") authorization: String,
        @Body body: FlickListScrobbleRequest
    )

    @POST("v3/scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") authorization: String,
        @Body body: FlickListScrobbleRequest
    )

    @POST("v3/scrobble/stop")
    suspend fun scrobbleStop(
        @Header("Authorization") authorization: String,
        @Body body: FlickListScrobbleRequest
    )
}
