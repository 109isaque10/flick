package com.stremflix.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StremioStreamResponse(
    @Json(name = "streams") val streams: List<StremioStream>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioStream(
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "externalUrl") val externalUrl: String? = null,
    @Json(name = "behaviorHints") val behaviorHints: StremioBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class StremioBehaviorHints(
    @Json(name = "videoID") val videoID: String? = null,
    @Json(name = "bingeGroup") val bingeGroup: String? = null,
    @Json(name = "filename") val filename: String? = null
)
