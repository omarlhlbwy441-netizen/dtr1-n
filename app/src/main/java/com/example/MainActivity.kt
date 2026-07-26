package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RafeeqTheme {
                RafeeqApp()
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector, val tag: String) {
    REPOSITORY("المستودع", Icons.Outlined.Folder, "nav_repo"),
    STORES("المتاجر", Icons.Outlined.Storefront, "nav_stores"),
    SHORTS("رفيق شورتس", Icons.Outlined.VideoLibrary, "nav_shorts"),
    WEBSITES("المواقع", Icons.Outlined.Web, "nav_websites"),
    SOCIAL("الشبكة", Icons.Outlined.Share, "nav_social"),
    WALLET("المحفظة", Icons.Outlined.AccountBalanceWallet, "nav_wallet"),
    PROFILE("الملف الشخصي", Icons.Outlined.Person, "nav_profile")
}

data class RepoFileItem(val name: String, val type: String, val size: String, val description: String)
data class StoreAppItem(val name: String, val category: String, val slots: Int, val fee: String, val status: String, val imageUrl: String = "")
data class WebsiteItem(val title: String, val slug: String, val category: String, val views: Int, val published: Boolean)
data class PostItem(val author: String, val content: String, val likes: Int, val comments: Int, val time: String, val authorAvatarUrl: String = "", val postImageUrl: String = "")

data class ShortVideoItem(
    val id: String,
    val creatorName: String,
    val creatorHandle: String,
    val description: String,
    val soundTrack: String,
    var likesCount: Int,
    var commentsCount: Int,
    val viewsCount: String,
    val linkedProduct: ShortProductLink?,
    val creatorEarnings: String,
    val gradientColors: List<Color>,
    val affiliateCommissionRate: String = "12%",
    val coverImageUrl: String = "",
    val creatorAvatarUrl: String = ""
)

data class ShortProductLink(
    val title: String,
    val price: String,
    val storeName: String,
    val imageUrl: String
)

data class VirtualGift(
    val name: String,
    val icon: String,
    val priceSar: Int
)

data class LiveAuctionItem(
    val id: String,
    val streamerName: String,
    val itemTitle: String,
    var currentBidSar: Int,
    val startingPriceSar: Int,
    var highestBidder: String,
    val activeViewers: String,
    val endsInMinutes: Int,
    val imageUrl: String = "",
    val streamerAvatarUrl: String = ""
)

data class WalletTransaction(
    val id: String,
    val title: String,
    val amount: String,
    val date: String,
    val type: String, // "CREDIT" or "DEBIT"
    val method: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RafeeqApp() {
    var selectedTab by remember { mutableStateOf(NavigationTab.SHORTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAuthDialog by remember { mutableStateOf(false) }

    val firebaseAuth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(firebaseAuth.currentUser) }

    DisposableEffect(firebaseAuth) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        firebaseAuth.addAuthStateListener(listener)
        onDispose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(PrimaryBlue, SecondaryIndigo)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(gradientBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🐺",
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = "رفيق | Rafeeq Kernel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v3.2.0 • Live Stream, VIP & Auth Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Account / Auth Button
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier
                            .testTag("account_button")
                    ) {
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.displayName?.firstOrNull() ?: currentUser?.email?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "تسجيل الدخول / الحساب",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentTeal.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentTeal)
                            )
                            Text(
                                text = "مُستقر",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header stats
            StatsHeaderSummary(selectedTab)

            // Search filter box (if not in shorts mode, wallet mode, or profile mode)
            if (selectedTab != NavigationTab.SHORTS && selectedTab != NavigationTab.PROFILE && selectedTab != NavigationTab.WALLET) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_input"),
                    placeholder = { Text("بحث في المنظومة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Body Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (selectedTab == NavigationTab.SHORTS || selectedTab == NavigationTab.PROFILE || selectedTab == NavigationTab.WALLET) 0.dp else 16.dp)
            ) {
                when (selectedTab) {
                    NavigationTab.SHORTS -> ShortsSection()
                    NavigationTab.REPOSITORY -> RepositorySection(searchQuery)
                    NavigationTab.STORES -> StoresSection(searchQuery)
                    NavigationTab.WEBSITES -> WebsitesSection(searchQuery)
                    NavigationTab.SOCIAL -> SocialSection(searchQuery)
                    NavigationTab.WALLET -> WalletScreen(onOpenAuthDialog = { showAuthDialog = true })
                    NavigationTab.PROFILE -> UserProfileScreen(onOpenAuthDialog = { showAuthDialog = true })
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(onDismissRequest = { showAuthDialog = false })
    }
}

@Composable
fun StatsHeaderSummary(activeTab: NavigationTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = "الأرباح الكلية",
            value = "4,250 SAR",
            subtitle = "+32% عائد مباشر",
            icon = Icons.Default.MonetizationOn,
            color = AccentTeal,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "شورتس وبث حي",
            value = "182.4K",
            subtitle = "تفاعل مباشر",
            icon = Icons.Default.VideoLibrary,
            color = SecondaryIndigo,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "عمولات ومزادات",
            value = "1,820 SAR",
            subtitle = "أرباح التسويق",
            icon = Icons.Default.Gavel,
            color = PrimaryBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsSection() {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("feed") } // "feed", "live_auction", "creator_wallet", "vip_club"
    var showGiftSheet by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var showAffiliateDialog by remember { mutableStateOf(false) }
    var selectedVideoForGift by remember { mutableStateOf<ShortVideoItem?>(null) }
    var userCoinBalance by remember { mutableStateOf(650) }

    val videos = remember {
        mutableStateListOf(
            ShortVideoItem(
                id = "v1",
                creatorName = "عمر الهلباوي",
                creatorHandle = "@omarlhlbwy",
                description = "عرض مميز لخنجر الرفيق الملكي المصنوع يدويًا ✨ شاهد التفاصيل الدقيقة واستمتع بالجودة العالية!",
                soundTrack = "الصوت الأصلي - رفيق نيتزن",
                likesCount = 1420,
                commentsCount = 88,
                viewsCount = "45.2K",
                linkedProduct = ShortProductLink(
                    title = "خنجر الرفيق الملكي الأصيل",
                    price = "350 SAR",
                    storeName = "متجر الرفيق الرقمي",
                    imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80"
                ),
                creatorEarnings = "1,240 SAR",
                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0EA5E9)),
                affiliateCommissionRate = "15%",
                coverImageUrl = "https://images.unsplash.com/photo-1544441893-675973e31985?w=800&auto=format&fit=crop&q=80",
                creatorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80"
            ),
            ShortVideoItem(
                id = "v2",
                creatorName = "سارة أحمد",
                creatorHandle = "@sara_store",
                description = "مراجعة سريعة لأحدث العطور الشرقية المتاحة حصريًا عبر فتحات متجر رفيق 🌸",
                soundTrack = "موسيقى الاسترخاء والجمال",
                likesCount = 2890,
                commentsCount = 140,
                viewsCount = "92.1K",
                linkedProduct = ShortProductLink(
                    title = "عطر اللافندر الملكي 100ml",
                    price = "180 SAR",
                    storeName = "عالم العطور الشامل",
                    imageUrl = "https://images.unsplash.com/photo-1541643600914-78b084683601?w=400&auto=format&fit=crop&q=80"
                ),
                creatorEarnings = "2,110 SAR",
                gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF6366F1)),
                affiliateCommissionRate = "12%",
                coverImageUrl = "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=800&auto=format&fit=crop&q=80",
                creatorAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&auto=format&fit=crop&q=80"
            ),
            ShortVideoItem(
                id = "v3",
                creatorName = "فريق تطوير رفيق",
                creatorHandle = "@rafeeq_dev",
                description = "شرح كيفية ربط الفيديوهات القصيرة بالمنتجات وحساب الأرباح التلقائي في النظام ⚙️💰",
                soundTrack = "نغمة التقنية الحديثة",
                likesCount = 540,
                commentsCount = 32,
                viewsCount = "12.8K",
                linkedProduct = null,
                creatorEarnings = "450 SAR",
                gradientColors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981)),
                affiliateCommissionRate = "10%",
                coverImageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&auto=format&fit=crop&q=80",
                creatorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80"
            )
        )
    }

    val liveAuctions = remember {
        mutableStateListOf(
            LiveAuctionItem(
                id = "auction_1",
                streamerName = "مزادات الرفيق الملكية",
                itemTitle = "ساعة يد أصلية مرصعة بالزمرد ⌚",
                currentBidSar = 1200,
                startingPriceSar = 800,
                highestBidder = "@faisal_saud",
                activeViewers = "1,420",
                endsInMinutes = 8,
                imageUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=800&auto=format&fit=crop&q=80",
                streamerAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80"
            ),
            LiveAuctionItem(
                id = "auction_2",
                streamerName = "معرض التحف النادرة",
                itemTitle = "لوحة زيتية أصلية للصحراء العربية 🎨",
                currentBidSar = 3400,
                startingPriceSar = 2000,
                highestBidder = "@norah_art",
                activeViewers = "3,890",
                endsInMinutes = 15,
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&auto=format&fit=crop&q=80",
                streamerAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80"
            )
        )
    }

    val virtualGifts = remember {
        listOf(
            VirtualGift("الذئب الذهبي 🐺", "🐺", 100),
            VirtualGift("الماسية 💎", "💎", 50),
            VirtualGift("القهوة العربية ☕", "☕", 10),
            VirtualGift("نجمة رفيق ⭐", "⭐", 5)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs (Feed, Live Auction, Wallet, VIP)
        ScrollableTabRow(
            selectedTabIndex = when (activeSubTab) {
                "feed" -> 0
                "live_auction" -> 1
                "creator_wallet" -> 2
                else -> 3
            },
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 8.dp
        ) {
            Tab(
                selected = activeSubTab == "feed",
                onClick = { activeSubTab = "feed" },
                text = { Text("Shorts Feed", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == "live_auction",
                onClick = { activeSubTab = "live_auction" },
                text = { Text("البث والمزاد 🔥", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Gavel, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == "creator_wallet",
                onClick = { activeSubTab = "creator_wallet" },
                text = { Text("المحفظة والسحب 💰", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == "vip_club",
                onClick = { activeSubTab = "vip_club" },
                text = { Text("نادي VIP 👑", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) }
            )
        }

        when (activeSubTab) {
            "feed" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp)
                                .testTag("video_card_${video.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Background Video Cover with Coil or fallback gradient
                                if (video.coverImageUrl.isNotBlank()) {
                                    CoilAsyncImage(
                                        imageUrl = video.coverImageUrl,
                                        contentDescription = video.description,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.85f)
                                                    )
                                                )
                                            )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(video.gradientColors))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                // Top Bar Overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.Black.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text(text = video.viewsCount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Gemini AI Assistant Button
                                        IconButton(
                                            onClick = {
                                                selectedVideoForGift = video
                                                showAiAssistantDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(PrimaryBlue.copy(alpha = 0.85f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "تحليل الذكاء الاصطناعي", tint = Color.Yellow, modifier = Modifier.size(18.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = AccentTeal.copy(alpha = 0.9f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Text(text = "ربح: ${video.creatorEarnings}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Center Play Icon Indicator
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .align(Alignment.Center)
                                        .clickable {
                                            Toast.makeText(context, "تشغيل الفيديو القصير ▶️", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.White, modifier = Modifier.size(36.dp))
                                }

                                // Right Vertical Action Buttons (TikTok style)
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Like
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = { video.likesCount += 1 },
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Favorite, contentDescription = "إعجاب", tint = Color.Red)
                                        }
                                        Text(text = "${video.likesCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Comment
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                Toast.makeText(context, "فتح التعليقات والتفاعل", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.ChatBubble, contentDescription = "تعليق", tint = Color.White)
                                        }
                                        Text(text = "${video.commentsCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Affiliate Link
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                selectedVideoForGift = video
                                                showAffiliateDialog = true
                                            },
                                            modifier = Modifier.background(SecondaryIndigo.copy(alpha = 0.9f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = "رابط التتبع", tint = Color.White)
                                        }
                                        Text(text = "عمولة", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Send Gift / Monetization Tip
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                selectedVideoForGift = video
                                                showGiftSheet = true
                                            },
                                            modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.8f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.CardGiftcard, contentDescription = "إهداء أرباح", tint = Color.White)
                                        }
                                        Text(text = "إهداء", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Bottom Creator Info & Shoppable Tag
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.82f)
                                        .align(Alignment.BottomStart),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Linked Product Badge if available
                                    video.linkedProduct?.let { product ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SecondaryIndigo,
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, "انتقال لصفحة الشراء في ${product.storeName}", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (product.imageUrl.isNotBlank()) {
                                                    CoilAsyncImage(
                                                        imageUrl = product.imageUrl,
                                                        contentDescription = product.title,
                                                        modifier = Modifier.size(36.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                } else {
                                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = product.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text(text = "${product.price} • عمولة ${video.affiliateCommissionRate}", color = Color.Yellow, fontSize = 10.sp)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.White
                                                ) {
                                                    Text(
                                                        text = "شراء 🛒",
                                                        color = SecondaryIndigo,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Creator details
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (video.creatorAvatarUrl.isNotBlank()) {
                                            CoilAsyncImage(
                                                imageUrl = video.creatorAvatarUrl,
                                                contentDescription = video.creatorName,
                                                modifier = Modifier.size(36.dp),
                                                shape = CircleShape
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = video.creatorName.first().toString(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = video.creatorName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = video.creatorHandle,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = video.description,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text(text = video.soundTrack, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            "live_auction" -> {
                // Live Stream & Live Auction View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "البث المباشر والمزادات الحية 🔴", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "شارك في المزايدة اللحظية واربح المنتجات النادرة", color = Color.Gray, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "جاري فتح استوديو البث المباشر (Rafeeq Studio Live) 🎥", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("ابدأ بثك 🎥", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(liveAuctions, key = { it.id }) { auction ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (auction.streamerAvatarUrl.isNotBlank()) {
                                            CoilAsyncImage(
                                                imageUrl = auction.streamerAvatarUrl,
                                                contentDescription = auction.streamerName,
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Red
                                        ) {
                                            Text(
                                                text = "مباشر LIVE 🔴",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(text = auction.streamerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }

                                    Text(
                                        text = "👀 ${auction.activeViewers} مشاهد",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AccentTeal
                                    )
                                }

                                Divider()

                                // Live Auction Item Image loaded via Coil
                                if (auction.imageUrl.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        CoilAsyncImage(
                                            imageUrl = auction.imageUrl,
                                            contentDescription = auction.itemTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Text(
                                    text = auction.itemTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "السعر الحالي في المزاد:", style = MaterialTheme.typography.labelMedium)
                                        Text(text = "${auction.currentBidSar} SAR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AccentTeal)
                                        Text(text = "أعلى مزايد: ${auction.highestBidder}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "ينتهي المزاد خلال:", style = MaterialTheme.typography.labelMedium)
                                        Text(text = "${auction.endsInMinutes} دقيقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            auction.currentBidSar += 50
                                            auction.highestBidder = "@انت (مستخدم رفيق)"
                                            Toast.makeText(context, "تمت المزايدة بـ ${auction.currentBidSar} SAR 🔨!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Text("زايد +50 SAR 🔨")
                                    }

                                    Button(
                                        onClick = {
                                            auction.currentBidSar += 200
                                            auction.highestBidder = "@انت (مستخدم رفيق)"
                                            Toast.makeText(context, "تمت المزايدة بـ ${auction.currentBidSar} SAR 🔨!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryIndigo)
                                    ) {
                                        Text("زايد +200 SAR ⚡")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "creator_wallet" -> {
                CreatorWalletView(userCoinBalance) { addedCoins ->
                    userCoinBalance += addedCoins
                }
            }

            "vip_club" -> {
                VipClubView()
            }
        }
    }

    // Modal Sheet for Gift Sending
    if (showGiftSheet && selectedVideoForGift != null) {
        AlertDialog(
            onDismissRequest = { showGiftSheet = false },
            title = {
                Text(
                    text = "ارسال هدية مالية لصانع المحتوى 🎁",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "دعم الصانع: ${selectedVideoForGift?.creatorName} (${selectedVideoForGift?.creatorHandle})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "رصيد نقاطك المتاح:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "$userCoinBalance SAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentTeal)
                    }

                    Text(text = "اختر الهدية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(virtualGifts) { gift ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .clickable {
                                        if (userCoinBalance >= gift.priceSar) {
                                            userCoinBalance -= gift.priceSar
                                            selectedVideoForGift?.likesCount = (selectedVideoForGift?.likesCount ?: 0) + 10
                                            Toast.makeText(context, "تم إرسال ${gift.name} بنجاح! 🚀", Toast.LENGTH_LONG).show()
                                            showGiftSheet = false
                                        } else {
                                            Toast.makeText(context, "عذرًا، رصيدك غير كافٍ. يرجى الشحن", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .border(1.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = gift.icon, fontSize = 28.sp)
                                    Text(text = gift.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "${gift.priceSar} SAR", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGiftSheet = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Modal for Gemini AI Shopping Assistant
    if (showAiAssistantDialog && selectedVideoForGift != null) {
        AlertDialog(
            onDismissRequest = { showAiAssistantDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue)
                    Text(text = "مساعد Gemini AI لإنشاء المحتوى ✨", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "قام الذكاء الاصطناعي بتحليل الفيديو وحساب أعلى سيناريو تسويقي جذاب:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "💡 عنوان مقترح:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                            Text(text = "\"استعد للفخامة والتميز مع خيارات رفيق الحصرية!\"", fontSize = 13.sp)
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(text = "🏷️ هاشتاجات ذكية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                            Text(text = "#رفيق_شورتس #موضة #أناقة #تسوق_ذكيا #تجارة_الرفيق", fontSize = 12.sp, color = SecondaryIndigo)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    Toast.makeText(context, "تم تطبيق العنوان والوسوم المقترحة 🪄", Toast.LENGTH_SHORT).show()
                    showAiAssistantDialog = false
                }) {
                    Text("تطبيق التوصيات ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiAssistantDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Modal for Affiliate Track Link Generation
    if (showAffiliateDialog && selectedVideoForGift != null) {
        val video = selectedVideoForGift!!
        val affiliateUrl = "https://rafeeq.app/ref/omarlhlbwy_${video.id}"

        AlertDialog(
            onDismissRequest = { showAffiliateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = AccentTeal)
                    Text(text = "رابط التسويق بالعمولة 🔗", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "شارك هذا الرابط مع متابعيك على المنصات الأخرى. ستحصل على عمولة تلقائية قدرها ${video.affiliateCommissionRate} عند كل عملية شراء يتم إتمامها!",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = affiliateUrl,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رابطك الخاص للتتبع") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "تم نسخ رابط التتبع الخاص بك! 📋", Toast.LENGTH_SHORT).show()
                        showAffiliateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("نسخ الرابط 📋")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAffiliateDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun VipClubView() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "نادي صناع المحتوى VIP 👑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFD700)
                        ) {
                            Text(text = "العضوية الذهبية", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Text(text = "احصل على معالم حصرية ومزايا مالية متقدمة داخل منظومة رفيق.", color = Color.LightGray, fontSize = 13.sp)

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                            Text(text = "نسبة عمولات مضاعفة 20% على تسويق المنتجات", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                            Text(text = "شارة التوثيق الذهبية 👑 بجانب اسمك", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                            Text(text = "سحب أرباح فوري بدون رسوم تحويل منصة", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                            Text(text = "دعم بث مباشر بجودة 4K وفلاتر تحسين المزايدة", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "تم تفعيل اشتراك نادي VIP بنجاح! 👑", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("الاشتراك بـ 99 SAR / شهريًا 🌟", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorWalletView(coins: Int, onTopUpCoins: (Int) -> Unit) {
    val context = LocalContext.current
    var selectedPayoutMethod by remember { mutableStateOf("STC Pay") }

    val transactions = remember {
        listOf(
            WalletTransaction("tx1", "أرباح إهداء الذئب الذهبي 🐺", "+100 SAR", "اليوم 10:15 am", "CREDIT", "شورتس"),
            WalletTransaction("tx2", "عمولة بيع خنجر الرفيق 🗡️", "+52.5 SAR", "أمس", "CREDIT", "افيلييت"),
            WalletTransaction("tx3", "سحب أرباح للبنك الأهلي 🏦", "-500 SAR", "منذ 3 أيام", "DEBIT", "STC Pay")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "محفظة أرباح رفيق والسحب المالي 💰", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "نظام استثمار الفيديوهات القصيرة والعمولات والمزادات الحية", color = Color.LightGray, fontSize = 12.sp)

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "إجمالي الأرباح المتاحة للسحب", color = Color.Gray, fontSize = 12.sp)
                            Text(text = "4,250.00 SAR", color = AccentTeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "تم إرسال طلب سحب $selectedPayoutMethod بنجاح! 🏦", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("سحب الأرباح 🏦")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "طريقة السحب المفضل:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("STC Pay", "Apple Pay", "حساب بنكي", "USDT Crypto").forEach { method ->
                            FilterChip(
                                selected = selectedPayoutMethod == method,
                                onClick = { selectedPayoutMethod = method },
                                label = { Text(method, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(text = "إحصائيات الأداء المالي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "أرباح المشاهدات CPM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "1,450 SAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "هدايا المتابعين", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "1,950 SAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "عمولات المنتجات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "850 SAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        item {
            Text(text = "سجل المعاملات المالي 📄", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(transactions, key = { it.id }) { tx ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = tx.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${tx.date} • ${tx.method}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        text = tx.amount,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == "CREDIT") AccentTeal else Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "شحن رصيد الهدايا والنقاط 🪙", fontWeight = FontWeight.Bold)
                    Text(text = "يمكنك شحن النقاط لاستخدامها في دعم صناع المحتوى أو ربط إعلاناتك الخاصة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTopUpCoins(100) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryIndigo)
                        ) {
                            Text("+100 SAR")
                        }
                        Button(
                            onClick = { onTopUpCoins(500) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryIndigo)
                        ) {
                            Text("+500 SAR")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepositorySection(query: String) {
    val repoFiles = remember {
        listOf(
            RepoFileItem("main.py", "Python", "48.8 KB", "المحرك الرئيسي ونقاط النهاية Flask + JWT + ORM"),
            RepoFileItem("auto_migrate.py", "Python", "24.2 KB", "محرك الترحيل الآلي لقواعد البيانات (v3.3.0)"),
            RepoFileItem("migrate_db.py", "Python", "3.1 KB", "سكربت مخصص للترحيلات المتقدمة"),
            RepoFileItem("src/App.tsx", "TypeScript", "48.8 KB", "واجهة التحكم التفاعلية React + Vite"),
            RepoFileItem("package.json", "JSON", "845 B", "إعدادات حزم Node والملحقات"),
            RepoFileItem("render.yaml", "YAML", "476 B", "إعدادات النشر على استضافة Render"),
            RepoFileItem("requirements.txt", "Text", "168 B", "اعتماديات Python (Flask, PyJWT, SQLAlchemy)"),
            RepoFileItem("templates/", "Directory", "-", "قوالب HTML للمتاجر والمواقع ولوحة المطور")
        )
    }

    val filtered = repoFiles.filter {
        it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "ملفات المستودع الهيكلية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        items(filtered) { file ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("repo_file_${file.name.replace('/', '_')}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (file.type == "Directory") Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = file.type,
                        tint = PrimaryBlue
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = file.size,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = file.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoresSection(query: String) {
    val stores = remember {
        listOf(
            StoreAppItem("متجر الرفيق الرقمي", "إلكترونيات وخناجر", 5, "150 SAR", "معتمد"),
            StoreAppItem("عالم العطور الشامل", "عطور وتجميل", 3, "100 SAR", "معتمد"),
            StoreAppItem("مكتبة الفكر العربي", "كتب ومطبوعات", 2, "75 SAR", "قيد المراجعة"),
            StoreAppItem("متجر التقنية السريعة", "برمجيات وأجهزة", 8, "250 SAR", "نشط جدًا")
        )
    }

    val filtered = stores.filter {
        it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "تطبيقات المتاجر والفتحات (Store Applications & Slots)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        items(filtered) { store ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("store_item_${store.name}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(
                            containerColor = if (store.status == "قيد المراجعة") Color(0xFFF59E0B) else AccentTeal,
                            contentColor = Color.White
                        ) {
                            Text(text = store.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "الفئة: ${store.category}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "الفتحات: ${store.slots}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(text = "الرسم: ${store.fee}", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun WebsitesSection(query: String) {
    val websites = remember {
        listOf(
            WebsiteItem("موقع رفيق التعريفي", "rafeeq-main", "منصة خدمات", 1420, true),
            WebsiteItem("بوابة الأعمال الذكية", "smart-biz", "حلول تقنية", 890, true),
            WebsiteItem("معرض الخدمات الرقمية", "digital-expo", "تسويق", 350, false)
        )
    }

    val filtered = websites.filter {
        it.title.contains(query, ignoreCase = true) || it.slug.contains(query, ignoreCase = true)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "المواقع الإلكترونية المُنشأة (Websites)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        items(filtered) { site ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("website_item_${site.slug}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = SecondaryIndigo)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = site.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/site/${site.slug} • ${site.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${site.views} زيارة",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = if (site.published) "منشور" else "مسودة",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (site.published) AccentTeal else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialSection(query: String) {
    val posts = remember {
        listOf(
            PostItem(
                author = "عمر الهلباوي",
                content = "تم إطلاق النسخة v3.2.0 من نظام رفيق مع دعم كامل للمزاد الحي والبث المباشر والتسويق بالعمولة وسحب الأرباح Multi-Payout! 🚀",
                likes = 72,
                comments = 18,
                time = "منذ ساعة",
                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                postImageUrl = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&auto=format&fit=crop&q=80"
            ),
            PostItem(
                author = "فريق التطوير",
                content = "تمت إضافة نظام الذكاء الاصطناعي لتأليف النصوص التسويقية والوسوم التلقائية للفيديوهات القصيرة 🤖✨",
                likes = 54,
                comments = 9,
                time = "منذ 3 ساعات",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80"
            ),
            PostItem(
                author = "المسؤول التنفيذي",
                content = "أهلاً وسهلاً بكافة التجار وصناع المحتوى في نادي VIP الرفيق المميز.",
                likes = 95,
                comments = 24,
                time = "منذ يوم",
                authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80"
            )
        )
    }

    val filtered = posts.filter {
        it.author.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "منشورات شبكة رفيق الاجتماعية (Social Feed)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        items(filtered) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("post_item_${post.author}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (post.authorAvatarUrl.isNotBlank()) {
                                CoilAsyncImage(
                                    imageUrl = post.authorAvatarUrl,
                                    contentDescription = post.author,
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = post.author.first().toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = post.author,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = post.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (post.postImageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            CoilAsyncImage(
                                imageUrl = post.postImageUrl,
                                contentDescription = "مرفق المنشور",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "إعجاب", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Text(text = "${post.likes}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "تعليقات", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Text(text = "${post.comments}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
