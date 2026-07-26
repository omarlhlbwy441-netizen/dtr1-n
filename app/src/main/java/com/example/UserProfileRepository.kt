package com.example

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Data model representing a User Profile in Firestore
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phone: String = "",
    val bio: String = "",
    val role: String = "USER", // "USER", "MERCHANT", "VIP"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "photoUrl" to photoUrl,
            "phone" to phone,
            "bio" to bio,
            "role" to role,
            "createdAt" to createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                bio = map["bio"] as? String ?: "",
                role = map["role"] as? String ?: "USER",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

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
