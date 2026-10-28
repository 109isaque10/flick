package com.stremflix.data.repository

import com.stremflix.data.local.SettingsDataStore
import com.stremflix.data.model.StremioStream
import com.stremflix.data.remote.ApiClient
import kotlinx.coroutines.flow.first

class StremioRepository(
    private val mediaRepository: MediaRepository,
    private val settingsDataStore: SettingsDataStore
) {
    private var addonBaseUrl = "https://torrentio.strem.fun/"

    suspend fun resolveStreams(
        tmdbId: Int,
        mediaType: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): List<StremioStream> {
        val strategy = settingsDataStore.stremioIdStrategy.first()
        val customAddonUrl = settingsDataStore.stremioAddonUrl.first().ifBlank { "https://torrentio.strem.fun/" }
        val stremioType = if (mediaType == "tv") "series" else "movie"

        val imdbId = mediaRepository.getImdbId(tmdbId, mediaType)
        val targetId = if (strategy == "IMDB" && !imdbId.isNullOrBlank()) {
            if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
                "$imdbId:$seasonNumber:$episodeNumber"
            } else {
                imdbId
            }
        } else {
            if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
                "tmdb:$tmdbId:$seasonNumber:$episodeNumber"
            } else {
                "tmdb:$tmdbId"
            }
        }

        val streams = fetchStreamsForId(customAddonUrl, stremioType, targetId)
        if (streams.isNotEmpty()) {
            return streams
        }

        // Fallback: If primary target returned empty, try alternate ID if available
        if (strategy == "IMDB" && streams.isEmpty()) {
            val fallbackTmdbId = if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
                "tmdb:$tmdbId:$seasonNumber:$episodeNumber"
            } else {
                "tmdb:$tmdbId"
            }
            val tmdbStreams = fetchStreamsForId(customAddonUrl, stremioType, fallbackTmdbId)
            if (tmdbStreams.isNotEmpty()) return tmdbStreams
        } else if (strategy == "TMDB" && streams.isEmpty() && !imdbId.isNullOrBlank()) {
            val fallbackImdbId = if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
                "$imdbId:$seasonNumber:$episodeNumber"
            } else {
                imdbId
            }
            val imdbStreams = fetchStreamsForId(customAddonUrl, stremioType, fallbackImdbId)
            if (imdbStreams.isNotEmpty()) return imdbStreams
        }

        return emptyList()
    }

    private suspend fun fetchStreamsForId(baseUrl: String, type: String, id: String): List<StremioStream> {
        return try {
            val stremioApi = ApiClient.buildStremioApi(baseUrl)
            val response = stremioApi.getStreams(type = type, id = id)
            val rawStreams = response.streams ?: emptyList()

            rawStreams.filter { stream ->
                val url = stream.url
                url != null && (url.startsWith("http://") || url.startsWith("https://"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun selectBestNextStream(
        availableStreams: List<StremioStream>,
        previousStream: StremioStream?
    ): StremioStream? {
        if (availableStreams.isEmpty()) return null
        if (previousStream == null) return availableStreams.first()

        val prevBingeGroup = previousStream.behaviorHints?.bingeGroup
        if (!prevBingeGroup.isNullOrBlank()) {
            val matchedGroup = availableStreams.find { it.behaviorHints?.bingeGroup == prevBingeGroup }
            if (matchedGroup != null) return matchedGroup
        }

        val prevTitle = previousStream.title ?: ""
        if (prevTitle.contains("1080p")) {
            val matched1080p = availableStreams.find { (it.title ?: "").contains("1080p") }
            if (matched1080p != null) return matched1080p
        } else if (prevTitle.contains("4k") || prevTitle.contains("2160p")) {
            val matched4k = availableStreams.find { (it.title ?: "").contains("4k") || (it.title ?: "").contains("2160p") }
            if (matched4k != null) return matched4k
        }

        return availableStreams.first()
    }
}
