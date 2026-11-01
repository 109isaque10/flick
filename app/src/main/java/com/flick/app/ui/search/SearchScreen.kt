package com.flick.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.app.R
import com.flick.app.data.model.MediaItem
import com.flick.app.ui.components.NetflixCleanPosterCard
import com.flick.app.ui.components.NetflixSelectionButton
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.components.touchClickable
import com.flick.app.ui.theme.NetflixBorderHighlight
import com.flick.app.ui.theme.NetflixCardSurface
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite
import com.flick.app.ui.theme.isCompactScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<MediaItem>,
    onMediaSelected: (MediaItem) -> Unit,
    searchHistory: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isMobile = isCompactScreen()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isMobile) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_search),
                color = NetflixWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            var isSearchFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        if (isSearchFocused) NetflixCardSurface else NetflixDarkSurface,
                        RoundedCornerShape(NetflixDimens.CardRadius)
                    )
                    .border(
                        width = if (isSearchFocused) 2.dp else 1.dp,
                        color = if (isSearchFocused) NetflixRed else NetflixLightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(NetflixDimens.CardRadius)
                    )
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.nav_search),
                    tint = NetflixLightGray,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { onQueryChange(it.replace("\n", "")) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    textStyle = TextStyle(
                        color = NetflixWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(NetflixWhite),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                color = NetflixLightGray,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.touchClickable { onQueryChange("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NetflixLightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Recent searches — mobile counterpart of the TV rail's quick items.
            // Hidden as soon as the box has content so results take over.
            if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.search_recent_header),
                    color = NetflixLightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(searchHistory, key = { it }) { entry ->
                        MobileSearchHistoryChip(label = entry, onClick = { onQueryChange(entry) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (searchResults.isNotEmpty()) {
                val distinctResults =
                    remember(searchResults) { searchResults.distinctBy { "${it.mediaType}_${it.id}" } }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(distinctResults, key = { _, item -> "${item.mediaType}_${item.id}" }) { _, item ->
                        NetflixCleanPosterCard(posterUrl = item.fullPosterUrl, title = item.title, onClick = { onMediaSelected(item) }, posterWidth = null)
                    }
                }
            } else {
                // The mobile branch previously rendered a blank screen for both
                // "nothing typed yet" and "no matches".
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.search_hint)
                        } else {
                            stringResource(R.string.search_no_results, searchQuery)
                        },
                        color = NetflixLightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
            }
        }
    } else {
        // TV 2-Pane Search Layout (image 8)
        // Single requester for the first keyboard key ("Q"), used once so D-pad
        // typing can begin immediately without navigating from the query bar.
        val keyboardFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            // Retry across frames: the key grid is lazily composed, so a single
            // requestFocus() on the first frame silently fails and leaves focus
            // parked on the sidebar (N3).
            repeat(6) {
                withFrameNanos { }
                if (runCatching { keyboardFocusRequester.requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
            }
        }

        Row(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp)
        ) {
            // Left TV rail: D-pad on-screen keyboard.
            //
            // Spec: 280dp rail, six keys per row, spacebar and backspace on their
            // own control row, and a focused key that is solid white with bold
            // black text. Keys are chunked to six per row rather than using the
            // old staggered QWERTY matrix so every key lines up vertically.
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.search_press_up),
                    color = NetflixLightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                var isSearchFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(NetflixDarkSurface, RoundedCornerShape(4.dp))
                        .border(
                            width = if (isSearchFocused) 2.dp else 1.dp,
                            color = if (isSearchFocused) NetflixBorderHighlight else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .onFocusChanged { isSearchFocused = it.isFocused }
                        // No-op click makes the bar a D-pad focus target so the
                        // focus border above actually becomes reachable.
                        .clickable { }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.nav_search),
                        tint = NetflixLightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { onQueryChange(it.replace("\n", "")) },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = TextStyle(color = NetflixWhite, fontSize = 14.sp),
                        cursorBrush = SolidColor(NetflixWhite),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_empty_hint_tv),
                                    color = NetflixLightGray,
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.close),
                            tint = NetflixLightGray,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onQueryChange("") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val keyShape = RoundedCornerShape(4.dp)
                val keyboardRows = remember {
                    ("abcdefghijklmnopqrstuvwxyz" + "1234567890").chunked(6)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NetflixBlack, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Top control row: wide spacebar, then backspace.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        var isSpaceFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .height(40.dp)
                                .background(if (isSpaceFocused) NetflixWhite else NetflixDarkSurface, keyShape)
                                .border(
                                    width = if (isSpaceFocused) 2.dp else 1.dp,
                                    color = if (isSpaceFocused) NetflixWhite else Color.White.copy(alpha = 0.1f),
                                    shape = keyShape
                                )
                                .onFocusChanged { isSpaceFocused = it.isFocused }
                                .clickable { onQueryChange("$searchQuery ") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_space),
                                color = if (isSpaceFocused) NetflixBlack else NetflixWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        var isBackspaceFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(if (isBackspaceFocused) NetflixWhite else NetflixDarkSurface, keyShape)
                                .border(
                                    width = if (isBackspaceFocused) 2.dp else 1.dp,
                                    color = if (isBackspaceFocused) NetflixWhite else Color.White.copy(alpha = 0.1f),
                                    shape = keyShape
                                )
                                .onFocusChanged { isBackspaceFocused = it.isFocused }
                                .clickable {
                                    if (searchQuery.isNotEmpty()) {
                                        onQueryChange(searchQuery.dropLast(1))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = stringResource(R.string.close),
                                tint = if (isBackspaceFocused) NetflixBlack else NetflixWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    keyboardRows.forEachIndexed { rowIndex, rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowKeys.forEachIndexed { keyIndex, charKey ->
                                var isKeyFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .background(if (isKeyFocused) NetflixWhite else NetflixDarkSurface, keyShape)
                                        .border(
                                            width = if (isKeyFocused) 2.dp else 1.dp,
                                            color = if (isKeyFocused) NetflixWhite else Color.White.copy(alpha = 0.1f),
                                            shape = keyShape
                                        )
                                        // Exactly one node carries the requester, so
                                        // D-pad typing can begin without a trip to
                                        // the sidebar.
                                        .then(
                                            if (rowIndex == 0 && keyIndex == 0) {
                                                Modifier.focusRequester(keyboardFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .onFocusChanged { isKeyFocused = it.isFocused }
                                        .clickable { onQueryChange(searchQuery + charKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = charKey.toString(),
                                        color = if (isKeyFocused) NetflixBlack else NetflixWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            repeat(6 - rowKeys.length) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Quick items: recent searches, in a column below the key grid.
                // D-pad DOWN from the last key row lands here naturally.
                if (searchHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.search_recent_header),
                        color = NetflixLightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        searchHistory.forEach { entry ->
                            NetflixSelectionButton(
                                label = entry,
                                onClick = { onQueryChange(entry) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Right Results Pane (4 columns)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (searchResults.isNotEmpty()) {
                    val distinctResults =
                        remember(searchResults) { searchResults.distinctBy { "${it.mediaType}_${it.id}" } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(distinctResults, key = { _, item -> "${item.mediaType}_${item.id}" }) { _, item ->
                            NetflixCleanPosterCard(posterUrl = item.fullPosterUrl, title = item.title, onClick = { onMediaSelected(item) }, posterWidth = null)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) stringResource(R.string.search_empty_prompt) else stringResource(
                                R.string.search_no_results,
                                searchQuery
                            ),
                            color = NetflixLightGray,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 520.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent-search chip for the mobile layout. Unlike the TV quick items these are
 * touch targets, so they carry a visible surface instead of relying on focus.
 */
@Composable
private fun MobileSearchHistoryChip(
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(NetflixDimens.CardRadius))
            .background(NetflixCardSurface)
            .border(
                width = 1.dp,
                color = NetflixLightGray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = NetflixLightGray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = NetflixWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

