package com.flick.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flick_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val KEY_TMDB_LANGUAGE = stringPreferencesKey("tmdb_language")
        val KEY_TMDB_REGION = stringPreferencesKey("tmdb_region")
        val KEY_STREMIO_ADDON_URL = stringPreferencesKey("stremio_addon_url")
        val KEY_OMDB_API_KEY = stringPreferencesKey("omdb_api_key")
        val KEY_YOUTUBE_BACKEND_URL = stringPreferencesKey("youtube_backend_url")
        val KEY_OMDB_ENABLED = booleanPreferencesKey("omdb_enabled")
        val KEY_RATING_PROVIDERS = stringSetPreferencesKey("rating_providers")
        val KEY_STREMIO_ID_STRATEGY = stringPreferencesKey("stremio_id_strategy") // "IMDB" or "TMDB"
        val KEY_COMPLETION_THRESHOLD = intPreferencesKey("completion_threshold") // percentage e.g. 90
        val KEY_PREFETCH_THRESHOLD = intPreferencesKey("prefetch_threshold") // percentage e.g. 85
        val KEY_POPUP_THRESHOLD = intPreferencesKey("popup_threshold") // percentage e.g. 90
        val KEY_TRAKT_API_KEY = stringPreferencesKey("trakt_api_key")
        val KEY_TRAKT_CLIENT_SECRET = stringPreferencesKey("trakt_client_secret")
        val KEY_TRAKT_ACCESS_TOKEN = stringPreferencesKey("trakt_access_token")
        val KEY_TRAKT_REFRESH_TOKEN = stringPreferencesKey("trakt_refresh_token")
        val KEY_TRAKT_USERNAME = stringPreferencesKey("trakt_username")
        val KEY_TRACKING_BACKEND = stringPreferencesKey("tracking_backend") // "TRAKT" or "FLICKLIST"
        val KEY_FLICKLIST_CLIENT_ID = stringPreferencesKey("flicklist_client_id")
        val KEY_FLICKLIST_API_KEY = stringPreferencesKey("flicklist_api_key")
        val KEY_FLICKLIST_ACCESS_TOKEN = stringPreferencesKey("flicklist_access_token")
        val KEY_FLICKLIST_USERNAME = stringPreferencesKey("flicklist_username")
        val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play")
        val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")

        // Newline-joined, most-recent-first. Capped so the rail never grows
        // past what the 10-foot layout can show.
        private const val SEARCH_HISTORY_LIMIT = 8
    }

    val tmdbApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TMDB_API_KEY] ?: ""
    }

    val tmdbLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TMDB_LANGUAGE] ?: "en-US"
    }

    val tmdbRegion: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TMDB_REGION] ?: "US"
    }

    val stremioAddonUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_STREMIO_ADDON_URL] ?: "https://torrentio.strem.fun/"
    }

    val omdbApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_OMDB_API_KEY] ?: ""
    }

    /**
     * Base URL of the user's YouTube backend. Trailers are resolved from
     * `{base}/api/video/{id}/download`, which returns a DASH manifest.
     */
    val youtubeBackendUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_YOUTUBE_BACKEND_URL] ?: ""
    }

    val omdbEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val key = prefs[KEY_OMDB_API_KEY] ?: ""
        key.isNotBlank()
    }

    val selectedRatingProviders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_RATING_PROVIDERS] ?: setOf("IMDb", "TMDB", "Rotten Tomatoes", "Metacritic", "Netflix")
    }

    val stremioIdStrategy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_STREMIO_ID_STRATEGY] ?: "IMDB"
    }

    val completionThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_COMPLETION_THRESHOLD] ?: 90
    }

    val prefetchThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFETCH_THRESHOLD] ?: 85
    }

    val popupThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_POPUP_THRESHOLD] ?: 90
    }

    val traktApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAKT_API_KEY] ?: ""
    }

    val traktClientSecret: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAKT_CLIENT_SECRET] ?: ""
    }

    val traktAccessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAKT_ACCESS_TOKEN]
    }

    val traktUsername: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAKT_USERNAME]
    }

    /** Which tracking backend syncs the watchlist, resume points and scrobbles. */
    val trackingBackend: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRACKING_BACKEND] ?: "TRAKT"
    }

    val flickListClientId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLICKLIST_CLIENT_ID] ?: ""
    }

    val flickListApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLICKLIST_API_KEY] ?: ""
    }

    val flickListAccessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLICKLIST_ACCESS_TOKEN]
    }

    val flickListUsername: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLICKLIST_USERNAME]
    }

    val autoPlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PLAY] ?: true
    }

    val searchHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeSearchHistory(prefs[KEY_SEARCH_HISTORY])
    }

    suspend fun saveTmdbApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TMDB_API_KEY] = key
        }
    }

    suspend fun saveTmdbLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TMDB_LANGUAGE] = language
        }
    }

    suspend fun saveTmdbRegion(region: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TMDB_REGION] = region
        }
    }

    suspend fun saveStremioAddonUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STREMIO_ADDON_URL] = url
        }
    }

    suspend fun saveOmdbApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OMDB_API_KEY] = key
        }
    }

    suspend fun saveYoutubeBackendUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_YOUTUBE_BACKEND_URL] = url
        }
    }

    suspend fun saveTraktApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAKT_API_KEY] = key
        }
    }

    suspend fun saveTraktClientSecret(secret: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAKT_CLIENT_SECRET] = secret
        }
    }

    suspend fun setOmdbEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OMDB_ENABLED] = enabled
        }
    }

    suspend fun setRatingProviders(providers: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RATING_PROVIDERS] = providers
        }
    }

    suspend fun setStremioIdStrategy(strategy: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STREMIO_ID_STRATEGY] = strategy
        }
    }

    suspend fun saveCompletionThreshold(pct: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMPLETION_THRESHOLD] = pct.coerceIn(50, 99)
        }
    }

    suspend fun saveUpNextThreshold(pct: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POPUP_THRESHOLD] = pct.coerceIn(50, 99)
        }
    }

    suspend fun saveTraktAuth(accessToken: String, refreshToken: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAKT_ACCESS_TOKEN] = accessToken
            prefs[KEY_TRAKT_REFRESH_TOKEN] = refreshToken
            prefs[KEY_TRAKT_USERNAME] = username
        }
    }

    suspend fun clearTraktAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TRAKT_ACCESS_TOKEN)
            prefs.remove(KEY_TRAKT_REFRESH_TOKEN)
            prefs.remove(KEY_TRAKT_USERNAME)
        }
    }

    suspend fun setTrackingBackend(backend: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRACKING_BACKEND] = backend
        }
    }

    suspend fun saveFlickListClientId(clientId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLICKLIST_CLIENT_ID] = clientId
        }
    }

    suspend fun saveFlickListApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLICKLIST_API_KEY] = key
        }
    }

    suspend fun saveFlickListUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLICKLIST_USERNAME] = username
        }
    }

    suspend fun saveFlickListAuth(username: String, accessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLICKLIST_USERNAME] = username
            prefs[KEY_FLICKLIST_ACCESS_TOKEN] = accessToken
        }
    }

    /** Clears the credentials (API key included) but keeps the registered app id. */
    suspend fun clearFlickListAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_FLICKLIST_ACCESS_TOKEN)
            prefs.remove(KEY_FLICKLIST_USERNAME)
            prefs.remove(KEY_FLICKLIST_API_KEY)
        }
    }

    /**
     * Records a query at the head of the history, de-duplicating
     * case-insensitively and trimming to [SEARCH_HISTORY_LIMIT]. Called when a
     * search result is opened, not on every keystroke.
     */
    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = decodeSearchHistory(prefs[KEY_SEARCH_HISTORY])
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(SEARCH_HISTORY_LIMIT)
            prefs[KEY_SEARCH_HISTORY] = updated.joinToString("\n")
        }
    }

    private fun decodeSearchHistory(raw: String?): List<String> =
        raw?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
}
