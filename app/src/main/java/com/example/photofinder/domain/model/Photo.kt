package com.example.photofinder.domain.model

/**
 * A photo from the device gallery plus the search-relevant index data
 * (OCR text, tags, and optional GPS coordinates from EXIF).
 */
data class Photo(
    val mediaId: Long,
    val uri: String,
    val displayName: String,
    val dateTakenMillis: Long,
    val ocrText: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val folder: String? = null,
    val tags: List<String> = emptyList()
)
