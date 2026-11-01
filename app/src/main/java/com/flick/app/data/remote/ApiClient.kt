package com.flick.app.data.remote

import com.flick.app.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC keeps URLs visible for debugging without dumping auth headers,
            // tokens or API keys into logcat.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()

    // Trakt rejects requests that omit the API version header.
    private val traktHttpClient = okHttpClient.newBuilder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("trakt-api-version", "2")
                    .build()
            )
        }
        .build()

    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApi::class.java)
    }

    val omdbApi: OmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OmdbApi::class.java)
    }

    val traktApi: TraktApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(traktHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraktApi::class.java)
    }

    val flickListApi: FlickListApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://flicklist.tv/api/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FlickListApi::class.java)
    }

    fun buildStremioApi(addonBaseUrl: String): StremioApi {
        val formattedUrl = if (addonBaseUrl.endsWith("/")) addonBaseUrl else "$addonBaseUrl/"
        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(StremioApi::class.java)
    }
}
