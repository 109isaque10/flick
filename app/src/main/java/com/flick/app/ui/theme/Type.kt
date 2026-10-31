package com.flick.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flick.app.R

val NetflixSans = FontFamily(
    Font(R.font.netflix_sans_light, FontWeight.Light),
    Font(R.font.netflix_sans_medium, FontWeight.Medium),
    Font(R.font.netflix_sans_bold, FontWeight.Bold),
    Font(R.font.netflix_sans_medium, FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NetflixSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        color = NetflixWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = NetflixSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = NetflixWhite
    ),
    titleLarge = TextStyle(
        fontFamily = NetflixSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = NetflixWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = NetflixSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
        color = NetflixLightGray
    ),
    labelMedium = TextStyle(
        fontFamily = NetflixSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = NetflixWhite
    )
)

