package com.stremflix.ui.settings

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/** TV settings sections. The rail shows these; the right pane shows one at a time. */
private enum class SettingsCategory(val label: String) {
    GENERAL("General"),
    STREAMING("Streaming & Addons"),
    PLAYBACK("Playback"),
    ACCOUNTS("Accounts")
}

private val ALL_CATEGORIES = listOf(
    SettingsCategory.GENERAL,
    SettingsCategory.STREAMING,
    SettingsCategory.PLAYBACK,
    SettingsCategory.ACCOUNTS
)

/**
 * Step size for the Apple-TV style threshold row: coarse far from the target,
 * fine near the top. 5% -> 2% -> 1% keeps 97% reachable without a row of chips.
 */
private fun stepThreshold(value: Int, direction: Int, min: Int, max: Int): Int {
    val delta = when {
        value < 90 -> 5
        value < 95 -> 2
        else -> 1
    }
    return (value + direction * delta).coerceIn(min, max)
}

private val THRESHOLD_PRESETS = listOf(75, 80, 85, 90, 95)

@OptIn(ExperimentalComposeUiApi::class)
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
    var category by remember { mutableStateOf(SettingsCategory.GENERAL) }

    // First control of the active pane. Only one category is composed at a time
    // on TV, so a single requester can never be attached to two live nodes.
    val firstControl = remember { FocusRequester() }
    val firstControlModifier: Modifier =
        if (isMobile) Modifier else Modifier.focusRequester(firstControl)

    LaunchedEffect(category, isMobile) {
        if (!isMobile) {
            withFrameNanos { }
            runCatching { firstControl.requestFocus() }
        }
    }

    // The pane body closes over every parameter, so it can be shared by the TV
    // two-pane layout and the mobile single-column layout without threading
    // thirty arguments through another composable.
    val body: @Composable (Modifier, Boolean, List<SettingsCategory>) -> Unit = { m, mobile, cats ->
        LazyColumn(
            modifier = m.focusRestorer(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = NetflixWhite,
                    fontSize = if (mobile) 22.sp else 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (mobile) {
                        "Settings auto-save automatically. Use D-pad selectors or tap to configure."
                    } else {
                        "Use ◀ ▶ to change a value, OK to cycle presets or open a list. Everything auto-saves."
                    },
                    color = NetflixLightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (SettingsCategory.GENERAL in cats) {
                item {
                    SettingsSectionHeader(title = "TMDB API Key (Required)")
                    SettingHint("Enter your personal TMDB API key to load movies, series, and artwork.")
                    AutoSaveInputField(
                        initialValue = tmdbApiKey,
                        placeholder = "Enter TMDB API Key (e.g. 3fd2be6f...)",
                        onSave = onSaveTmdbApiKey,
                        modifier = firstControlModifier
                    )
                }

                item {
                    SettingsSectionHeader(title = "TMDB Language & Country / Region")
                    SettingHint("Controls metadata language and regional release popularity rankings.")
                    TvCyclicSelector(
                        label = "Audio & Subtitle Language",
                        options = SUPPORTED_LANGUAGES.map { it.code to it.name },
                        selectedCode = tmdbLanguage,
                        onSelect = onSaveTmdbLanguage
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TvCyclicSelector(
                        label = "Country / Region",
                        options = SUPPORTED_COUNTRIES.map { it.code to it.name },
                        selectedCode = tmdbRegion,
                        onSelect = onSaveTmdbRegion
                    )
                }

                item {
                    SettingsSectionHeader(title = "Display Rating Providers")
                    SettingHint("Choose which rating sources appear on the details screen.")
                    listOf("IMDb", "TMDB", "Rotten Tomatoes", "Metacritic", "Netflix").forEach { provider ->
                        TvToggleRow(
                            label = provider,
                            checked = selectedProviders.contains(provider),
                            onToggle = { onToggleProvider(provider) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    SettingsSectionHeader(title = "OMDb Ratings Integration")
                    SettingHint("An OMDb key enables Rotten Tomatoes, Metacritic and IMDb rating clusters.")
                    AutoSaveInputField(
                        initialValue = omdbApiKey,
                        placeholder = "Enter OMDb API Key (e.g. 1a2b3c4d)",
                        onSave = onSaveOmdbApiKey
                    )
                }
            }

            if (SettingsCategory.STREAMING in cats) {
                item {
                    SettingsSectionHeader(title = "Stremio Addon Base URL")
                    SettingHint("Custom Stremio addon manifest endpoint used to resolve streams.")
                    AutoSaveInputField(
                        initialValue = stremioAddonUrl,
                        placeholder = "e.g. https://torrentio.strem.fun/",
                        onSave = onSaveStremioAddonUrl,
                        modifier = firstControlModifier
                    )
                }

                item {
                    SettingsSectionHeader(title = "Stremio Stream Resolution ID Strategy")
                    SettingHint("Which identifier is sent to the addon when resolving a stream.")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvChoiceButton(
                            text = "IMDb IDs (tt1234567)",
                            selected = stremioIdStrategy == "IMDB",
                            onClick = { onSelectStremioStrategy("IMDB") }
                        )
                        TvChoiceButton(
                            text = "TMDB IDs (tmdb:123)",
                            selected = stremioIdStrategy == "TMDB",
                            onClick = { onSelectStremioStrategy("TMDB") }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(title = "YouTube Trailer Backend")
                    SettingHint("Base URL of your YouTube backend. Trailers load from /api/video/{id}/download.")
                    AutoSaveInputField(
                        initialValue = youtubeBackendUrl,
                        placeholder = "Enter backend base URL (e.g. http://127.0.0.1:8000)",
                        onSave = onSaveYoutubeBackendUrl
                    )
                }
            }

            if (SettingsCategory.PLAYBACK in cats) {
                item {
                    SettingsSectionHeader(title = "Playback & Next Episode Thresholds")
                    SettingHint(
                        "When an episode reaches this progress %, clicking Play advances to the next " +
                            "episode. Below it, Play resumes the current one."
                    )
                    TvThresholdRow(
                        label = "Episode Completion Threshold",
                        currentValue = completionThreshold,
                        onValueChange = onSaveCompletionThreshold,
                        min = 50,
                        max = 98,
                        modifier = firstControlModifier
                    )
                }

                item {
                    SettingHint(
                        "Triggers the Up Next overlay near the end of a video. Stream preloading " +
                            "starts 5% earlier (${(upNextThreshold - 5).coerceAtLeast(10)}%)."
                    )
                    TvThresholdRow(
                        label = "Up Next Popup Trigger Threshold",
                        currentValue = upNextThreshold,
                        onValueChange = onSaveUpNextThreshold,
                        min = 50,
                        max = 99
                    )
                }
            }

            if (SettingsCategory.ACCOUNTS in cats) {
                item {
                    SettingsSectionHeader(title = stringResource(R.string.trakt_account_title))
                    SettingHint("Trakt Client ID and Client Secret from your Trakt API app. Both are required for device pairing.")
                    AutoSaveInputField(
                        initialValue = traktApiKey,
                        placeholder = "Enter Trakt Client ID / API Key",
                        onSave = onSaveTraktApiKey,
                        modifier = firstControlModifier
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AutoSaveInputField(
                        initialValue = traktClientSecret,
                        placeholder = "Enter Trakt Client Secret",
                        onSave = onSaveTraktClientSecret
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!traktUsername.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.trakt_connected_status, traktUsername),
                            color = NetflixRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TvActionButton(
                            text = stringResource(R.string.action_disconnect),
                            onClick = onDisconnectTrakt
                        )
                    } else if (!traktDeviceCode.isNullOrBlank() && !traktVerificationUrl.isNullOrBlank()) {
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
                        TvActionButton(
                            text = stringResource(R.string.action_connect),
                            onClick = onRequestTraktPairing
                        )
                    }
                }
            }
        }
    }

    if (isMobile) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            body(Modifier.fillMaxSize(), true, ALL_CATEGORIES)
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .statusBarsPadding()
                .padding(horizontal = 48.dp, vertical = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .focusRestorer()
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = NetflixWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )
                ALL_CATEGORIES.forEach { c ->
                    CategoryItem(
                        label = c.label,
                        selected = category == c,
                        onClick = { category = c }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            body(Modifier.weight(1f).fillMaxHeight(), false, listOf(category))
        }
    }
}

@Composable
private fun CategoryItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NetflixDimens.RailItemHeight)
            .clip(shape)
            .background(
                when {
                    focused -> NetflixWhite.copy(alpha = 0.12f)
                    selected -> NetflixDarkSurface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(22.dp)
                .background(
                    color = if (selected) NetflixRed else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (selected || focused) NetflixWhite else NetflixLightGray,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Apple TV / Netflix style stepper: one focusable row, no chip clutter.
 *
 * D-pad LEFT/RIGHT adjust by 5% below 90, 2% up to 95 and 1% above it, so both
 * coarse and 1%-precise edits are reachable from the remote. OK cycles presets.
 */
@Composable
fun TvThresholdRow(
    label: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 50,
    max: Int = 99,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, label = "thresholdScale")
    val shape = RoundedCornerShape(8.dp)
    val presets = remember(min, max) { THRESHOLD_PRESETS.filter { it in min..max } }
    val fraction = (((currentValue - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f))
        .coerceAtLeast(0.02f)

    fun cyclePreset() {
        val next = presets.firstOrNull { it > currentValue } ?: presets.firstOrNull() ?: currentValue
        onValueChange(next)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(if (focused) NetflixDarkSurface else NetflixCardSurface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NetflixWhite else NetflixBorderHighlight,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onValueChange(stepThreshold(currentValue, -1, min, max)); true
                    }

                    Key.DirectionRight -> {
                        onValueChange(stepThreshold(currentValue, 1, min, max)); true
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        cyclePreset(); true
                    }

                    else -> false
                }
            }
            .clickable { cyclePreset() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NetflixWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "◀",
                color = if (focused) NetflixWhite else NetflixLightGray,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "$currentValue%",
                color = NetflixRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "▶",
                color = if (focused) NetflixWhite else NetflixLightGray,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Value track. Purely decorative: focus stays on the row, so there is
        // never a second D-pad stop for the same setting.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NetflixLightGray.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(NetflixRed, RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "◀ ▶ adjust • OK cycles 75/80/85/90/95",
            color = NetflixLightGray,
            fontSize = 11.sp
        )
    }
}

/**
 * Inline cyclic selector: LEFT/RIGHT step through options, OK opens a TV modal
 * list. Replaces the mouse-oriented DropdownMenu, which a remote could open but
 * not navigate comfortably.
 */
@Composable
fun TvCyclicSelector(
    label: String,
    options: List<Pair<String, String>>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }
    val index = options.indexOfFirst { it.first == selectedCode }.coerceAtLeast(0)
    val current = options.getOrNull(index)?.second ?: "—"
    val shape = RoundedCornerShape(NetflixDimens.CardRadius)

    fun move(direction: Int) {
        if (options.isEmpty()) return
        val next = (index + direction + options.size) % options.size
        onSelect(options[next].first)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (focused) NetflixCardSurface else NetflixDarkSurface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NetflixWhite else NetflixLightGray.copy(alpha = 0.3f),
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        move(-1); true
                    }

                    Key.DirectionRight -> {
                        move(1); true
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        showList = true; true
                    }

                    else -> false
                }
            }
            .clickable { showList = true }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NetflixLightGray,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "◀",
            color = if (focused) NetflixWhite else NetflixLightGray,
            fontSize = 15.sp
        )
        Text(
            text = current,
            color = NetflixWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
            maxLines = 1
        )
        Text(
            text = "▶",
            color = if (focused) NetflixWhite else NetflixLightGray,
            fontSize = 15.sp
        )
    }

    if (showList) {
        TvSelectionDialog(
            title = label,
            options = options,
            selectedCode = selectedCode,
            onSelect = {
                onSelect(it)
                showList = false
            },
            onDismiss = { showList = false }
        )
    }
}

@Composable
private fun TvSelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val shapes = RoundedCornerShape(10.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 560.dp)
                .clip(shapes)
                .background(NetflixDarkSurface)
                .border(2.dp, NetflixWhite.copy(alpha = 0.85f), shapes)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = NetflixWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options, key = { it.first }) { option ->
                    DialogOptionRow(
                        label = option.second,
                        selected = option.first == selectedCode,
                        requestFocusOnStart = option.first == selectedCode,
                        onClick = { onSelect(option.first) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogOptionRow(
    label: String,
    selected: Boolean,
    requestFocusOnStart: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val requester = remember { FocusRequester() }

    if (requestFocusOnStart) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(
                when {
                    focused -> NetflixWhite.copy(alpha = 0.16f)
                    selected -> NetflixWhite.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "✓" else " ",
            color = NetflixRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = label,
            color = if (selected || focused) NetflixWhite else NetflixLightGray,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** Focusable checkbox replacement with a trailing check badge. */
@Composable
fun TvToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, label = "toggleScale")
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(
                when {
                    focused -> NetflixWhite.copy(alpha = 0.15f)
                    checked -> NetflixDarkSurface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NetflixWhite else NetflixBorderHighlight,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NetflixWhite,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (checked) "✓ Included" else "— Hidden",
            color = if (checked) NetflixRed else NetflixLightGray,
            fontSize = 13.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** TV action/choice button: focused scale + 2dp white border, no glow. */
@Composable
fun TvChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "choiceScale")
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(
                when {
                    selected -> NetflixRed
                    focused -> NetflixWhite.copy(alpha = 0.22f)
                    else -> NetflixCardSurface
                }
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NetflixWhite else NetflixBorderHighlight,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NetflixWhite,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Red filled action button (connect/disconnect) with a TV focus ring. */
@Composable
fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "actionScale")
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(NetflixRed)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NetflixWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
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
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = NetflixWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingHint(text: String) {
    Text(
        text = text,
        color = NetflixLightGray,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}