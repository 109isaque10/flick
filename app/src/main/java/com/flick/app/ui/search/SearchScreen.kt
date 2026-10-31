package com.flick.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.Search
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.app.R
import com.flick.app.data.model.MediaItem
import com.flick.app.ui.home.PosterCard
import com.flick.app.ui.theme.NetflixBlack
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
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NetflixLightGray,
                            modifier = Modifier.size(18.dp)
                        )
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
                        PosterCard(item = item, onClick = { onMediaSelected(item) }, cardWidth = null)
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
            withFrameNanos { }
            runCatching { keyboardFocusRequester.requestFocus() }
        }

        Row(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp)
        ) {
            // Left TV Keyboard & Search Bar Pane
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(R.string.search_press_up),
                    color = NetflixLightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // TV Search Query Display Bar
                var isSearchFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(NetflixDarkSurface, RoundedCornerShape(NetflixDimens.CardRadius))
                        .border(
                            width = if (isSearchFocused) 2.dp else 1.dp,
                            color = if (isSearchFocused) NetflixBorderHighlight else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(NetflixDimens.CardRadius)
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

                Spacer(modifier = Modifier.height(14.dp))

                // QWERTY TV keyboard laid out as a Netflix-style matrix: every row
                // shares the same column count, so keys line up vertically instead
                // of being staggered with per-row insets.
                val keyboardRows = listOf(
                    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                    listOf("Z", "X", "C", "V", "B", "N", "M"),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                )
                val keyboardColumnCount = 10
                val keyHeight = 40.dp
                val keyShape = RoundedCornerShape(4.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keyboardRows.forEachIndexed { rowIndex, rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEachIndexed { keyIndex, charKey ->
                                var isKeyFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(keyHeight)
                                        .background(
                                            if (isKeyFocused) Color.White.copy(alpha = 0.25f) else NetflixDarkSurface,
                                            keyShape
                                        )
                                        .border(
                                            width = if (isKeyFocused) 2.dp else 1.dp,
                                            color = if (isKeyFocused) NetflixBorderHighlight else Color.White.copy(alpha = 0.1f),
                                            shape = keyShape
                                        )
                                        .then(
                                            if (rowIndex == 0 && keyIndex == 0) {
                                                Modifier.focusRequester(keyboardFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .onFocusChanged { isKeyFocused = it.isFocused }
                                        .clickable { onQueryChange(searchQuery + charKey.lowercase()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = charKey,
                                        color = NetflixWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            repeat(keyboardColumnCount - rowKeys.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Space, Backspace, Clear buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onQueryChange("$searchQuery ") },
                        colors = ButtonDefaults.colors(
                            containerColor = NetflixDarkSurface,
                            contentColor = NetflixWhite,
                            focusedContainerColor = NetflixWhite,
                            focusedContentColor = NetflixBlack
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_space),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                onQueryChange(searchQuery.dropLast(1))
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = NetflixDarkSurface,
                            contentColor = NetflixWhite,
                            focusedContainerColor = NetflixWhite,
                            focusedContentColor = NetflixBlack
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = { onQueryChange("") },
                        colors = ButtonDefaults.colors(
                            containerColor = NetflixDarkSurface,
                            contentColor = NetflixWhite,
                            focusedContainerColor = NetflixWhite,
                            focusedContentColor = NetflixBlack
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_clear),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            PosterCard(item = item, onClick = { onMediaSelected(item) }, cardWidth = null)
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
