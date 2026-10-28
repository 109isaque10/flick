package com.stremflix.ui.player

/**
 * Adapter between a TMDB `/videos` YouTube key and a stream the player can open.
 *
 * ExoPlayer cannot play a YouTube watch URL, so inline trailers go through the
 * user's own YouTube backend, configured in Settings. Its
 * `GET /api/video/{id}/download` returns a fully proxied DASH manifest, which
 * Media3 plays natively — no URL rewriting needed on this side.
 */
object TrailerSource {
    /**
     * Builds the DASH manifest URL for a video key, or null when no backend is
     * configured yet (callers then fall back to the YouTube app).
     */
    fun manifestUrl(backendBaseUrl: String, videoKey: String): String? {
        val base = backendBaseUrl.trim().trimEnd('/')
        if (base.isEmpty() || videoKey.isBlank()) return null
        if (!base.startsWith("http://") && !base.startsWith("https://")) return null
        return "$base/api/video/$videoKey/download"
    }

    fun youtubeWatchUrl(videoKey: String): String = "https://www.youtube.com/watch?v=$videoKey"
}
