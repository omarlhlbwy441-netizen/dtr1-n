package com.example

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for managing Live Broadcast streams & comments in Firestore (`live_streams` collection)
 */
class LiveStreamRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Listen to active live streams in real-time
     */
    fun observeLiveStreams(): Flow<List<LiveStream>> = callbackFlow {
        val listener = firestore.collection("live_streams")
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(getSampleLiveStreams())
                    return@addSnapshotListener
                }

                val streams = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { LiveStream.fromMap(doc.id, it) }
                }
                trySend(streams)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Listen to comments for a specific live stream session
     */
    fun observeComments(streamId: String): Flow<List<LiveComment>> = callbackFlow {
        val listener = firestore.collection("live_streams")
            .document(streamId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(getSampleComments())
                    return@addSnapshotListener
                }

                val comments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LiveComment::class.java)
                }
                trySend(comments)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Start a new live stream broadcast session
     */
    suspend fun createLiveStream(stream: LiveStream): Result<String> {
        return try {
            val docId = if (stream.id.isBlank()) UUID.randomUUID().toString() else stream.id
            val finalStream = stream.copy(id = docId, isLive = true, startedAt = System.currentTimeMillis())
            firestore.collection("live_streams")
                .document(docId)
                .set(finalStream.toMap())
                .await()
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End live broadcast session
     */
    suspend fun endLiveStream(streamId: String): Result<Unit> {
        return try {
            firestore.collection("live_streams")
                .document(streamId)
                .update("isLive", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add comment to live stream
     */
    suspend fun sendComment(streamId: String, comment: LiveComment): Result<Unit> {
        return try {
            val commentId = UUID.randomUUID().toString()
            val commentWithId = comment.copy(id = commentId, timestamp = System.currentTimeMillis())
            firestore.collection("live_streams")
                .document(streamId)
                .collection("comments")
                .document(commentId)
                .set(commentWithId)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increment like count atomically
     */
    suspend fun sendLike(streamId: String): Result<Unit> {
        return try {
            firestore.collection("live_streams")
                .document(streamId)
                .update("likesCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSampleLiveStreams(): List<LiveStream> {
        return listOf(
            LiveStream(
                id = "live_1",
                title = "🔥 إطلاق عروض وتخفيضات رفيق VIP الحصرية!",
                hostName = "الشيخ خالد العتيبي VIP",
                category = "تسوق مباشر",
                viewerCount = 1840,
                likesCount = 5200,
                isLive = true,
                featuredProductName = "ساعة الرفيق الذكية Pro Max",
                featuredProductPrice = 499.0
            ),
            LiveStream(
                id = "live_2",
                title = "استعراض منتجات العطور والبخور الملكي 🌸",
                hostName = "بوتيك العبير الشهير",
                category = "عطور ومستحضرات",
                viewerCount = 920,
                likesCount = 2100,
                isLive = true,
                featuredProductName = "عطر اللافندر والمسك الملكي 100ml",
                featuredProductPrice = 180.0
            )
        )
    }

    fun getSampleComments(): List<LiveComment> {
        val now = System.currentTimeMillis()
        return listOf(
            LiveComment("c1", "سارة الشمري", "", "ما شاء الله المنتج رائع جداً! 😍", isVip = true, timestamp = now - 20000),
            LiveComment("c2", "فهد الدوسري", "", "كم السعر مع التوصيل للرياض؟ 🚚", isVip = false, timestamp = now - 15000),
            LiveComment("c3", "منى القحطاني", "", "تم الطلب الآن من المنتج المثبت! 👍", isVip = true, giftType = "❤️", timestamp = now - 10000),
            LiveComment("c4", "عبدالله العنزي", "", "بث ممتاز واصلوا بالتوفيق 👏✨", isVip = false, giftType = "🔥", timestamp = now - 5000)
        )
    }
}
