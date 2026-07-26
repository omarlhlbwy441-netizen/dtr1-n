package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: WalletRepository = remember { WalletRepository() },
    onOpenAuthDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        UnauthenticatedWalletState(onOpenAuthDialog = onOpenAuthDialog)
    } else {
        AuthenticatedWalletState(
            user = currentUser!!,
            repository = repository
        )
    }
}

@Composable
private fun UnauthenticatedWalletState(
    onOpenAuthDialog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wallet_screen")
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = "المحفظة الأرباح",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "محفظة الأرباح الرقمية 💰",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "سجّل الدخول لمتابعة أرباحك، عرض سجل السحوبات، وسحب مستحقاتك بكل سهولة وأمان.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenAuthDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_auth_wallet_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الدخول لعرض المحفظة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWalletState(
    user: FirebaseUser,
    repository: WalletRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var walletBalance by remember { mutableStateOf(WalletBalance()) }
    var transactionsList by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "EARNING", "WITHDRAWAL", "PENDING"

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showAddTestEarningDialog by remember { mutableStateOf(false) }

    // Observe Firestore Streams
    LaunchedEffect(user.uid) {
        launch {
            repository.observeWalletBalance(user.uid).collect { balance ->
                walletBalance = balance
            }
        }
        launch {
            repository.observeTransactions(user.uid).collect { list ->
                transactionsList = list
            }
        }
    }

    val filteredTransactions = remember(transactionsList, selectedFilter) {
        when (selectedFilter) {
            "EARNING" -> transactionsList.filter { it.amount > 0 }
            "WITHDRAWAL" -> transactionsList.filter { it.type == "WITHDRAWAL" || it.amount < 0 }
            "PENDING" -> transactionsList.filter { it.status == "PENDING" }
            else -> transactionsList
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wallet_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Balance Card
        item {
            HeroBalanceCard(
                walletBalance = walletBalance,
                onRequestWithdrawal = { showWithdrawDialog = true },
                onAddDemoEarnings = { showAddTestEarningDialog = true }
            )
        }

        // Stats Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceMiniStatCard(
                    title = "الأرباح المعلقة",
                    amount = "${String.format(Locale.US, "%.2f", walletBalance.pendingBalance)} ${walletBalance.currency}",
                    icon = Icons.Outlined.HourglassTop,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )

                BalanceMiniStatCard(
                    title = "إجمالي السحوبات",
                    amount = "${String.format(Locale.US, "%.2f", walletBalance.totalWithdrawn)} ${walletBalance.currency}",
                    icon = Icons.Outlined.Paid,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section Title & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل المعاملات والأرباح",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredTransactions.size} معاملة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل") }
                    )
                    FilterChip(
                        selected = selectedFilter == "EARNING",
                        onClick = { selectedFilter = "EARNING" },
                        label = { Text("الأرباح 📈") }
                    )
                    FilterChip(
                        selected = selectedFilter == "WITHDRAWAL",
                        onClick = { selectedFilter = "WITHDRAWAL" },
                        label = { Text("السحوبات 💸") }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("المعلقة ⏳") }
                    )
                }
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد معاملات تندرج تحت هذا التصنيف بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { item ->
                TransactionCardItem(transaction = item)
            }
        }
    }

    // Request Withdrawal Dialog
    if (showWithdrawDialog) {
        RequestWithdrawalDialog(
            availableBalance = walletBalance.availableBalance,
            currency = walletBalance.currency,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { amount, method, details ->
                coroutineScope.launch {
                    val result = repository.requestWithdrawal(
                        uid = user.uid,
                        amount = amount,
                        paymentMethod = method,
                        accountDetails = details,
                        currentBalance = walletBalance
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, "تم تقديم طلب السحب بنجاح وهي قيد المعالجة! 💸", Toast.LENGTH_LONG).show()
                        showWithdrawDialog = false
                    } else {
                        Toast.makeText(context, "فشل تقديم الطلب: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Add Demo Earning Dialog
    if (showAddTestEarningDialog) {
        AddDemoEarningDialog(
            onDismiss = { showAddTestEarningDialog = false },
            onSubmit = { amount, title, desc ->
                coroutineScope.launch {
                    val result = repository.addTestEarning(
                        uid = user.uid,
                        amount = amount,
                        title = title,
                        description = desc,
                        currentBalance = walletBalance
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, "تمت إضافة الأرباح إلى المحفظة وتحديث Firestore! 🚀", Toast.LENGTH_SHORT).show()
                        showAddTestEarningDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun HeroBalanceCard(
    walletBalance: WalletBalance,
    onRequestWithdrawal: () -> Unit,
    onAddDemoEarnings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card")
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Dark slate
                        Color(0xFF1E293B),
                        Color(0xFF0F766E)  // Deep teal
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "الرصيد المتاح للسحب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2DD4BF).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2DD4BF))
                ) {
                    Text(
                        text = walletBalance.currency,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2DD4BF)
                    )
                }
            }

            // Big Balance Amount Display
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f", walletBalance.availableBalance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
                Text(
                    text = walletBalance.currency,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Divider(color = Color.White.copy(alpha = 0.12f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRequestWithdrawal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("request_withdrawal_button")
                ) {
                    Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طلب سحب الأرباح", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onAddDemoEarnings,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_demo_earning_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("أرباح تجريبية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun BalanceMiniStatCard(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TransactionCardItem(transaction: TransactionItem) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("ar")) }
    val isEarning = transaction.amount > 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transaction Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEarning) Color(0xFF10B981).copy(alpha = 0.15f)
                        else Color(0xFFEF4444).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEarning) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isEarning) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Title & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title.ifBlank { if (isEarning) "إيداع أرباح" else "طلب سحب" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(transaction.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Status Badge
                    val statusColor = when (transaction.status) {
                        "COMPLETED" -> Color(0xFF10B981)
                        "PENDING" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    val statusText = when (transaction.status) {
                        "COMPLETED" -> "مكتملة"
                        "PENDING" -> "قيد المعالجة"
                        else -> "ملغاة"
                    }

                    Text(
                        text = "• $statusText",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Amount Display
            Text(
                text = "${if (isEarning) "+" else ""}${String.format(Locale.US, "%.2f", transaction.amount)} ${transaction.currency}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isEarning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RequestWithdrawalDialog(
    availableBalance: Double,
    currency: String,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, details: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Vodafone Cash / فودافون كاش") }
    var accountDetails by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val methods = listOf(
        "Vodafone Cash / فودافون كاش",
        "Bank Transfer / تحويل بنكي (IBAN)",
        "PayPal / بايبال",
        "Crypto USDT / محفظة رقمية"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CallMade, contentDescription = null, tint = Color(0xFF10B981))
                Text("طلب سحب الأرباح 💸", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الرصيد المتاح:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${String.format(Locale.US, "%.2f", availableBalance)} $currency",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Quick Amount Selection Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(20, 50, 100).forEach { amt ->
                        FilterChip(
                            selected = amountText == amt.toString(),
                            onClick = { amountText = amt.toString() },
                            label = { Text("$$amt") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = amountText == availableBalance.toString(),
                        onClick = { amountText = availableBalance.toString() },
                        label = { Text("الكل") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = ""
                    },
                    label = { Text("المبلغ المطلوب سحبه ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("وسيلة التحويل المفضلة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMethod = method }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(method, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = accountDetails,
                    onValueChange = { accountDetails = it },
                    label = { Text("تفاصيل الحساب / رقم المحفظة / IBAN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_account_details_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "يرجى كتابة مبلغ صحيح"
                        return@Button
                    }
                    if (amt > availableBalance) {
                        errorMessage = "المبلغ المطلوب أعلى من الرصيد المتاح"
                        return@Button
                    }
                    if (accountDetails.isBlank()) {
                        errorMessage = "يرجى أدخال تفاصيل الحساب للتحويل"
                        return@Button
                    }
                    onSubmit(amt, selectedMethod, accountDetails)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_withdrawal_button")
            ) {
                Text("تأكيد طلب السحب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddDemoEarningDialog(
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, title: String, desc: String) -> Unit
) {
    var amountText by remember { mutableStateOf("50.0") }
    var titleText by remember { mutableStateOf("عمولة مبيعات جديدة 🚀") }
    var descText by remember { mutableStateOf("إيداع تلقائي من نظام رفيق الإلكتروني") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة أرباح تجريبية ➕", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("عنوان المعاملة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("الوصف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 10.0
                    onSubmit(amt, titleText, descText)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إضافة إلى المحفظة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
