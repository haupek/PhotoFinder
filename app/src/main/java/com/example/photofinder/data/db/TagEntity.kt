package com.example.photofinder.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * A user-assigned keyword for a photo. A photo can carry many tags.
 * tagLower stores the lower-cased value for case-insensitive search.
 */
@Entity(
    tableName = "tags",
    primaryKeys = ["mediaId", "tagLower"],
    indices = [Index(value = ["tagLower"])]
)
data class TagEntity(
    val mediaId: Long,
    val tag: String,
    val tagLower: String
)
