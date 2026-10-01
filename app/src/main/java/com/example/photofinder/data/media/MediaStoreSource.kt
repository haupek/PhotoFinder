package com.example.photofinder.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.photofinder.data.db.PhotoEntity

/** Result of attempting to read GPS from a photo's EXIF data. */
sealed interface GeoReadResult {
    data class Found(val lat: Double, val lon: Double) : GeoReadResult

    /** Read succeeded, but the photo contains no GPS data. */
    data object None : GeoReadResult

    /** Could not access the original image (e.g. permission/access error) - retry later. */
    data object Unreadable : GeoReadResult
}

/**
 * Read-only access to the device photo collection via MediaStore. Reads ids,
 * names, capture dates, and - with ACCESS_MEDIA_LOCATION granted - GPS from EXIF.
 */
class MediaStoreSource(private val context: Context) {

    fun queryImages(): List<PhotoEntity> {
        val result = ArrayList<PhotoEntity>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "image_$id"
                val taken = cursor.getLong(takenCol)
                val added = cursor.getLong(addedCol)
                val dateMillis = if (taken > 0L) taken else added * 1000L
                val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null

                result.add(
                    PhotoEntity(
                        mediaId = id,
                        displayName = name,
                        dateTakenMillis = dateMillis,
                        ocrText = null,
                        ocrIndexed = false,
                        latitude = null,
                        longitude = null,
                        geoChecked = false,
                        bucketName = bucket
                    )
                )
            }
        }
        return result
    }

    fun uriFor(mediaId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)

    /**
     * Reads the GPS position from the image's EXIF data.
     *
     * Distinguishes "no GPS in the file" (None - definitive, do not retry) from
     * "could not access the original" (Unreadable - e.g. missing media-location
     * permission or only partial photo access, retry on a later run).
     */
    fun readGps(mediaId: Long): GeoReadResult {
        return try {
            val original = MediaStore.setRequireOriginal(uriFor(mediaId))
            val stream = context.contentResolver.openInputStream(original)
                ?: return GeoReadResult.Unreadable
            stream.use { input ->
                try {
                    val exif = ExifInterface(input)
                    val latLong = exif.latLong
                    if (latLong != null) {
                        GeoReadResult.Found(latLong[0], latLong[1])
                    } else {
                        GeoReadResult.None
                    }
                } catch (e: Exception) {
                    // Access worked but EXIF could not be parsed: treat as no GPS.
                    GeoReadResult.None
                }
            }
        } catch (e: Exception) {
            // setRequireOriginal / openInputStream failed: access/permission issue.
            GeoReadResult.Unreadable
        }
    }
}
