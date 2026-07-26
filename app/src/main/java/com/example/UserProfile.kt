package com.example

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Data model representing a User Profile in Firestore.
 * Contains core user details like Name, Email, Profile Picture URL, Phone Number, Bio, Role, and Timestamps.
 */
@IgnoreExtraProperties
data class UserProfile(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("photoUrl") @set:PropertyName("photoUrl") var photoUrl: String = "",
    @get:PropertyName("phone") @set:PropertyName("phone") var phone: String = "",
    @get:PropertyName("bio") @set:PropertyName("bio") var bio: String = "",
    @get:PropertyName("role") @set:PropertyName("role") var role: String = "USER", // "USER", "MERCHANT", "VIP"
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long = System.currentTimeMillis()
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
