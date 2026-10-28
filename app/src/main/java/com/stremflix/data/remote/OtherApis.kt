package com.stremflix.data.remote

import com.stremflix.data.model.OmdbResponse
import com.stremflix.data.model.StremioStreamResponse
import com.stremflix.data.model.TraktDeviceCodeRequest
import com.stremflix.data.model.TraktDeviceCodeResponse
import com.stremflix.data.model.TraktDeviceTokenRequest
import com.stremflix.data.model.TraktTokenResponse
import com.stremflix.data.model.TraktUser
import retrofit2.http.Body
import retrofit2.http.GET
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
    ): List<com.stremflix.data.model.TraktWatchlistItem>

    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.stremflix.data.model.TraktSyncContainer
    )

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.stremflix.data.model.TraktSyncContainer
    )

    @GET("sync/playback")
    suspend fun getPlayback(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String
    ): List<com.stremflix.data.model.TraktPlaybackItem>

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: com.stremflix.data.model.TraktScrobbleRequest
    )
}
