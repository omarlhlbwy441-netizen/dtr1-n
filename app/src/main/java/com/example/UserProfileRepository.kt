package com.example

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for saving and retrieving user profile data from Cloud Firestore
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    /**
     * Store or update user profile data in Firestore
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            if (userProfile.uid.isBlank()) {
                return Result.failure(IllegalArgumentException("User UID cannot be blank"))
            }
            usersCollection.document(userProfile.uid)
                .set(userProfile.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update specific fields of a user profile in Firestore
     */
    suspend fun updateUserProfileFields(
        uid: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        return try {
            if (uid.isBlank()) {
                return Result.failure(IllegalArgumentException("User UID cannot be blank"))
            }
            val updatedMap = fields.toMutableMap()
            updatedMap["updatedAt"] = System.currentTimeMillis()
            usersCollection.document(uid)
                .set(updatedMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper method to update core profile fields (Name, Photo URL, Phone, Bio)
     */
    suspend fun updateProfile(
        uid: String,
        displayName: String? = null,
        photoUrl: String? = null,
        phone: String? = null,
        bio: String? = null
    ): Result<Unit> {
        val fields = mutableMapOf<String, Any>()
        displayName?.let { fields["displayName"] = it }
        photoUrl?.let { fields["photoUrl"] = it }
        phone?.let { fields["phone"] = it }
        bio?.let { fields["bio"] = it }
        return updateUserProfileFields(uid, fields)
    }

    /**
     * Activate or update VIP subscription status in Firestore
     */
    suspend fun updateVipSubscription(
        uid: String,
        isVip: Boolean,
        planName: String = "باقة VIP الذهبية",
        durationDays: Int = 30
    ): Result<Unit> {
        val expirationDate = if (isVip) {
            System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
        } else {
            0L
        }
        val fields = mapOf(
            "isVip" to isVip,
            "role" to if (isVip) "VIP" else "USER",
            "vipPlanName" to if (isVip) planName else "",
            "vipExpirationDate" to expirationDate
        )
        return updateUserProfileFields(uid, fields)
    }

    /**
     * Retrieve user profile data by UID
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            if (uid.isBlank()) {
                return Result.success(null)
            }
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists() && snapshot.data != null) {
                Result.success(UserProfile.fromMap(snapshot.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of user profile data
     */
    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                    trySend(UserProfile.fromMap(snapshot.data!!))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }
}
