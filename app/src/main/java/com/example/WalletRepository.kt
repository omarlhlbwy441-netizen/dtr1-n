package com.example

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for managing user wallet balance, transaction logs, and withdrawal requests in Firestore
 */
class WalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Real-time stream of User Wallet Balance from `users/{uid}/wallet/balance`
     */
    fun observeWalletBalance(uid: String): Flow<WalletBalance> = callbackFlow {
        if (uid.isBlank()) {
            trySend(WalletBalance())
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(uid).collection("wallet").document("balance")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                trySend(WalletBalance.fromMap(snapshot.data!!))
            } else {
                // Return default demo starting balance if doc doesn't exist yet
                trySend(
                    WalletBalance(
                        availableBalance = 350.00,
                        pendingBalance = 75.50,
                        totalWithdrawn = 120.00,
                        currency = "USD"
                    )
                )
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of user transactions from `users/{uid}/transactions`
     */
    fun observeTransactions(uid: String): Flow<List<TransactionItem>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("users").document(uid)
            .collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If index missing or error, send initial sample list instead of crashing
                trySend(getSampleTransactions())
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { TransactionItem.fromMap(it) }
                }
                trySend(list)
            } else {
                trySend(getSampleTransactions())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Submit a withdrawal request and update pending/available balance in Firestore
     */
    suspend fun requestWithdrawal(
        uid: String,
        amount: Double,
        paymentMethod: String,
        accountDetails: String,
        currentBalance: WalletBalance
    ): Result<Unit> {
        return try {
            if (uid.isBlank()) return Result.failure(IllegalArgumentException("User UID cannot be empty"))
            if (amount <= 0) return Result.failure(IllegalArgumentException("المبلغ يجب أن يكون أكبر من 0"))
            if (amount > currentBalance.availableBalance) return Result.failure(IllegalArgumentException("رصيدك الحالي المتاح غير كافٍ لهذا الطلب"))

            val requestId = UUID.randomUUID().toString()
            val withdrawal = WithdrawalRequest(
                id = requestId,
                uid = uid,
                amount = amount,
                paymentMethod = paymentMethod,
                accountDetails = accountDetails,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            // Save withdrawal request doc
            firestore.collection("withdrawals").document(requestId)
                .set(withdrawal.toMap())
                .await()

            // Create pending transaction entry
            val transactionId = UUID.randomUUID().toString()
            val transaction = TransactionItem(
                id = transactionId,
                type = "WITHDRAWAL",
                title = "طلب سحب أرباح ($paymentMethod)",
                description = "تحويل إلى $accountDetails",
                amount = -amount,
                currency = currentBalance.currency,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .set(transaction.toMap())
                .await()

            // Update user balance doc
            val newBalance = currentBalance.copy(
                availableBalance = currentBalance.availableBalance - amount,
                pendingBalance = currentBalance.pendingBalance + amount,
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid)
                .collection("wallet").document("balance")
                .set(newBalance.toMap(), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add test earnings or bonus to Firestore
     */
    suspend fun addTestEarning(
        uid: String,
        amount: Double,
        title: String,
        description: String,
        currentBalance: WalletBalance
    ): Result<Unit> {
        return try {
            if (uid.isBlank()) return Result.failure(IllegalArgumentException("User UID empty"))

            val transactionId = UUID.randomUUID().toString()
            val transaction = TransactionItem(
                id = transactionId,
                type = "EARNING",
                title = title,
                description = description,
                amount = amount,
                currency = currentBalance.currency,
                status = "COMPLETED",
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .set(transaction.toMap())
                .await()

            val newBalance = currentBalance.copy(
                availableBalance = currentBalance.availableBalance + amount,
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid)
                .collection("wallet").document("balance")
                .set(newBalance.toMap(), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getSampleTransactions(): List<TransactionItem> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return listOf(
            TransactionItem(
                id = "tx_1",
                type = "EARNING",
                title = "أرباح مبيعات متجر الإلكترونيات",
                description = "عمولة بيع تطبيق المتجر الذكي",
                amount = 150.00,
                status = "COMPLETED",
                createdAt = now - (dayMs * 1)
            ),
            TransactionItem(
                id = "tx_2",
                type = "EARNING",
                title = "مكافأة مشاهدات رفيق شورتس",
                description = "حافز منشئي المحتوى للأسبوع الحالي",
                amount = 45.50,
                status = "COMPLETED",
                createdAt = now - (dayMs * 2)
            ),
            TransactionItem(
                id = "tx_3",
                type = "WITHDRAWAL",
                title = "طلب سحب أرباح إلى الحساب البنكي",
                description = "تحويل إلى بنك مصر ****4821",
                amount = -120.00,
                status = "COMPLETED",
                createdAt = now - (dayMs * 5)
            ),
            TransactionItem(
                id = "tx_4",
                type = "BONUS",
                title = "مكافأة انضمام VIP الرائعة",
                description = "هدية ترحيبية لمشتركي الباقة السنوية",
                amount = 25.00,
                status = "COMPLETED",
                createdAt = now - (dayMs * 8)
            )
        )
    }
}
