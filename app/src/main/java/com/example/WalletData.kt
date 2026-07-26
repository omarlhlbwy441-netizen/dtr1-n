package com.example

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Data model for User Wallet Balance in Firestore
 */
@IgnoreExtraProperties
data class WalletBalance(
    @get:PropertyName("availableBalance") @set:PropertyName("availableBalance") var availableBalance: Double = 0.0,
    @get:PropertyName("pendingBalance") @set:PropertyName("pendingBalance") var pendingBalance: Double = 0.0,
    @get:PropertyName("totalWithdrawn") @set:PropertyName("totalWithdrawn") var totalWithdrawn: Double = 0.0,
    @get:PropertyName("currency") @set:PropertyName("currency") var currency: String = "USD",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "availableBalance" to availableBalance,
            "pendingBalance" to pendingBalance,
            "totalWithdrawn" to totalWithdrawn,
            "currency" to currency,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): WalletBalance {
            return WalletBalance(
                availableBalance = (map["availableBalance"] as? Number)?.toDouble() ?: 0.0,
                pendingBalance = (map["pendingBalance"] as? Number)?.toDouble() ?: 0.0,
                totalWithdrawn = (map["totalWithdrawn"] as? Number)?.toDouble() ?: 0.0,
                currency = map["currency"] as? String ?: "USD",
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data model for Wallet Transactions in Firestore
 */
@IgnoreExtraProperties
data class TransactionItem(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "EARNING", // "EARNING", "WITHDRAWAL", "BONUS", "REFUND"
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("currency") @set:PropertyName("currency") var currency: String = "USD",
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "COMPLETED", // "COMPLETED", "PENDING", "FAILED"
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "type" to type,
            "title" to title,
            "description" to description,
            "amount" to amount,
            "currency" to currency,
            "status" to status,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): TransactionItem {
            return TransactionItem(
                id = map["id"] as? String ?: "",
                type = map["type"] as? String ?: "EARNING",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                currency = map["currency"] as? String ?: "USD",
                status = map["status"] as? String ?: "COMPLETED",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data model for Withdrawal Request in Firestore
 */
@IgnoreExtraProperties
data class WithdrawalRequest(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod") var paymentMethod: String = "", // "BANK", "VODAFONE_CASH", "PAYPAL", "CRYPTO"
    @get:PropertyName("accountDetails") @set:PropertyName("accountDetails") var accountDetails: String = "",
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "uid" to uid,
            "amount" to amount,
            "paymentMethod" to paymentMethod,
            "accountDetails" to accountDetails,
            "status" to status,
            "createdAt" to createdAt
        )
    }
}
