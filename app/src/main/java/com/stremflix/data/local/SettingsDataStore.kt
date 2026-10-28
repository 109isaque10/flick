package com.stremflix.data.local

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stremflix_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val KEY_TMDB_LANGUAGE = stringPreferencesKey("tmdb_language")
        val KEY_TMDB_REGION = stringPreferencesKey("tmdb_region")
        val KEY_STREMIO_ADDON_URL = stringPreferencesKey("stremio_addon_url")
        val KEY_OMDB_API_KEY = stringPreferencesKey("omdb_api_key")
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
        val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play")
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

    val autoPlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PLAY] ?: true
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
}
