package com.stremflix.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TmdbPageResponse<T>(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<T>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class TmdbMediaDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "popularity") val popularity: Double? = 0.0,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDatesResponse(
    @Json(name = "results") val results: List<TmdbReleaseDateCountryDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateCountryDto(
    @Json(name = "iso_3166_1") val isoCode: String,
    @Json(name = "release_dates") val releaseDates: List<TmdbReleaseDateItemDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateItemDto(
    @Json(name = "certification") val certification: String? = ""
)

@JsonClass(generateAdapter = true)
data class TmdbTvContentRatingsResponse(
    @Json(name = "results") val results: List<TmdbTvContentRatingItemDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbTvContentRatingItemDto(
    @Json(name = "iso_3166_1") val isoCode: String,
    @Json(name = "rating") val rating: String? = ""
)

@JsonClass(generateAdapter = true)
data class TmdbExternalIdsDto(
    @Json(name = "id") val id: Int,
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "tvdb_id") val tvdbId: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "tagline") val tagline: String? = null,
    @Json(name = "runtime") val runtime: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int = 1,
    @Json(name = "seasons") val seasons: List<TmdbSeasonDto>? = emptyList()
)


@JsonClass(generateAdapter = true)
data class TmdbSeasonDto(
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "episode_count") val episodeCount: Int,
    @Json(name = "poster_path") val posterPath: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonDetailDto(
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "episodes") val episodes: List<TmdbEpisodeDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbEpisodeDto(
    @Json(name = "id") val id: Int,
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "runtime") val runtime: Int? = 45,
    @Json(name = "air_date") val airDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbImagesResponse(
    @Json(name = "logos") val logos: List<TmdbImageDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbImageDto(
    @Json(name = "file_path") val filePath: String? = null,
    @Json(name = "iso_639_1") val language: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "vote_count") val voteCount: Int? = 0,
    @Json(name = "width") val width: Int? = 0,
    @Json(name = "height") val height: Int? = 0
)

@JsonClass(generateAdapter = true)
data class TmdbGenreListDto(
    @Json(name = "genres") val genres: List<TmdbGenreDto>
)

@JsonClass(generateAdapter = true)
data class TmdbGenreDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

interface TmdbApi {
    @GET("trending/{type}/{time_window}")
    suspend fun getTrending(
        @Path("type") type: String = "all",
        @Path("time_window") timeWindow: String = "day",
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("region") region: String? = null
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("{type}/popular")
    suspend fun getPopular(
        @Path("type") type: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("region") region: String? = null,
        @Query("page") page: Int = 1
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("region") region: String? = null,
        @Query("page") page: Int = 1
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("discover/{type}")
    suspend fun discoverByGenre(
        @Path("type") type: String,
        @Query("with_genres") genreId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("region") region: String? = null,
        @Query("page") page: Int = 1
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("{type}/{id}/recommendations")
    suspend fun getRecommendations(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("{type}/{id}/similar")
    suspend fun getSimilar(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbPageResponse<TmdbMediaDto>

    @GET("{type}/{id}/images")
    suspend fun getImages(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TmdbImagesResponse

    @GET("{type}/{id}/external_ids")
    suspend fun getExternalIds(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbExternalIdsDto

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetailDto

    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbTvDetailDto

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbSeasonDetailDto

    @GET("genre/{type}/list")
    suspend fun getGenres(
        @Path("type") type: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbGenreListDto

    @GET("movie/{id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbReleaseDatesResponse

    @GET("tv/{id}/content_ratings")
    suspend fun getTvContentRatings(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): TmdbTvContentRatingsResponse
}
