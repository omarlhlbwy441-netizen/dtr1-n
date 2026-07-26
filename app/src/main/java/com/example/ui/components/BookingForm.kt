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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Booking
import com.example.data.TravelBookingEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingForm(
    savedBookings: List<Booking>,
    onSaveBooking: (Booking) -> Unit,
    onDeleteBooking: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var origin by remember { mutableStateOf("الرياض") }
    var destination by remember { mutableStateOf("جدة") }
    var selectedBookingType by remember { mutableStateOf("جوي ✈️") }
    var travelDate by remember { mutableStateOf("2026-08-01") }
    var passengersCount by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Calculate dynamic estimated price based on type & passengers
    val basePrice = when (selectedBookingType) {
        "جوي ✈️" -> 450.0
        "بري 🚌" -> 120.0
        "بحري 🚢" -> 1800.0
        else -> 350.0
    }
    val calculatedPrice = basePrice * passengersCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(GoldAccent, CyanGlow))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                                imageVector = Icons.Outlined.FlightTakeoff,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "نموذج حجز وكالة الذئب الرقمي ✈️",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
                        ) {
                            Text(
                                text = "محفوظة في Room 💾",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "قم بتعبئة بيانات رحلتك لحفظها محلياً في قاعدة البيانات وسحب التذكرة الرقمية تحت نظام الضمان المالي.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // 1. Select Booking Type Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. اختر نوع الحجز والسفر:",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                )

                val bookingTypes = listOf("جوي ✈️", "بري 🚌", "بحري 🚢")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bookingTypes.forEach { type ->
                        val isSelected = selectedBookingType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { selectedBookingType = type },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldAccent.copy(alpha = 0.2f) else CyberCardBg
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GoldAccent else CyberCardBorder
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldAccent else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Origin & Destination Inputs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. مسار الرحلة (المنطلق والوجهة):",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("مدينة المغادرة (المنطلق)", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = CyanGlow)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberCardBg,
                            unfocusedContainerColor = CyberCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("الوجهة المطلوبة", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = GoldAccent)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberCardBg,
                            unfocusedContainerColor = CyberCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
        }

        // 3. Interactive Date Picker & Quick Date Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. اختيار تاريخ السفر (مواعيد تفاعلية):",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Date Display Field with Picker Trigger
                OutlinedCard(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = CyanGlow
                            )
                            Column {
                                Text(
                                    text = "تاريخ السفر المحدد",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = travelDate,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyanGlow.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "تغيير التاريخ 📅",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quick Date Choice Chips
                Text(
                    text = "مواعيد مقترحة سريعة:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )

                val quickDates = listOf("2026-08-01", "2026-08-05", "2026-08-10", "2026-08-15", "2026-09-01")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickDates) { date ->
                        val isSelected = travelDate == date
                        FilterChip(
                            selected = isSelected,
                            onClick = { travelDate = date },
                            label = {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.25f),
                                selectedLabelColor = GoldAccent,
                                containerColor = CyberCardBg,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = CyberCardBorder,
                                selectedBorderColor = GoldAccent
                            )
                        )
                    }
                }
            }
        }

        // 4. Passenger Stepper & Price Calculation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
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
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = GoldAccent)
                            Text(
                                text = "عدد المسافرين / الأفراد:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Stepper Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (passengersCount > 1) passengersCount-- },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberDarkBg)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "تخفيض", tint = TextPrimary)
                            }

                            Text(
                                text = "$passengersCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent
                                )
                            )

                            IconButton(
                                onClick = { if (passengersCount < 20) passengersCount++ },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberDarkBg)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة", tint = TextPrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = CyberCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "التكلفة المحتسبة بالضمان:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f", calculatedPrice)} SAR",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldAccent.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "الضمان الذكي Escrow 🔒",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Save to Room Button
        item {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val escrowCode = "ESC-ROOM-${Random.nextInt(100000, 999999)}"
                    val ticketCode = "RFQ-TRV-${Random.nextInt(1000, 9999)}"
                    val newEntity = Booking(
                        origin = origin.ifBlank { "الرياض" },
                        destination = destination.ifBlank { "جدة" },
                        travelDate = travelDate,
                        bookingType = selectedBookingType,
                        passengersCount = passengersCount,
                        notes = notes,
                        priceSar = calculatedPrice,
                        escrowId = escrowCode,
                        ticketRef = ticketCode
                    )
                    onSaveBooking(newEntity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Text(
                        text = "حفظ الحجز في قاعدة البيانات Room 💾",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }

        // 6. Display List of Saved Bookings from Room
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الحجوزات المحفوظة محلياً (Room Database):",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Surface(
                    shape = CircleShape,
                    color = CyanGlow.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${savedBookings.size} حجز",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanGlow,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        if (savedBookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ConfirmationNumber,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد حجوزات محفوظة في Room حتى الآن.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            text = "قم بإدخال البيانات واضغط على زر الحفظ لإضافتها لقاعدة البيانات المحلية.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(savedBookings, key = { it.id }) { booking ->
                SavedBookingItemCard(
                    booking = booking,
                    onDelete = { onDeleteBooking(booking.id) }
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            travelDate = formatter.format(Date(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("تأكيد الموعد 📅", color = GoldAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CyberDarkBg)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CyberDarkBg,
                    titleContentColor = GoldAccent,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = GoldAccent,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = GoldAccent,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = GoldAccent,
                    todayContentColor = CyanGlow,
                    todayDateBorderColor = CyanGlow
                )
            )
        }
    }
}

@Composable
fun SavedBookingItemCard(
    booking: Booking,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GoldAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = booking.bookingType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = "مرجع: ${booking.ticketRef}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "حذف من Room",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = booking.origin,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "➔",
                        style = MaterialTheme.typography.titleMedium.copy(color = GoldAccent)
                    )
                    Text(
                        text = booking.destination,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.0f", booking.priceSar)} SAR",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            HorizontalDivider(color = CyberCardBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${booking.travelDate} | 👤 ${booking.passengersCount} مسافر",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )

                Text(
                    text = "🔒 ${booking.escrowId}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanGlow,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
