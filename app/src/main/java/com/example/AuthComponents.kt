package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun AuthDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            AuthCardContent(onDismissRequest = onDismissRequest)
        }
    }
}

@Composable
fun AuthCardContent(
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // Listen for auth state changes
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser != null) {
        UserProfileView(
            user = currentUser!!,
            auth = auth,
            onDismiss = onDismissRequest
        )
    } else {
        AuthFormView(
            auth = auth,
            onDismiss = onDismissRequest
        )
    }
}

@Composable
fun UserProfileView(
    user: FirebaseUser,
    auth: FirebaseAuth,
    onDismiss: (() -> Unit)?
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حساب المستخدم",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        }

        // User Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!user.photoUrl?.toString().isNullOrBlank()) {
                CoilAsyncImage(
                    imageUrl = user.photoUrl.toString(),
                    contentDescription = user.displayName ?: "صورة الملف الشخصي",
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                )
            } else {
                Text(
                    text = (user.displayName?.firstOrNull() ?: user.email?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = user.displayName ?: "مستخدم رفيق",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email ?: "لا يوجد بريد إلكتروني",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Email Verification Status
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (user.isEmailVerified) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (user.isEmailVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (user.isEmailVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (user.isEmailVerified) "البريد الإلكتروني موثق" else "البريد غير موثق",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (user.isEmailVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!user.isEmailVerified && user.email != null) {
            OutlinedButton(
                onClick = {
                    user.sendEmailVerification().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "تم إرسال رابط التوثيق للبريد الإلكتروني", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "فشل الإرسال: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة إرسال رابط التوثيق")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Sign Out Button
        Button(
            onClick = {
                auth.signOut()
                Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                onDismiss?.invoke()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = "تسجيل الخروج")
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AuthFormView(
    auth: FirebaseAuth,
    onDismiss: (() -> Unit)?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when (authMode) {
                        AuthMode.SIGN_IN -> "تسجيل الدخول"
                        AuthMode.SIGN_UP -> "إنشاء حساب جديد"
                        AuthMode.FORGOT_PASSWORD -> "استعادة كلمة المرور"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        }

        // Auth Mode Switcher Segmented Buttons
        if (authMode != AuthMode.FORGOT_PASSWORD) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (authMode == AuthMode.SIGN_IN) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable { authMode = AuthMode.SIGN_IN; errorMessage = null }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تسجيل الدخول",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Normal,
                        color = if (authMode == AuthMode.SIGN_IN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (authMode == AuthMode.SIGN_UP) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable { authMode = AuthMode.SIGN_UP; errorMessage = null }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "حساب جديد",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (authMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Normal,
                        color = if (authMode == AuthMode.SIGN_UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "خطأ",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Display Name field (only for SIGN_UP)
        if (authMode == AuthMode.SIGN_UP) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("البريد الإلكتروني") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            trailingIcon = {
                if (email.isNotEmpty()) {
                    IconButton(onClick = { email = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = if (authMode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Password Field (if not FORGOT_PASSWORD)
        if (authMode != AuthMode.FORGOT_PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Forgot Password link
        if (authMode == AuthMode.SIGN_IN) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = { authMode = AuthMode.FORGOT_PASSWORD; errorMessage = null }) {
                    Text("نسيت كلمة المرور؟", fontSize = 12.sp)
                }
            }
        }

        // Primary Action Button
        Button(
            onClick = {
                if (email.isBlank()) {
                    errorMessage = "يرجى إدخال البريد الإلكتروني"
                    return@Button
                }

                when (authMode) {
                    AuthMode.SIGN_IN -> {
                        if (password.isBlank()) {
                            errorMessage = "يرجى إدخال كلمة المرور"
                            return@Button
                        }
                        isLoading = true
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "أهلاً بك مجدداً!", Toast.LENGTH_SHORT).show()
                                onDismiss?.invoke()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = translateAuthError(e.message)
                            }
                    }

                    AuthMode.SIGN_UP -> {
                        if (password.length < 6) {
                            errorMessage = "كلمة المرور يجب أن لا تقل عن 6 خانات"
                            return@Button
                        }
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                isLoading = false
                                if (displayName.isNotBlank()) {
                                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build()
                                    result.user?.updateProfile(profileUpdates)
                                }
                                Toast.makeText(context, "تم إنشاء الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                                onDismiss?.invoke()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = translateAuthError(e.message)
                            }
                    }

                    AuthMode.FORGOT_PASSWORD -> {
                        isLoading = true
                        auth.sendPasswordResetEmail(email.trim())
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "تم إرسال تعليمات إعادت التعيين لبريدك", Toast.LENGTH_LONG).show()
                                authMode = AuthMode.SIGN_IN
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = translateAuthError(e.message)
                            }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(if (authMode == AuthMode.SIGN_UP) "register_button" else "login_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = when (authMode) {
                        AuthMode.SIGN_IN -> "تسجيل الدخول"
                        AuthMode.SIGN_UP -> "إنشاء الحساب"
                        AuthMode.FORGOT_PASSWORD -> "إرسال رابط إعادة التعيين"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        if (authMode == AuthMode.FORGOT_PASSWORD) {
            TextButton(onClick = { authMode = AuthMode.SIGN_IN; errorMessage = null }) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(" العودة لتسجيل الدخول")
            }
        }

        // Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                text = " أو من خلال ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(modifier = Modifier.weight(1f))
        }

        // Google Sign-In Button (Credential Manager / Google Identity)
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    performGoogleSignIn(context, auth, onSuccess = {
                        isLoading = false
                        Toast.makeText(context, "تم تسجيل الدخول بواسطة Google!", Toast.LENGTH_SHORT).show()
                        onDismiss?.invoke()
                    }, onError = { err ->
                        isLoading = false
                        errorMessage = err
                    })
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_signin_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Custom Google "G" Icon indicator
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "متابعة باستخدام حساب Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Initiates Google Sign-In via Android Credential Manager
 */
private suspend fun performGoogleSignIn(
    context: Context,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val credentialManager = CredentialManager.create(context)

        // Web Client ID can be injected from BuildConfig or Google Services configuration
        val webClientId = BuildConfig.BUILD_TYPE // Web client ID placeholder or from configuration
        
        // Build GoogleIdOption
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("DEFAULT_WEB_CLIENT_ID")
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val idToken = credential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(translateAuthError(e.message)) }
        } else {
            onError("نوع الاعتماد غير مدعوم")
        }
    } catch (e: Exception) {
        // Handle common CredentialManager / Google auth exceptions gracefully
        val userFriendlyMessage = when {
            e.message?.contains("No credentials available", ignoreCase = true) == true ->
                "لم يتم العثور على حسابات Google مسجلة على الجهاز"
            e.message?.contains("canceled", ignoreCase = true) == true ->
                "تم إلغاء عملية تسجيل الدخول"
            else -> "تنبيه Google Sign-In: ${e.localizedMessage ?: "يتطلب ربط Google Web Client ID"}"
        }
        onError(userFriendlyMessage)
    }
}

private fun translateAuthError(message: String?): String {
    if (message == null) return "حدث خطأ غير متوقع أثناء الاتصال"
    return when {
        message.contains("user-not-found", ignoreCase = true) -> "المستخدم غير موجود. يرجى إنشاء حساب جديد"
        message.contains("wrong-password", ignoreCase = true) || message.contains("invalid-credential", ignoreCase = true) -> "كلمة المرور أو البريد غير صحيح"
        message.contains("email-already-in-use", ignoreCase = true) -> "البريد الإلكتروني مستخدم بالفعل"
        message.contains("invalid-email", ignoreCase = true) -> "صيغة البريد الإلكتروني غير صحيحة"
        message.contains("weak-password", ignoreCase = true) -> "كلمة المرور ضعيفة للغاية"
        message.contains("network", ignoreCase = true) -> "تعذر الاتصال بالشبكة. تحقق من اتصالك"
        else -> message
    }
}
