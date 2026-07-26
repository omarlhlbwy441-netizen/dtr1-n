package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RafeeqUiState
import com.example.ui.RafeeqViewModel
import com.example.ui.model.*
import com.example.ui.theme.*

// ----------------------------------------------------
// Top Header Component
// ----------------------------------------------------
@Composable
fun RafeeqHeader(
    uiState: RafeeqUiState,
    onOpenAi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CyberDarkBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right / Start side: Avatar & Title (RTL support)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wolf Avatar with glowing ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CyanGlow.copy(alpha = 0.6f), CyberCardBg)
                            )
                        )
                        .border(1.5.dp, CyanGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Rafeeq Wolf Avatar",
                        tint = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rafeeq Kernel | ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "رفيق",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                        )
                    }
                    Text(
                        text = "v3.2.0 • Live Stream, VIP & Affiliate Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Left / End side: Status Badge & AI Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
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
                                .background(EmeraldGreen)
                        )
                        Text(
                            text = "مستقر",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // AI Sparkle FAB Icon
                IconButton(
                    onClick = onOpenAi,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CyanGlow, BlueGlow)
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Rafeeq AI Companion",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Dashboard Stats Section
// ----------------------------------------------------
@Composable
fun StatsCardsSection(uiState: RafeeqUiState) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Total Earnings
        item {
            StatCardItem(
                title = "الأرباح الكلية",
                value = "${String.format("%.0f", uiState.totalEarningsSar)} SAR",
                subtitle = "↗ عائد مباشر +32%",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = EmeraldGreen
            )
        }
        // Card 2: Shorts & Live Interaction
        item {
            StatCardItem(
                title = "شورتس وبث حي",
                value = uiState.totalInteractions,
                subtitle = "تفاعل مباشر عبر الفيديو",
                icon = Icons.Default.PlayCircleFilled,
                accentColor = CyanGlow
            )
        }
        // Card 3: Commissions & Auctions
        item {
            StatCardItem(
                title = "عمولات ومزادات",
                value = "${String.format("%.0f", uiState.affiliateCommissionsSar)} SAR",
                subtitle = "أرباح التسويق بالعمولة",
                icon = Icons.Default.Gavel,
                accentColor = GoldAccent
            )
        }
    }
}

@Composable
fun StatCardItem(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// ----------------------------------------------------
// Action Filter / Navigation Bar
// ----------------------------------------------------
@Composable
fun ActionTabsBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Pair("رفيق شورتس", Icons.Default.PlayArrow),
        Pair("البث والمزاد 🔥", Icons.Default.Whatshot),
        Pair("المحفظة والسحب 💰", Icons.Default.AccountBalance),
        Pair("نادي VIP 👑", Icons.Default.WorkspacePremium),
        Pair("حجوزات السفر ✈️", Icons.Default.FlightTakeoff)
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = CyberDarkBg,
        contentColor = CyanGlow,
        edgePadding = 16.dp,
        divider = {}
    ) {
        tabs.forEachIndexed { index, pair ->
            val isSelected = selectedTab == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = pair.second,
                            contentDescription = null,
                            tint = if (isSelected) GoldAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = pair.first,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GoldAccent else TextSecondary
                            )
                        )
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------
// Shorts Feed Player View
// ----------------------------------------------------
@Composable
fun ShortsFeedView(
    uiState: RafeeqUiState,
    viewModel: RafeeqViewModel
) {
    val currentShort = uiState.shortsList.getOrNull(uiState.currentShortIndex) ?: uiState.shortsList.first()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            )
            .border(1.5.dp, Brush.horizontalGradient(listOf(CyanGlow.copy(alpha = 0.5f), PurpleAccent.copy(alpha = 0.5f))), RoundedCornerShape(24.dp))
    ) {
        // Video Mockup Player Background Simulation
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Live Viewers Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${currentShort.views} مشاهد",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Commission Indicator Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "عمولة ${currentShort.commissionRate} (${currentShort.commissionAmount})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Center Video Play Button Mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { /* Toggle Video Pause / Play */ },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, GoldAccent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = GoldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom Player Overlay Info & Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(16.dp)
            ) {
                // Product Purchase Affiliate Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg.copy(alpha = 0.9f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = CyanGlow.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = CyanGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = currentShort.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentShort.productPrice} • ربح عمولتك: ${currentShort.commissionAmount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldAccent,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.sendAiMessage("أريد معلومات وشراء ${currentShort.productName}")
                                viewModel.toggleAiAssistant(true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "شراء الآن",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }

                // Creator & Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentShort.creatorName.take(1),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentShort.creatorName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentShort.creatorHandle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                        Text(
                            text = currentShort.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Right Floating Social Actions Column
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Like Button
            FloatingInteractionButton(
                icon = if (currentShort.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                tint = if (currentShort.isLiked) RoseAccent else Color.White,
                count = "${currentShort.likesCount}",
                onClick = { viewModel.toggleLikeShort(currentShort.id) }
            )

            // Comments Button
            FloatingInteractionButton(
                icon = Icons.Outlined.ModeComment,
                tint = Color.White,
                count = "${currentShort.commentsCount}",
                onClick = {}
            )

            // Share Affiliate Link
            FloatingInteractionButton(
                icon = Icons.Outlined.Share,
                tint = CyanGlow,
                count = "عمولة",
                onClick = {
                    viewModel.sendAiMessage("تم نسخ رابط العمولة لمنتج ${currentShort.productName}")
                }
            )

            // Gift Button
            FloatingInteractionButton(
                icon = Icons.Outlined.CardGiftcard,
                tint = GoldAccent,
                count = "إهداء",
                onClick = {}
            )
        }
    }
}

@Composable
fun FloatingInteractionButton(
    icon: ImageVector,
    tint: Color,
    count: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, tint.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ----------------------------------------------------
// Live Stream & Auctions Tab View
// ----------------------------------------------------
@Composable
fun AuctionsView(
    uiState: RafeeqUiState,
    viewModel: RafeeqViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المزادات الحية والبث المباشر 🔥",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "LIVE الآن",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = RoseAccent,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        items(uiState.liveAuctions) { auction ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = auction.streamerName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "متبقي: 05:45",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyanGlow)
                            )
                        }
                    }

                    Text(
                        text = auction.itemTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Divider(color = CyberCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المزايدة الحالية:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = "${auction.currentBidSar} SAR",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen
                                )
                            )
                            Text(
                                text = "أعلى مزايد: ${auction.highestBidder}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.placeBidOnAuction(auction.id, 50) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+50 SAR", color = TextPrimary)
                            }
                            Button(
                                onClick = { viewModel.placeBidOnAuction(auction.id, 100) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+100 SAR", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Wallet & Payouts Tab View
// ----------------------------------------------------
@Composable
fun WalletView(
    uiState: RafeeqUiState,
    viewModel: RafeeqViewModel
) {
    var payoutAmountInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(EmeraldGreen, CyanGlow))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "رصيد المحفظة المتاح للسحب",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Text(
                        text = "${String.format("%.2f", uiState.walletBalanceSar)} SAR",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen
                        )
                    )

                    OutlinedTextField(
                        value = payoutAmountInput,
                        onValueChange = { payoutAmountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("المبلغ المراد سحبه (SAR)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = payoutAmountInput.toDoubleOrNull() ?: 0.0
                                viewModel.requestPayout(amt, "STC Pay")
                                payoutAmountInput = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("سحب STC Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amt = payoutAmountInput.toDoubleOrNull() ?: 0.0
                                viewModel.requestPayout(amt, "تحويل بنكي")
                                payoutAmountInput = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تحويل بنكي", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Transactions History Header
        item {
            Text(
                text = "سجل السحب والعمولات الأخيرة",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        items(uiState.walletTransactions) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tx.type,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = tx.date,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isPositive = tx.amountSar > 0
                        Text(
                            text = "${if (isPositive) "+" else ""}${tx.amountSar} SAR",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositive) EmeraldGreen else RoseAccent
                            )
                        )
                        Text(
                            text = tx.status,
                            style = MaterialTheme.typography.labelSmall.copy(color = CyanGlow)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// VIP Creator Club & Store Slots Tab View
// ----------------------------------------------------
@Composable
fun VipSlotsView(uiState: RafeeqUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نادي VIP صناع المحتوى 👑",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "مستوى VIP الذبي",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "عضويتك تمنحك أولوية في المزادات وعمولات مضاعفة على جميع المبيعات في المتاجر الشريكة.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        item {
            Text(
                text = "حجوزات المتاجر والشركاء (Store Slots)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        items(uiState.storeSlots) { slot ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = slot.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${slot.category} • كود: ${slot.code}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = slot.fee,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "الحالة: ${slot.status}",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// AI Companion Floating Dialog Component
// ----------------------------------------------------
@Composable
fun AiAssistantDialog(
    uiState: RafeeqUiState,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = CyberCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanGlow
                    )
                    Text(
                        text = "مساعد رفيق الذكي 🤖",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.aiMessages) { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (msg.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (msg.isFromUser) CyanGlow.copy(alpha = 0.2f) else CyberCardBorder,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (msg.isFromUser) CyanGlow else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اسأل رفيق...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyanGlow)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black)
                    }
                }
            }
        }
    )
}

// ----------------------------------------------------
// Bottom Navigation Bar
// ----------------------------------------------------
@Composable
fun RafeeqBottomBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val items = listOf(
        Pair("المستودع", Icons.Outlined.Folder),
        Pair("المتاجر", Icons.Outlined.Storefront),
        Pair("رفيق شورتس", Icons.Default.PlayCircle),
        Pair("المواقع", Icons.Outlined.Public),
        Pair("الشبكة", Icons.Outlined.Hub)
    )

    NavigationBar(
        containerColor = CyberDarkBg,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEachIndexed { index, pair ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectIndex(index) },
                icon = {
                    Icon(
                        imageVector = pair.second,
                        contentDescription = pair.first,
                        tint = if (isSelected) GoldAccent else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) GoldAccent else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyberCardBorder
                )
            )
        }
    }
}
