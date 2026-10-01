package com.example.photofinder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local index row for one MediaStore image. The image bytes are never copied;
 * only metadata, recognised OCR text and optional GPS coordinates are stored.
 *
 * ocrIndexed and geoChecked are independent flags so OCR (slow) and GPS reading
 * (fast) can progress separately and be backfilled independently.
 */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val mediaId: Long,
    val displayName: String,
    val dateTakenMillis: Long,
    val ocrText: String?,
    val ocrIndexed: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geoChecked: Boolean = false,
    val bucketName: String? = null
)
