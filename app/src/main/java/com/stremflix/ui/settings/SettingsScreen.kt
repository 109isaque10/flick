package com.stremflix.ui.settings

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremflix.R
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import com.stremflix.ui.theme.isCompactScreen
import kotlinx.coroutines.delay

data class LanguageOption(val code: String, val name: String)
data class CountryOption(val code: String, val name: String)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("pt-BR", "Português (Brasil)"),
    LanguageOption("en-US", "English (United States)"),
    LanguageOption("es-ES", "Español (España)"),
    LanguageOption("fr-FR", "Français"),
    LanguageOption("de-DE", "Deutsch"),
    LanguageOption("it-IT", "Italiano"),
    LanguageOption("ja-JP", "日本語 (Japanese)"),
    LanguageOption("zh-CN", "中文 (Chinese)"),
    LanguageOption("ru-RU", "Русский (Russian)"),
    LanguageOption("ar-SA", "العربية (Arabic)")
)

val SUPPORTED_COUNTRIES = listOf(
    CountryOption("BR", "Brazil (Brasil)"),
    CountryOption("US", "United States"),
    CountryOption("GB", "United Kingdom"),
    CountryOption("ES", "Spain"),
    CountryOption("FR", "France"),
    CountryOption("DE", "Germany"),
    CountryOption("IT", "Italy"),
    CountryOption("JP", "Japan"),
    CountryOption("AR", "Argentina"),
    CountryOption("MX", "Mexico")
)

@Composable
fun SettingsScreen(
    tmdbApiKey: String,
    onSaveTmdbApiKey: (String) -> Unit,
    tmdbLanguage: String,
    onSaveTmdbLanguage: (String) -> Unit,
    tmdbRegion: String,
    onSaveTmdbRegion: (String) -> Unit,
    stremioAddonUrl: String,
    onSaveStremioAddonUrl: (String) -> Unit,
    omdbApiKey: String,
    onSaveOmdbApiKey: (String) -> Unit,
    youtubeBackendUrl: String,
    onSaveYoutubeBackendUrl: (String) -> Unit,
    selectedProviders: Set<String>,
    onToggleProvider: (String) -> Unit,
    stremioIdStrategy: String,
    onSelectStremioStrategy: (String) -> Unit,
    traktApiKey: String,
    onSaveTraktApiKey: (String) -> Unit,
    traktClientSecret: String,
    onSaveTraktClientSecret: (String) -> Unit,
    traktUsername: String?,
    traktDeviceCode: String?,
    traktVerificationUrl: String?,
    onRequestTraktPairing: () -> Unit,
    onDisconnectTrakt: () -> Unit,
    completionThreshold: Int = 90,
    onSaveCompletionThreshold: (Int) -> Unit = {},
    upNextThreshold: Int = 90,
    onSaveUpNextThreshold: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMobile = isCompactScreen()
    val screenPadding = if (isMobile) 16.dp else 40.dp

    // Enter the screen on the first setting, not on the invisible content
    // container left behind by sidebar navigation.
    val firstSettingRequester = remember { FocusRequester() }
    LaunchedEffect(isMobile) {
        if (!isMobile) {
            withFrameNanos { }
            runCatching { firstSettingRequester.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .statusBarsPadding()
            .padding(screenPadding)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                color = NetflixWhite,
                fontSize = if (isMobile) 22.sp else 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Settings auto-save automatically. Use D-pad selectors or tap to configure.",
                color = NetflixLightGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // TMDB API Key Section
            SettingsSectionHeader(title = "TMDB API Key (Required)")
            Text(
                text = "Enter your personal TMDB API key to load movies, series, and artwork.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AutoSaveInputField(
                initialValue = tmdbApiKey,
                placeholder = "Enter TMDB API Key (e.g. 3fd2be6f...)",
                onSave = onSaveTmdbApiKey,
                modifier = Modifier.focusRequester(firstSettingRequester)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // TMDB Language & Region Dropdowns
            SettingsSectionHeader(title = "TMDB Language & Country / Region")
            Text(
                text = "Controls metadata language and regional release popular rankings.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LanguageDropdownMenu(
                        selectedCode = tmdbLanguage,
                        onSelectLanguage = onSaveTmdbLanguage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CountryDropdownMenu(
                        selectedCode = tmdbRegion,
                        onSelectCountry = onSaveTmdbRegion,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LanguageDropdownMenu(
                        selectedCode = tmdbLanguage,
                        onSelectLanguage = onSaveTmdbLanguage,
                        modifier = Modifier.weight(1f)
                    )
                    CountryDropdownMenu(
                        selectedCode = tmdbRegion,
                        onSelectCountry = onSaveTmdbRegion,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Stremio Addon Base URL Section
            SettingsSectionHeader(title = "Stremio Addon Base URL")
            Text(
                text = "Custom Stremio addon manifest endpoint URL for resolving streams.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AutoSaveInputField(
                initialValue = stremioAddonUrl,
                placeholder = "e.g. https://torrentio.strem.fun/",
                onSave = onSaveStremioAddonUrl
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // OMDB API Key Section
            SettingsSectionHeader(title = "OMDb Ratings Integration")
            Text(
                text = "Entering an OMDb API key automatically enables fetching Rotten Tomatoes, Metacritic, and IMDb rating clusters.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AutoSaveInputField(
                initialValue = omdbApiKey,
                placeholder = "Enter OMDb API Key (e.g. 1a2b3c4d)",
                onSave = onSaveOmdbApiKey
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            // Rating Provider Clusters Selection
            SettingsSectionHeader(title = "Display Rating Providers")

            val allProviders = listOf("IMDb", "TMDB", "Rotten Tomatoes", "Metacritic", "Netflix")
            allProviders.forEach { provider ->
                val isChecked = selectedProviders.contains(provider)
                var rowFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowFocused) NetflixDarkSurface else NetflixBlack,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = if (rowFocused) 2.dp else 1.dp,
                            color = if (rowFocused) NetflixBorderHighlight else NetflixBlack,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .onFocusChanged { rowFocused = it.isFocused || it.hasFocus }
                        .clickable { onToggleProvider(provider) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleProvider(provider) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NetflixRed,
                            uncheckedColor = NetflixLightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = provider,
                        color = NetflixWhite,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            // Stremio Stream ID Strategy
            SettingsSectionHeader(title = "Stremio Stream Resolution ID Strategy")

            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StremioStrategyButtons(stremioIdStrategy, onSelectStremioStrategy)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StremioStrategyButtons(stremioIdStrategy, onSelectStremioStrategy)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            // YouTube Trailer Backend
            SettingsSectionHeader(title = "YouTube Trailer Backend")
            Text(
                text = "Base URL of your YouTube backend (e.g. http://192.168.1.10:8000). Trailers are loaded from /api/video/{id}/download.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AutoSaveInputField(
                initialValue = youtubeBackendUrl,
                placeholder = "Enter backend base URL (e.g. http://127.0.0.1:8000)",
                onSave = onSaveYoutubeBackendUrl
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            // Playback & Next Episode Thresholds (D-pad Friendly Stepper)
            SettingsSectionHeader(title = "Playback & Next Episode Thresholds")

            Text(
                text = "When an episode reaches this progress %, clicking Play advances to the next episode. Below this, clicking Play resumes current episode.",
                color = NetflixLightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ThresholdStepper(
                label = "Episode Completion Threshold",
                currentValue = completionThreshold,
                onValueChange = onSaveCompletionThreshold,
                min = 50,
                max = 98,
                step = 5
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Triggers the Up Next overlay near end of video. Stream preloading automatically triggers 5% prior (${
                    (upNextThreshold - 5).coerceAtLeast(
                        10
                    )
                }%).",
                color = NetflixLightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ThresholdStepper(
                label = "Up Next Popup Trigger Threshold",
                currentValue = upNextThreshold,
                onValueChange = onSaveUpNextThreshold,
                min = 50,
                max = 98,
                step = 5
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            // Trakt.tv Integration & API Key
            SettingsSectionHeader(title = stringResource(R.string.trakt_account_title))
            Text(
                text = "Trakt Client ID and Client Secret from your Trakt API app. Both are required for device pairing.",
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AutoSaveInputField(
                initialValue = traktApiKey,
                placeholder = "Enter Trakt Client ID / API Key",
                onSave = onSaveTraktApiKey
            )

            Spacer(modifier = Modifier.height(12.dp))

            AutoSaveInputField(
                initialValue = traktClientSecret,
                placeholder = "Enter Trakt Client Secret",
                onSave = onSaveTraktClientSecret
            )

            Spacer(modifier = Modifier.height(16.dp))

            var connectFocused by remember { mutableStateOf(false) }
            var disconnectFocused by remember { mutableStateOf(false) }

            if (!traktUsername.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.trakt_connected_status, traktUsername),
                    color = NetflixRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDisconnectTrakt,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .border(
                            width = if (disconnectFocused) 2.dp else 0.dp,
                            color = if (disconnectFocused) NetflixWhite else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .onFocusChanged { disconnectFocused = it.isFocused || it.hasFocus }
                ) {
                    Text(text = stringResource(R.string.action_disconnect), color = NetflixWhite)
                }
            } else {
                if (!traktDeviceCode.isNullOrBlank() && !traktVerificationUrl.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NetflixCardSurface, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_trakt_step_visit_format,
                                traktVerificationUrl
                            ),
                            color = NetflixWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.settings_trakt_step_code_format,
                                traktDeviceCode
                            ),
                            color = NetflixRed,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestTraktPairing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRed,
                            contentColor = NetflixWhite
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .border(
                                width = if (connectFocused) 2.dp else 0.dp,
                                color = if (connectFocused) NetflixWhite else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .onFocusChanged { connectFocused = it.isFocused || it.hasFocus }
                    ) {
                        Text(text = stringResource(R.string.action_connect), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ThresholdStepper(
    label: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 50,
    max: Int = 100,
    step: Int = 5
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NetflixDarkSurface, RoundedCornerShape(8.dp))
            .border(1.dp, NetflixBorderHighlight, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NetflixWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentValue%",
                color = NetflixRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Discrete choices instead of a Slider. A Slider cannot be driven by a
        // TV remote: D-pad focus could land on the track but left/right never
        // resolved to a value, so these thresholds could not be changed at all.
        val presets = listOf(70, 85, 95).filter { it in min..max }
            .ifEmpty { listOf(min, max) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThresholdChip(
                text = "\u2212",
                selected = false,
                enabled = currentValue > min,
                onClick = { onValueChange((currentValue - step).coerceAtLeast(min)) }
            )
            presets.forEach { preset ->
                ThresholdChip(
                    text = "$preset%",
                    selected = currentValue == preset,
                    enabled = true,
                    onClick = { onValueChange(preset) }
                )
            }
            ThresholdChip(
                text = "+",
                selected = false,
                enabled = currentValue < max,
                onClick = { onValueChange((currentValue + step).coerceAtMost(max)) }
            )
        }
    }
}

@Composable
private fun ThresholdChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    selected -> NetflixRed
                    focused -> NetflixWhite.copy(alpha = 0.22f)
                    else -> NetflixCardSurface
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) NetflixWhite else NetflixLightGray.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun LanguageDropdownMenu(
    selectedCode: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val currentOption = SUPPORTED_LANGUAGES.find { it.code == selectedCode } ?: SUPPORTED_LANGUAGES[0]

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    if (isFocused) NetflixCardSurface else NetflixDarkSurface,
                    RoundedCornerShape(NetflixDimens.CardRadius)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) NetflixRed else NetflixLightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(NetflixDimens.CardRadius)
                )
                .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_language_value_format, currentOption.name),
                color = NetflixWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown",
                tint = NetflixWhite
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NetflixDarkSurface)
        ) {
            SUPPORTED_LANGUAGES.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.name,
                            color = if (option.code == selectedCode) NetflixRed else NetflixWhite,
                            fontWeight = if (option.code == selectedCode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectLanguage(option.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CountryDropdownMenu(
    selectedCode: String,
    onSelectCountry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val currentOption = SUPPORTED_COUNTRIES.find { it.code == selectedCode } ?: SUPPORTED_COUNTRIES[0]

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    if (isFocused) NetflixCardSurface else NetflixDarkSurface,
                    RoundedCornerShape(NetflixDimens.CardRadius)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) NetflixRed else NetflixLightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(NetflixDimens.CardRadius)
                )
                .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_country_value_format, currentOption.name),
                color = NetflixWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown",
                tint = NetflixWhite
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NetflixDarkSurface)
        ) {
            SUPPORTED_COUNTRIES.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.name,
                            color = if (option.code == selectedCode) NetflixRed else NetflixWhite,
                            fontWeight = if (option.code == selectedCode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectCountry(option.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AutoSaveInputField(
    initialValue: String,
    placeholder: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    var isFocused by remember { mutableStateOf(false) }
    val isTv = !isCompactScreen()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-save debouncer as user types
    LaunchedEffect(input) {
        if (input != initialValue) {
            delay(300)
            onSave(input)
        }
    }

    // Do not let D-pad navigation through Settings pop the IME on every text
    // field. OK explicitly opens it below.
    LaunchedEffect(isFocused, isTv) {
        if (isTv && isFocused) {
            keyboardController?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                if (isFocused) NetflixCardSurface else NetflixDarkSurface,
                RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) {
                    if (isTv) NetflixWhite else NetflixRed
                } else {
                    NetflixLightGray.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = input,
            onValueChange = { newVal ->
                // Strip newlines so enter does not insert \n
                input = newVal.replace("\n", "")
            },
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSave(input)
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            textStyle = TextStyle(color = NetflixWhite, fontSize = 15.sp),
            cursorBrush = SolidColor(NetflixWhite),
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused || focusState.hasFocus
                }
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            // On TV, OK on a field has to open the system keyboard;
                            // otherwise the field can never be edited by remote.
                            if (isTv) {
                                keyboardController?.show()
                            } else {
                                onSave(input)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                if (input.isEmpty()) {
                    Text(text = placeholder, color = NetflixLightGray, fontSize = 14.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun StremioStrategyButtons(
    stremioIdStrategy: String,
    onSelectStremioStrategy: (String) -> Unit
) {
    var imdbBtnFocused by remember { mutableStateOf(false) }
    Button(
        onClick = { onSelectStremioStrategy("IMDB") },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (stremioIdStrategy == "IMDB") NetflixRed else NetflixDarkSurface,
            contentColor = NetflixWhite
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .border(
                width = if (imdbBtnFocused) 2.dp else 0.dp,
                color = if (imdbBtnFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { imdbBtnFocused = it.isFocused || it.hasFocus }
    ) {
        Text(text = "IMDb IDs (tt1234567)")
    }

    var tmdbBtnFocused by remember { mutableStateOf(false) }
    Button(
        onClick = { onSelectStremioStrategy("TMDB") },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (stremioIdStrategy == "TMDB") NetflixRed else NetflixDarkSurface,
            contentColor = NetflixWhite
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .border(
                width = if (tmdbBtnFocused) 2.dp else 0.dp,
                color = if (tmdbBtnFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { tmdbBtnFocused = it.isFocused || it.hasFocus }
    ) {
        Text(text = "TMDB IDs (tmdb:123)")
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = NetflixWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
