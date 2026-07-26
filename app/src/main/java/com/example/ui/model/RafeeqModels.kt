package com.example.ui.model

data class ShortVideoItem(
    val id: String,
    val creatorName: String,
    val creatorHandle: String,
    val description: String,
    val views: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean = false,
    val productName: String,
    val productPrice: String,
    val commissionRate: String,
    val commissionAmount: String,
    val giftCount: Int = 12
)

data class LiveAuctionItem(
    val id: String,
    val itemTitle: String,
    val streamerName: String,
    val currentBidSar: Int,
    val highestBidder: String,
    val timeRemainingSeconds: Int,
    val isLiveNow: Boolean = true
)

data class StoreSlotItem(
    val id: Int,
    val code: String,
    val name: String,
    val category: String,
    val slotsCount: Int,
    val fee: String,
    val status: String
)

data class WalletTransaction(
    val id: String,
    val type: String, // Affiliate, Auction, Shorts, Payout
    val amountSar: Double,
    val date: String,
    val status: String
)

data class AiChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String = "الآن"
)
