package com.trailguide.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reviews
 */
@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE trailId = :trailId ORDER BY createdAt DESC")
    fun getReviewsForTrail(trailId: String): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReviewsByUser(userId: String): Flow<List<ReviewEntity>>
    
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    suspend fun getAllReviews(): List<ReviewEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
    
    @Delete
    suspend fun deleteReview(review: ReviewEntity)
    
    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteReviewById(reviewId: String)
    
    @Query("SELECT AVG(rating) FROM reviews WHERE trailId = :trailId")
    suspend fun getAverageRating(trailId: String): Double?
    
    @Query("SELECT COUNT(*) FROM reviews WHERE trailId = :trailId")
    suspend fun getReviewCount(trailId: String): Int
}

