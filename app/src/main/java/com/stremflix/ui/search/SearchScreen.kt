package com.stremflix.ui.search

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stremflix.R
import com.stremflix.data.model.MediaItem
import com.stremflix.ui.home.PosterCard
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import com.stremflix.ui.theme.isCompactScreen

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

                // On-Screen TV Alphanumeric Keyboard Grid (6 columns)
                val tvKeys = listOf(
                    "A", "B", "C", "D", "E", "F",
                    "G", "H", "I", "J", "K", "L",
                    "M", "N", "O", "P", "Q", "R",
                    "S", "T", "U", "V", "W", "X",
                    "Y", "Z", "1", "2", "3", "4",
                    "5", "6", "7", "8", "9", "0"
                )

                // Fixed 6-column grid. This used to be a LazyVerticalGrid inside a
                // height-bounded Column, which clipped the bottom rows behind the
                // space/backspace/clear buttons on short TV viewports.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tvKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { charKey ->
                                var isKeyFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            if (isKeyFocused) Color.White.copy(alpha = 0.25f) else NetflixDarkSurface,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (isKeyFocused) 2.dp else 1.dp,
                                            color = if (isKeyFocused) NetflixBorderHighlight else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Space, Backspace, Clear buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var spaceFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { onQueryChange("$searchQuery ") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (spaceFocused) Color.White.copy(alpha = 0.25f) else NetflixDarkSurface,
                            contentColor = NetflixWhite
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .onFocusChanged { spaceFocused = it.isFocused }
                    ) {
                        Text(
                            text = stringResource(R.string.search_space),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var bkFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                onQueryChange(searchQuery.dropLast(1))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bkFocused) Color.White.copy(alpha = 0.25f) else NetflixDarkSurface,
                            contentColor = NetflixWhite
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .onFocusChanged { bkFocused = it.isFocused }
                    ) {
                        Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(16.dp))
                    }

                    var clearFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { onQueryChange("") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (clearFocused) Color.White.copy(alpha = 0.25f) else NetflixDarkSurface,
                            contentColor = NetflixWhite
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .onFocusChanged { clearFocused = it.isFocused }
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
