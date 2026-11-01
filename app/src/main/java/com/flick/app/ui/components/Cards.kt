package com.flick.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.flick.app.R
import com.flick.app.data.model.MediaItem
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.theme.NetflixCardSurface
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite

/**
 * Canonical Netflix-2020 card library.
 *
 * Every grid/badge/button in the app should come from here so focus frames,
 * corner radii, poster sizes and progress meters cannot drift between Home,
 * Search, Categories, Detail and the player.
 *
 * Geometry rules that MUST be preserved by anything added to this file:
 *
 *  1. The focus target is the OUTER [Box]; its layout bounds never change. The
 *     focus scale lives on an INNER [graphicsLayer]. Scaling the focus node's
 *     own bounds made the scroll container re-centre by a few pixels on every
 *     Left/Right, which read as the whole screen jittering vertically (N8).
 *  2. Borders are draw-phase only and are constant-width containers, so they
 *     never trigger a parent re-measure either.
 *  3. The bottom contrast gradient is emitted only when there is bottom text to
 *     keep legible (the rank). Cards that only carry a top badge do not get it
 *     (H1) — the last row of a shelf used to pick up a needless dark overlay.
 */

// ---------------------------------------------------------------------------
// 2:3 poster card — the workhorse for every poster shelf and results grid.
// ---------------------------------------------------------------------------

@Composable
fun NetflixPosterCard(
    posterUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = NetflixDimens.PosterWidthTv,
    rank: Int? = null,
    originalBadge: Boolean = false,
    progress: Float = 0f,
    scaleOnFocus: Boolean = true,
    cornerRadius: Dp = NetflixDimens.CardRadius
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && scaleOnFocus) NetflixDimens.CardFocusScale else 1f,
        // Short tween instead of the default spring: the spring's ~400ms settle
        // read as sluggish focus movement on a low-RAM TV box.
        animationSpec = tween(durationMillis = 120),
        label = "posterScale"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .then(if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
            .aspectRatio(2f / 3f)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(NetflixCardSurface)
                .border(
                    width = if (isFocused) NetflixDimens.CardFocusBorder else 0.dp,
                    color = if (isFocused) NetflixWhite else Color.Transparent,
                    shape = shape
                )
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (rank != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    NetflixBlack.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }

            if (originalBadge) {
                Image(
                    painter = painterResource(R.drawable.logo_flick),
                    contentDescription = stringResource(R.string.label_series),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 6.dp, top = 4.dp)
                        .size(16.dp)
                )
            }

            if (rank != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (rank <= 10) R.string.badge_top_10 else R.string.badge_top_250
                        ),
                        color = NetflixWhite.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "$rank",
                        color = NetflixWhite,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 42.sp
                    )
                }
            }

            if (progress > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NetflixLightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(NetflixRed)
                    )
                }
            }
        }
    }
}

/**
 * Results-grid poster: identical to [NetflixPosterCard] but with zero focus
 * scale, matching the reference layout for the details/collections and search
 * result grids (a grid of scaling tiles reads as noise, not as focus).
 */
@Composable
fun NetflixCleanPosterCard(
    posterUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterWidth: Dp? = NetflixDimens.PosterWidthTv
) = NetflixPosterCard(
    posterUrl = posterUrl,
    title = title,
    onClick = onClick,
    modifier = modifier,
    cardWidth = posterWidth,
    scaleOnFocus = false,
    cornerRadius = 2.dp
)

/** MediaItem convenience wrapper around [NetflixPosterCard]. */
@Composable
fun NetflixSelectionCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = NetflixDimens.PosterWidthTv
) = NetflixPosterCard(
    posterUrl = item.fullPosterUrl,
    title = item.title,
    onClick = onClick,
    modifier = modifier,
    cardWidth = cardWidth
)

// ---------------------------------------------------------------------------
// Episode still (16:9) with the S1: E1 badge and the red progress strip.
// ---------------------------------------------------------------------------

@Composable
fun NetflixEpisodeThumbnailCard(
    stillUrl: String?,
    title: String,
    seasonNumber: Int,
    episodeNumber: Int,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    thumbnailWidth: Dp? = 168.dp,
    focused: Boolean? = null,
    showEpisodeBadge: Boolean = true,
    showPlayIconOnFocus: Boolean = true,
    showFocusBorder: Boolean = true,
    cornerRadius: Dp = NetflixDimens.ThumbnailRadius
) {
    // When the caller owns focus (an enclosing row is the focus target) it passes
    // `focused` and this local state stays false, so the two never fight.
    var ownsFocus by remember { mutableStateOf(false) }
    val isFocused = focused ?: ownsFocus
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .then(if (thumbnailWidth != null) Modifier.width(thumbnailWidth) else Modifier.fillMaxWidth())
            .aspectRatio(16f / 9f)
            .then(
                if (onClick != null) {
                    Modifier
                        .onFocusChanged { ownsFocus = it.isFocused }
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(NetflixCardSurface)
                .border(
                    width = if (isFocused && showFocusBorder) NetflixDimens.CardFocusBorder else 0.dp,
                    color = if (isFocused && showFocusBorder) NetflixWhite else Color.Transparent,
                    shape = shape
                )
        ) {
            // Untinted episode still.
            AsyncImage(
                model = stillUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Netflix only reveals the play affordance on the focused item.
            if (showPlayIconOnFocus && isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(NetflixBlack.copy(alpha = 0.62f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NetflixWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showEpisodeBadge) {
                Text(
                    text = "S$seasonNumber: E$episodeNumber",
                    color = NetflixWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp)
                        .background(NetflixBlack.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (progress > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NetflixLightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(NetflixRed)
                    )
                }
            }
        }
    }
}

/** Generic 16:9 still with no episode badge, for trailer tiles and shelves. */
@Composable
fun NetflixThumbnailCard(
    stillUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    cardWidth: Dp? = 168.dp
) = NetflixEpisodeThumbnailCard(
    stillUrl = stillUrl,
    title = title,
    seasonNumber = 0,
    episodeNumber = 0,
    progress = progress,
    modifier = modifier,
    onClick = onClick,
    thumbnailWidth = cardWidth,
    showEpisodeBadge = false
)

// ---------------------------------------------------------------------------
// Selection button — the details/player action rows.
// ---------------------------------------------------------------------------

/**
 * One uniform selection box for the details action stack.
 *
 * Every action uses the same shape: transparent when idle,
 * `White.copy(alpha = 0.15f)` with a crisp 2dp white border when focused,
 * fixed 48dp height and a zero scale transform. The Play action carries the
 * watch-progress meter pinned to the box's right edge; width is owned entirely
 * by the caller so every button in the stack lines up.
 */
@Composable
fun NetflixSelectionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    progress: Float = 0f
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(2.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (isFocused) NetflixWhite.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NetflixWhite,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = label,
            color = NetflixWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (progress > 0f) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NetflixWhite.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(NetflixRed)
                )
            }
        }
    }
}


/* ---------------------------------------------------------------------------
 * Mobile (touch) controls.
 *
 * Phone screens must not use `androidx.tv.material3` Button/OutlinedButton: those
 * are 10-foot components whose visuals (focus scale, focus container colour) and
 * interaction model assume D-pad focus, and they only respond to touch when every
 * call site wraps them in `touchClickable`. These foundation-based controls are
 * plain `clickable` boxes, so they render and behave correctly on a phone with no
 * per-call-site workaround, and still respond to D-pad if one is present.
 *
 * The card-library geometry rules still apply: the focusable box keeps constant
 * layout bounds and the progress meter is a draw-time overlay, never a layout change.
 * ------------------------------------------------------------------------- */

@Composable
fun MobilePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.PlayArrow,
    progress: Float = 0f,
    height: Dp = 42.dp,
    radius: Dp = 4.dp
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(NetflixWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // Resume meter pinned to the bottom edge, as on the classic details page.
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(NetflixLightGray.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(NetflixRed)
                )
            }
        }
    }
}

@Composable
fun MobileSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 42.dp,
    radius: Dp = 4.dp
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(NetflixDarkSurface.copy(alpha = 0.7f))
            .border(1.dp, NetflixWhite.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NetflixWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = NetflixWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Icon-over-label action used in the phone hero bar (My List / Info). */
@Composable
fun MobileHeroActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NetflixWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = NetflixWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
