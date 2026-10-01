package com.example.photofinder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewPhotos(photos: List<PhotoEntity>)

    // --- OCR indexing ---

    @Query("SELECT mediaId FROM photos WHERE ocrIndexed = 0 LIMIT :limit")
    suspend fun unindexedIds(limit: Int): List<Long>

    @Query("UPDATE photos SET ocrText = :text, ocrIndexed = 1 WHERE mediaId = :mediaId")
    suspend fun storeOcrResult(mediaId: Long, text: String?)

    @Query("SELECT COUNT(*) FROM photos WHERE ocrIndexed = 0")
    suspend fun pendingOcrCount(): Int

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE ocrIndexed = 1")
    suspend fun indexedCount(): Int

    // --- GPS indexing ---

    @Query("SELECT mediaId FROM photos WHERE geoChecked = 0 LIMIT :limit")
    suspend fun geoUncheckedIds(limit: Int): List<Long>

    @Query("UPDATE photos SET latitude = :lat, longitude = :lon, geoChecked = 1 WHERE mediaId = :mediaId")
    suspend fun storeGeo(mediaId: Long, lat: Double?, lon: Double?)

    @Query("SELECT COUNT(*) FROM photos WHERE geoChecked = 0")
    suspend fun geoPendingCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE latitude IS NOT NULL")
    suspend fun coordinatesCount(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE bucketName IS NULL")
    suspend fun missingBucketCount(): Int

    @Query("UPDATE photos SET bucketName = :bucket WHERE mediaId = :mediaId AND bucketName IS NULL")
    suspend fun storeBucketIfMissing(mediaId: Long, bucket: String?)

    @Transaction
    suspend fun backfillBuckets(photos: List<PhotoEntity>) {
        for (photo in photos) {
            storeBucketIfMissing(photo.mediaId, photo.bucketName)
        }
    }

    /** Forces all photos to be re-read for GPS on the next indexing run. */
    @Query("UPDATE photos SET geoChecked = 0, latitude = NULL, longitude = NULL")
    suspend fun resetGeoChecked()

    // --- Tags ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE mediaId = :mediaId AND tagLower = :tagLower")
    suspend fun removeTag(mediaId: Long, tagLower: String)

    @Query("SELECT tag FROM tags WHERE mediaId = :mediaId ORDER BY tag")
    suspend fun tagsForPhoto(mediaId: Long): List<String>

    @Query("DELETE FROM photos WHERE mediaId = :mediaId")
    suspend fun deletePhoto(mediaId: Long)

    @Query("DELETE FROM tags WHERE mediaId = :mediaId")
    suspend fun deleteTagsForPhoto(mediaId: Long)

    @Query("DELETE FROM photos WHERE mediaId IN (:ids)")
    suspend fun deletePhotos(ids: List<Long>)

    @Query("DELETE FROM tags WHERE mediaId IN (:ids)")
    suspend fun deleteTagsForPhotos(ids: List<Long>)

    @Query("SELECT DISTINCT tag FROM tags ORDER BY tag")
    suspend fun allTagSuggestions(): List<String>

    // --- Combined search ---
    @Query(
        """
        SELECT DISTINCT p.* FROM photos p
        LEFT JOIN tags t ON t.mediaId = p.mediaId
        WHERE (:hasTag = 0 OR t.tagLower LIKE '%' || :tag || '%')
          AND (:hasText = 0 OR LOWER(p.ocrText) LIKE '%' || :text || '%')
          AND (:fromMillis = 0 OR p.dateTakenMillis >= :fromMillis)
          AND (:toMillis = 0 OR p.dateTakenMillis <= :toMillis)
          AND (:locMode = 0
               OR (:locMode = 1 AND p.latitude IS NOT NULL)
               OR (:locMode = 2 AND p.latitude IS NULL))
          AND (:useBbox = 0
               OR (p.latitude IS NOT NULL AND p.longitude IS NOT NULL
                   AND p.latitude BETWEEN :minLat AND :maxLat
                   AND p.longitude BETWEEN :minLon AND :maxLon))
        ORDER BY p.dateTakenMillis DESC
        """
    )
    suspend fun search(
        hasTag: Int,
        tag: String,
        hasText: Int,
        text: String,
        fromMillis: Long,
        toMillis: Long,
        locMode: Int,
        useBbox: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<PhotoEntity>
}
