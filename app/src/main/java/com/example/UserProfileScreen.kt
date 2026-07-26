package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
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
fun UserProfileScreen(
    repository: UserProfileRepository = remember { UserProfileRepository() },
    onOpenAuthDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // Listen to Firebase Auth state
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
        UnauthenticatedProfileState(onOpenAuthDialog = onOpenAuthDialog)
    } else {
        AuthenticatedProfileState(
            user = currentUser!!,
            repository = repository,
            auth = auth
        )
    }
}

@Composable
private fun UnauthenticatedProfileState(
    onOpenAuthDialog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen")
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
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "الملف الشخصي",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "مرحباً بك في رفيق",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "سجّل الدخول لعرض وتحديث بيانات ملفك الشخصي والاستفادة من الميزات المتقدمة.",
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
                        .testTag("open_auth_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الدخول / إنشاء حساب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedProfileState(
    user: FirebaseUser,
    repository: UserProfileRepository,
    auth: FirebaseAuth
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var profileState by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // Editable form fields
    var editDisplayName by remember { mutableStateOf("") }
    var editPhotoUrl by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    // Observe Firestore UserProfile in real-time
    LaunchedEffect(user.uid) {
        repository.observeUserProfile(user.uid).collect { firestoreProfile ->
            isLoading = false
            val current = firestoreProfile ?: UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            profileState = current
            if (!isEditing) {
                editDisplayName = current.displayName.ifBlank { user.displayName ?: "" }
                editPhotoUrl = current.photoUrl.ifBlank { user.photoUrl?.toString() ?: "" }
                editPhone = current.phone
                editBio = current.bio
            }
        }
    }

    val displayProfile = profileState ?: UserProfile(
        uid = user.uid,
        displayName = user.displayName ?: "مستخدم رفيق",
        email = user.email ?: "",
        photoUrl = user.photoUrl?.toString() ?: ""
    )

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }

    val isVipActive = displayProfile.isVipActive()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Profile Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar Box with VIP Golden Badge Overlay
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isVipActive) 3.dp else 0.dp,
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00), Color(0xFFFFD700))
                                ),
                                shape = CircleShape
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = if (isVipActive)
                                        listOf(Color(0xFFFFD700), Color(0xFFD97706))
                                    else
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = displayProfile.photoUrl.ifBlank { user.photoUrl?.toString() ?: "" }
                        if (photoUrl.isNotBlank()) {
                            CoilAsyncImage(
                                imageUrl = photoUrl,
                                contentDescription = displayProfile.displayName,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape
                            )
                        } else {
                            Text(
                                text = (displayProfile.displayName.firstOrNull() ?: user.email?.firstOrNull() ?: 'U')
                                    .uppercaseChar().toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Floating Crown / VIP Badge
                    if (isVipActive) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD700),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .testTag("vip_avatar_badge")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "عضوية VIP",
                                    tint = Color(0xFF1F1200),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Name & Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayProfile.displayName.ifBlank { user.displayName ?: "مستخدم رفيق" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (isVipActive) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "VIP Badge",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = displayProfile.email.ifBlank { user.email ?: "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Role & Verification Status Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isVipActive) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isVipActive) Color(0xFFF59E0B) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isVipActive) Icons.Default.WorkspacePremium else Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (isVipActive) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isVipActive -> "عضو VIP ذهبي 👑"
                                    displayProfile.role.equals("MERCHANT", ignoreCase = true) -> "تاجر معتمد"
                                    else -> "عضو رفيق"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isVipActive) Color(0xFFB45309) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Email Verification Status
                    val isVerified = user.isEmailVerified
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isVerified) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isVerified) "موثق" else "غير موثق",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isVerified) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }

        // VIP Membership Status Card
        VipMembershipCard(
            userProfile = displayProfile,
            repository = repository,
            userId = user.uid
        )

        // Details / Edit Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل الحساب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.testTag("edit_profile_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "إلغاء التعديل" else "تعديل الملف الشخصي",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditing) "إلغاء" else "تعديل الملف الشخصي",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isEditing) {
                    // Edit Form
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("الاسم الكامل") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editPhotoUrl,
                        onValueChange = { editPhotoUrl = it },
                        label = { Text("رابط صورة الملف الشخصي (Image URL)") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_photo_url_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("نبذة شخصية (Bio)") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                val result = repository.updateProfile(
                                    uid = user.uid,
                                    displayName = editDisplayName,
                                    photoUrl = editPhotoUrl,
                                    phone = editPhone,
                                    bio = editBio
                                )
                                isSaving = false
                                if (result.isSuccess) {
                                    isEditing = false
                                    Toast.makeText(context, "تم تحديث الملف الشخصي بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "فشل التحديث: ${result.exceptionOrNull()?.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ التحديثات", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Display View
                    ProfileDetailItem(
                        icon = Icons.Default.Person,
                        label = "الاسم الكامل",
                        value = displayProfile.displayName.ifBlank { "غير محدد" }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ProfileDetailItem(
                        icon = Icons.Default.Email,
                        label = "البريد الإلكتروني",
                        value = displayProfile.email.ifBlank { user.email ?: "غير محدد" }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ProfileDetailItem(
                        icon = Icons.Default.Phone,
                        label = "رقم الهاتف",
                        value = displayProfile.phone.ifBlank { "لم يُضاف رقم هاتف" }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ProfileDetailItem(
                        icon = Icons.Default.Info,
                        label = "نبذة شخصية",
                        value = displayProfile.bio.ifBlank { "لا توجد نبذة شخصية بعد" }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ProfileDetailItem(
                        icon = Icons.Default.CalendarToday,
                        label = "تاريخ الانضمام",
                        value = dateFormat.format(Date(displayProfile.createdAt))
                    )
                }
            }
        }

        // Account Actions
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!user.isEmailVerified && user.email != null) {
                    OutlinedButton(
                        onClick = {
                            user.sendEmailVerification().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "تم إرسال رابط التوثيق إلى بريدك", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "فشل الإرسال: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال رابط توثيق البريد الإلكتروني")
                    }
                }

                Button(
                    onClick = {
                        auth.signOut()
                        Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "تسجيل الخروج")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VipMembershipCard(
    userProfile: UserProfile,
    repository: UserProfileRepository,
    userId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("YEARLY") } // "MONTHLY", "YEARLY"
    val isVip = userProfile.isVipActive()

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vip_membership_card")
            .border(
                width = if (isVip) 2.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    if (isVip) listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00))
                    else listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    if (isVip) listOf(Color(0xFF2A1B03), Color(0xFF1F1200))
                    else listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isVip) Color(0xFFFFD700) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVip) Icons.Default.WorkspacePremium else Icons.Outlined.MilitaryTech,
                                contentDescription = "VIP",
                                tint = if (isVip) Color(0xFF1F1200) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isVip) "عضوية VIP الذهبية النشطة 👑" else "ترقية إلى عضوية VIP 🌟",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVip) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isVip)
                                (if (userProfile.vipPlanName.isNotBlank()) userProfile.vipPlanName else "باقة VIP المتميزة")
                            else "احصل على ميزات استثنائية وتجربة فاخرة",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isVip) Color(0xFFFFE082) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isVip) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (isVip) "نشط الأن" else "غير مفعل",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isVip) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isVip) {
                // Active VIP View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاريخ انتهاء الاشتراك:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFECB3)
                        )
                        Text(
                            text = if (userProfile.vipExpirationDate > 0) dateFormat.format(Date(userProfile.vipExpirationDate)) else "عضوية دائمية",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    Text(
                        text = "المميزات المتاحة لحسابك:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    VipFeatureItem(icon = Icons.Default.Verified, text = "شارة VIP الذهبية بجانب اسمك في كافة أقسام التطبيق", isGold = true)
                    VipFeatureItem(icon = Icons.Default.FastForward, text = "أولوية دعم العملاء والرد الفوري 24/7", isGold = true)
                    VipFeatureItem(icon = Icons.Default.Discount, text = "خصم حصري 20% على رسوم المزاد والمنتجات", isGold = true)
                    VipFeatureItem(icon = Icons.Default.Block, text = "تصفح كامل وخالٍ تماماً من الإعلانات", isGold = true)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isUpdating = true
                                val result = repository.updateVipSubscription(
                                    uid = userId,
                                    isVip = true,
                                    planName = "تجديد باقة VIP السنوية 👑",
                                    durationDays = 365
                                )
                                isUpdating = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "تم تمديد اشتراك VIP لمدة سنة بنجاح! 👑", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700))
                    ) {
                        Text("تمديد سنة إضافية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                isUpdating = true
                                val result = repository.updateVipSubscription(
                                    uid = userId,
                                    isVip = false
                                )
                                isUpdating = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "تم إلغاء تفعيل VIP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isUpdating,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("إلغاء التفعيل", fontSize = 12.sp)
                    }
                }
            } else {
                // Non-VIP Offer View
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VipFeatureItem(icon = Icons.Default.Star, text = "شارة ذهبية موثقة تظهر لدى جميع المستخدمين", isGold = false)
                    VipFeatureItem(icon = Icons.Default.Speed, text = "تسريع وتفضيل إعلاناتك ومحتوى المتاجر", isGold = false)
                    VipFeatureItem(icon = Icons.Default.Percent, text = "خصم خاص 20% على عمولات الشراء والمزادات", isGold = false)
                    VipFeatureItem(icon = Icons.Default.HeadsetMic, text = "قناة تواصل مباشرة ومخصصة لمشتركي VIP", isGold = false)
                }

                // Plan Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Monthly Plan
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPlan == "MONTHLY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedPlan == "MONTHLY") MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPlan = "MONTHLY" }
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الباقة الشهرية", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("9.99$ / شهر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Yearly Plan
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPlan == "YEARLY") Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedPlan == "YEARLY") Color(0xFFD97706) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPlan = "YEARLY" }
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الباقة السنوية (توفير 30%)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Text("79.99$ / سنة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }
                }

                // Upgrade Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isUpdating = true
                            val planName = if (selectedPlan == "YEARLY") "باقة VIP السنوية 👑" else "باقة VIP الشهرية 👑"
                            val durationDays = if (selectedPlan == "YEARLY") 365 else 30
                            val result = repository.updateVipSubscription(
                                uid = userId,
                                isVip = true,
                                planName = planName,
                                durationDays = durationDays
                            )
                            isUpdating = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "تهانينا! تم تفعيل اشتراك VIP بنجاح 🎉", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "حدث خطأ أثناء التفعيل: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_vip_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تفعيل اشتراك VIP الآن 👑", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VipFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isGold: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGold) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGold) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
