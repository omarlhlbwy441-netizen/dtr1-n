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
