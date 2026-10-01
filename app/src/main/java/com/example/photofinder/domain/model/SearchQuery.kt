package com.example.photofinder.domain.model

/** How the location dimension constrains a search. */
enum class LocationMode {
    /** No location constraint. */
    NONE,

    /** Only photos that have a GPS position. */
    WITH,

    /** Only photos without a GPS position. */
    WITHOUT,

    /** Only photos within [SearchQuery.radiusKm] of the given center. */
    RADIUS
}

/**
 * One combined query across all search dimensions. Any field may be unset,
 * meaning "do not constrain on this dimension". Active constraints are ANDed.
 */
data class SearchQuery(
    val tag: String? = null,
    val text: String? = null,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val locationMode: LocationMode = LocationMode.NONE,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val radiusKm: Int? = null,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false
)
