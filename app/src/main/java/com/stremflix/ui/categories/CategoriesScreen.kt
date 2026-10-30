package com.stremflix.ui.categories

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremflix.R
import com.stremflix.data.model.MediaItem
import com.stremflix.ui.components.TvSpinner
import com.stremflix.ui.home.PosterCard
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixMutedGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import com.stremflix.ui.theme.isCompactScreen

data class GenreItem(val id: Int, val name: String)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoriesScreen(
    genres: List<GenreItem>,
    selectedGenre: GenreItem?,
    onGenreSelected: (GenreItem) -> Unit,
    categoryItems: List<MediaItem>,
    isLoading: Boolean = false,
    onMediaSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMobile = isCompactScreen()

    if (isMobile) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.categories_title),
                color = NetflixWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Scrollable Genre Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(genres, key = { it.id }) { genre ->
                    val isSelected = selectedGenre?.id == genre.id
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) NetflixRed else NetflixDarkSurface,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onGenreSelected(genre) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genre.name,
                            color = NetflixWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    TvSpinner(color = NetflixRed, size = 44.dp)
                }
            } else if (categoryItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.categories_empty),
                        color = NetflixMutedGray,
                        fontSize = 15.sp
                    )
                }
            } else {
                val distinctCategoryItems = categoryItems.distinctBy { "${it.mediaType}_${it.id}" }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 115.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        distinctCategoryItems,
                        key = { _, item -> "${item.mediaType}_${item.id}" }) { _, item ->
                        PosterCard(item = item, onClick = { onMediaSelected(item) }, cardWidth = null)
                    }
                }
            }
        }
    } else {
        // TV 2-Pane Layout
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp)
        ) {
            // Left Category Navigation List
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(R.string.categories_title),
                    color = NetflixWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(genres, key = { it.id }) { genre ->
                        val isSelected = selectedGenre?.id == genre.id
                        var isFocused by remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    if (isFocused) Color.White.copy(alpha = 0.18f) else if (isSelected) NetflixCardSurface else Color.Transparent,
                                    RoundedCornerShape(NetflixDimens.CardRadius)
                                )
                                .border(
                                    width = if (isFocused) 2.dp else 0.dp,
                                    color = if (isFocused) NetflixBorderHighlight else Color.Transparent,
                                    shape = RoundedCornerShape(NetflixDimens.CardRadius)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable { onGenreSelected(genre) }
                                .padding(horizontal = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .background(if (isSelected) NetflixRed else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = genre.name,
                                color = if (isSelected || isFocused) NetflixWhite else NetflixLightGray,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Right Content Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                selectedGenre?.let { genre ->
                    Text(
                        text = genre.name,
                        color = NetflixWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TvSpinner(color = NetflixRed, size = 44.dp)
                    }
                } else if (categoryItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.categories_empty),
                            color = NetflixMutedGray,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    val distinctCategoryItems = categoryItems.distinctBy { "${it.mediaType}_${it.id}" }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            distinctCategoryItems,
                            key = { _, item -> "${item.mediaType}_${item.id}" }) { _, item ->
                            PosterCard(item = item, onClick = { onMediaSelected(item) }, cardWidth = null)
                        }
                    }
                }
            }
        }
    }
}
