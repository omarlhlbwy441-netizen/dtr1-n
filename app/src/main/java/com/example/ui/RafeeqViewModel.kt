package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Booking
import com.example.data.BookingRepository
import com.example.ui.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RafeeqUiState(
    val selectedTab: Int = 0, // 0: Shorts, 1: Live & Auctions, 2: Wallet, 3: VIP, 4: Travel Bookings
    val selectedBottomNav: Int = 2, // 0: Inventory, 1: Stores, 2: Shorts Feed, 3: Sites/Kernel, 4: Network
    val totalEarningsSar: Double = 4250.0,
    val totalInteractions: String = "182.4K",
    val affiliateCommissionsSar: Double = 1820.0,
    val walletBalanceSar: Double = 1240.0,
    val shortsList: List<ShortVideoItem> = listOf(
        ShortVideoItem(
            id = "s1",
            creatorName = "عمر الهلباوي",
            creatorHandle = "@omarlhlbwy",
            description = "عرض مميز لخنجر الرفيق الملكي المصنوع يدويًا ✨ شاهد التفاصيل الدقيقة واستمتع بالجودة العالية في التصميم والتصنيع!",
            views = "45.2K",
            likesCount = 1420,
            commentsCount = 88,
            isLiked = false,
            productName = "خنجر الرفيق الملكي الأصلي",
            productPrice = "350 SAR",
            commissionRate = "15%",
            commissionAmount = "52.5 SAR"
        ),
        ShortVideoItem(
            id = "s2",
            creatorName = "نادي صناع VIP",
            creatorHandle = "@vip_creators",
            description = "مراجعة شاشة القيادة الرقمية لنظام رفيق v3.2.0 - سرعة المزامنة فائقة وأداء النواة متكامل 🚀",
            views = "92.1K",
            likesCount = 3850,
            commentsCount = 210,
            isLiked = true,
            productName = "حزمة التسويق الفاخرة VIP",
            productPrice = "1,200 SAR",
            commissionRate = "20%",
            commissionAmount = "240 SAR"
        )
    ),
    val currentShortIndex: Int = 0,
    val liveAuctions: List<LiveAuctionItem> = listOf(
        LiveAuctionItem(
            id = "a1",
            itemTitle = "ساعة الرفيق الكرونوغراف الذهبية (الإصدار الملكي #01)",
            streamerName = "مزادات رفيق المباشرة",
            currentBidSar = 2850,
            highestBidder = "سليمان القحطاني",
            timeRemainingSeconds = 345,
            isLiveNow = true
        ),
        LiveAuctionItem(
            id = "a2",
            itemTitle = "لوحة ترخيص رقمية حصرية (REF-999)",
            streamerName = "نادي المقتنيات النادرة",
            currentBidSar = 5400,
            highestBidder = "عبدالله الشهري",
            timeRemainingSeconds = 890,
            isLiveNow = true
        )
    ),
    val storeSlots: List<StoreSlotItem> = listOf(
        StoreSlotItem(1, "SLOT-001", "متجر الفخامة السعودية", "العطور والجمال", 3, "Free VIP", "نشط"),
        StoreSlotItem(2, "SLOT-002", "معرض المقتنيات الملكية", "المزادات والتحف", 5, "10% Commission", "مستقر"),
        StoreSlotItem(3, "SLOT-003", "أكاديمية صناع المحتوى", "دورات ومنتجات رقمية", 2, "Free VIP", "نشط")
    ),
    val walletTransactions: List<WalletTransaction> = listOf(
        WalletTransaction("t1", "عمولة شورتس", 52.5, "2026-07-25 12:30", "مكتمل"),
        WalletTransaction("t2", "مزايدة ناجحة", 350.0, "2026-07-24 18:10", "مكتمل"),
        WalletTransaction("t3", "سحب إلى STC Pay", -500.0, "2026-07-23 09:15", "معالج")
    ),
    val savedTravelBookings: List<Booking> = emptyList(),
    val isAiAssistantOpen: Boolean = false,
    val aiMessages: List<AiChatMessage> = listOf(
        AiChatMessage("أهلاً بك في رفيق الذكي! كيف يمكنني مساعدتك اليوم في إدارة مبيعاتك وعمولاتك؟", isFromUser = false)
    ),
    val userNotificationMessage: String? = null
)

class RafeeqViewModel(application: Application) : AndroidViewModel(application) {
    private val travelRepository: BookingRepository

    private val _uiState = MutableStateFlow(RafeeqUiState())
    val uiState: StateFlow<RafeeqUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        travelRepository = BookingRepository(database.bookingDao())

        // Collect Room Database Flow reactively
        viewModelScope.launch {
            travelRepository.allBookings.collect { bookings ->
                _uiState.update { it.copy(savedTravelBookings = bookings) }
            }
        }
    }

    fun saveTravelBooking(booking: Booking) {
        viewModelScope.launch {
            travelRepository.saveBooking(booking)
            _uiState.update {
                it.copy(userNotificationMessage = "تم حفظ الحجز بنجاح في قاعدة البيانات Room 💾!")
            }
        }
    }

    fun deleteTravelBooking(id: Long) {
        viewModelScope.launch {
            travelRepository.deleteBooking(id)
            _uiState.update {
                it.copy(userNotificationMessage = "تم حذف الحجز من قاعدة البيانات Room.")
            }
        }
    }

    fun selectTopTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectBottomNav(index: Int) {
        _uiState.update { it.copy(selectedBottomNav = index) }
    }

    fun toggleLikeShort(id: String) {
        _uiState.update { state ->
            val updatedShorts = state.shortsList.map { short ->
                if (short.id == id) {
                    val newLiked = !short.isLiked
                    val newLikes = if (newLiked) short.likesCount + 1 else short.likesCount - 1
                    short.copy(isLiked = newLiked, likesCount = newLikes)
                } else short
            }
            state.copy(shortsList = updatedShorts)
        }
    }

    fun placeBidOnAuction(auctionId: String, incrementSar: Int) {
        _uiState.update { state ->
            val updatedAuctions = state.liveAuctions.map { auction ->
                if (auction.id == auctionId) {
                    val newBid = auction.currentBidSar + incrementSar
                    auction.copy(currentBidSar = newBid, highestBidder = "أنت (صانع VIP)")
                } else auction
            }
            state.copy(
                liveAuctions = updatedAuctions,
                userNotificationMessage = "تمت المزايدة بنجاح بقيمة $incrementSar SAR!"
            )
        }
    }

    fun requestPayout(amountSar: Double, method: String) {
        if (amountSar <= 0 || amountSar > _uiState.value.walletBalanceSar) {
            _uiState.update { it.copy(userNotificationMessage = "الرصيد المتاح غير كافٍ للسحب!") }
            return
        }
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            type = "سحب عبر $method",
            amountSar = -amountSar,
            date = "2026-07-25 الآن",
            status = "قيد التنفيذ"
        )
        _uiState.update { state ->
            state.copy(
                walletBalanceSar = state.walletBalanceSar - amountSar,
                walletTransactions = listOf(newTx) + state.walletTransactions,
                userNotificationMessage = "تم تقديم طلب سحب $amountSar SAR إلى $method بنجاح!"
            )
        }
    }

    fun toggleAiAssistant(open: Boolean? = null) {
        _uiState.update { state ->
            state.copy(isAiAssistantOpen = open ?: !state.isAiAssistantOpen)
        }
    }

    fun sendAiMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiChatMessage(prompt, isFromUser = true)
        _uiState.update { state ->
            state.copy(aiMessages = state.aiMessages + userMsg)
        }
        viewModelScope.launch {
            val replyText = when {
                prompt.contains("عمول") || prompt.contains("أرباح") ->
                    "إجمالي أرباحك الحالية هو 4,250 SAR. يمكنك زيادة نسبة عمولتك إلى 25% بالترقية في نادي VIP صناع المحتوى."
                prompt.contains("مزاد") || prompt.contains("بث") ->
                    "المزاد النشط الآن على ساعة الرفيق الملكية وصل إلى ${_uiState.value.liveAuctions.firstOrNull()?.currentBidSar ?: 2850} SAR. يُنصح بزيادة المزايدة في الدقائق الأخيرة."
                prompt.contains("حجز") || prompt.contains("سفر") || prompt.contains("طيران") ->
                    "تم توفير نظام حجوزات وكالة الذئب الرقمي! يمكنك استخدام نموذج BookingForm لحفظ بيانات رحلاتك جوياً، برياً، وبحرياً في قاعدة بيانات Room المحلية مع حماية الضمان المالي."
                prompt.contains("سحب") || prompt.contains("محفظ") ->
                    "رصيدك المتاح للسحب الفوري هو ${_uiState.value.walletBalanceSar} SAR. طرق السحب المتاحة: STC Pay، تحويل بنكي، وApple Pay."
                else ->
                    "منظومة رفيق v3.2.0 تعمل بكفاءة عالية. لقد قمت بتحليل تفاعلات الشورتس والحجوزات الخاصة بك وهناك ارتفاع بنسبة 32% في التفاعل المباشر!"
            }
            val botMsg = AiChatMessage(replyText, isFromUser = false)
            _uiState.update { state ->
                state.copy(aiMessages = state.aiMessages + botMsg)
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
