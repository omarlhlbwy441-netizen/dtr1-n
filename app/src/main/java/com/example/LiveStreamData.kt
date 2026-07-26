package com.example

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Data model for Live Stream session
 */
@IgnoreExtraProperties
data class LiveStream(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("hostName") @set:PropertyName("hostName") var hostName: String = "المدرب أحمد - VIP",
    @get:PropertyName("hostAvatar") @set:PropertyName("hostAvatar") var hostAvatar: String = "",
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "تسوق مباشر",
    @get:PropertyName("viewerCount") @set:PropertyName("viewerCount") var viewerCount: Int = 1240,
    @get:PropertyName("likesCount") @set:PropertyName("likesCount") var likesCount: Int = 3400,
    @get:PropertyName("isLive") @set:PropertyName("isLive") var isLive: Boolean = false,
    @get:PropertyName("featuredProductId") @set:PropertyName("featuredProductId") var featuredProductId: String = "",
    @get:PropertyName("featuredProductName") @set:PropertyName("featuredProductName") var featuredProductName: String = "خنجر الرفيق الملكي الأصيل",
    @get:PropertyName("featuredProductPrice") @set:PropertyName("featuredProductPrice") var featuredProductPrice: Double = 350.0,
    @get:PropertyName("startedAt") @set:PropertyName("startedAt") var startedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "hostName" to hostName,
            "hostAvatar" to hostAvatar,
            "category" to category,
            "viewerCount" to viewerCount,
            "likesCount" to likesCount,
            "isLive" to isLive,
            "featuredProductId" to featuredProductId,
            "featuredProductName" to featuredProductName,
            "featuredProductPrice" to featuredProductPrice,
            "startedAt" to startedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): LiveStream {
            return LiveStream(
                id = id.ifBlank { map["id"] as? String ?: "" },
                title = map["title"] as? String ?: "بث مباشر تجريبي",
                hostName = map["hostName"] as? String ?: "منشئ محتوى VIP",
                hostAvatar = map["hostAvatar"] as? String ?: "",
                category = map["category"] as? String ?: "تسوق مباشر",
                viewerCount = (map["viewerCount"] as? Number)?.toInt() ?: 1200,
                likesCount = (map["likesCount"] as? Number)?.toInt() ?: 3500,
                isLive = map["isLive"] as? Boolean ?: true,
                featuredProductId = map["featuredProductId"] as? String ?: "",
                featuredProductName = map["featuredProductName"] as? String ?: "منتج مميز",
                featuredProductPrice = (map["featuredProductPrice"] as? Number)?.toDouble() ?: 299.0,
                startedAt = (map["startedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data model for Live Comments
 */
@IgnoreExtraProperties
data class LiveComment(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName") var senderName: String = "",
    @get:PropertyName("senderAvatar") @set:PropertyName("senderAvatar") var senderAvatar: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("isVip") @set:PropertyName("isVip") var isVip: Boolean = false,
    @get:PropertyName("giftType") @set:PropertyName("giftType") var giftType: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis()
)
