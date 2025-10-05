package com.trailguide.android.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Review model for trail reviews with photos and comments
 */
@Parcelize
data class Review(
    val id: String,
    val trailId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val rating: Double,
    val comment: String,
    val photos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val likes: Int = 0
) : Parcelable

/**
 * Collection model for organizing favorite trails
 */
@Parcelize
data class TrailCollection(
    val id: String,
    val name: String,
    val description: String? = null,
    val trailIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val coverImage: String? = null
) : Parcelable

