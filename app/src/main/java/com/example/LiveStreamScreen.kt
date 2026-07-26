package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    repository: LiveStreamRepository = remember { LiveStreamRepository() },
    onCloseScreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission states
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    // CameraX settings
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isMuted by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Stream State
    var isBroadcasting by remember { mutableStateOf(false) }
    var currentStreamId by remember { mutableStateOf("") }
    var streamTitle by remember { mutableStateOf("🔥 البث المباشر الفاخر للـ VIP - حسم خاص للمشاهدين!") }
    var streamCategory by remember { mutableStateOf("تسوق مباشر") }
    var viewerCount by remember { mutableIntStateOf(1420) }
    var likesCount by remember { mutableIntStateOf(3850) }
    var durationSeconds by remember { mutableIntStateOf(0) }

    // Comments & Reactions
    var commentText by remember { mutableStateOf("") }
    var commentsList by remember { mutableStateOf<List<LiveComment>>(emptyList()) }
    val commentsListState = rememberLazyListState()

    // Floating heart reaction state
    var floatingHeartsCount by remember { mutableIntStateOf(0) }

    // Timer coroutine when broadcasting
    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            durationSeconds = 0
            while (isBroadcasting) {
                delay(1000)
                durationSeconds++
                // Random viewer fluctuation
                if (durationSeconds % 5 == 0) {
                    viewerCount += (-10..15).random()
                    if (viewerCount < 100) viewerCount = 100
                }
            }
        }
    }

    // Observe Firestore Comments when broadcasting or viewing live
    LaunchedEffect(currentStreamId) {
        if (currentStreamId.isNotEmpty()) {
            repository.observeComments(currentStreamId).collect { list ->
                commentsList = list
                if (list.isNotEmpty()) {
                    commentsListState.animateScrollToItem(list.size - 1)
                }
            }
        } else {
            commentsList = repository.getSampleComments()
        }
    }

    // Auto scroll comments on start
    LaunchedEffect(commentsList.size) {
        if (commentsList.isNotEmpty()) {
            commentsListState.animateScrollToItem(commentsList.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_stream_screen")
            .background(Color.Black)
    ) {
        // Camera Preview Layer (CameraX)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(previewView.context))
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback Request Permission View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "إذن الكاميرا والمايكروفون مطلوب 📹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "لبدء البث المباشر التفاعلي وتواصلك مع المتابعين، يرجى منح إذن استخدام الكاميرا والميكروفون.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("منح الصلاحيات للبدء")
                        }
                    }
                }
            }
        }

        // Overlay Semi-Transparent Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Floating Hearts Animation Container
        RepeatFloatingHearts(
            triggerCount = floatingHeartsCount,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 24.dp)
        )

        // Main Overlay UI Structure
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Host info, live status badge, camera controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Host Avatar + Live Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        // VIP Crown Icon
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "VIP",
                                tint = Color.White,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "المدرب VIP 👑",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,d", viewerCount)} مشاهد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Center Live Badge or Duration
                if (isBroadcasting) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            val minutes = durationSeconds / 60
                            val seconds = durationSeconds % 60
                            Text(
                                text = "مباشر ${String.format(Locale.US, "%02d:%02d", minutes, seconds)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Camera Action Controls Row (Flip Camera, Flash, Mute)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "تبديل الكاميرا", tint = Color.White)
                    }

                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isMuted) Color(0xFFEF4444) else Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "كتم الصوت",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onCloseScreen,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }
            }

            // MIDDLE / BOTTOM AREA
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If not broadcasting yet: Creator Start Stream Card
                if (!isBroadcasting) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("إعداد البث المباشر 🎥", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = streamTitle,
                                onValueChange = { streamTitle = it },
                                label = { Text("عنوان البث المباشر", color = Color.LightGray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            Button(
                                onClick = {
                                    isBroadcasting = true
                                    coroutineScope.launch {
                                        val newStream = LiveStream(
                                            title = streamTitle,
                                            category = streamCategory,
                                            isLive = true
                                        )
                                        val result = repository.createLiveStream(newStream)
                                        if (result.isSuccess) {
                                            currentStreamId = result.getOrDefault("")
                                            Toast.makeText(context, "تم إطلاق البث المباشر بنجاح! 🚀🔴", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("live_start_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بدء البث المباشر الآن", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Pinned Featured Product Banner during Live Broadcast
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Column {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "عرض البث المباشر 🛍️",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Text(
                                        text = "خنجر الرفيق الملكي الأصيل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "350 SAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "تم أخذ الكوبون وشراء المنتج أثناء البث! 🛍️✨", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("شراء فورياً", fontSize = 12.sp)
                            }
                        }
                    }

                    // End Stream Button
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (currentStreamId.isNotEmpty()) {
                                    repository.endLiveStream(currentStreamId)
                                }
                                isBroadcasting = false
                                Toast.makeText(context, "تم إنهاء البث المباشر 🏁", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنهاء البث المباشر", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }

                // LIVE COMMENTS OVERLAY (LazyColumn)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = commentsListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(commentsList) { comment ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = comment.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (comment.isVip) Color(0xFFF59E0B) else Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                if (comment.giftType.isNotEmpty()) {
                                    Text(text = comment.giftType, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // BOTTOM COMMENT INPUT & REACTION BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("أكتب تعليقاً في البث...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_comment_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commentText.isNotBlank()) {
                                val newComment = LiveComment(
                                    senderName = "أنا (VIP)",
                                    message = commentText,
                                    isVip = true
                                )
                                coroutineScope.launch {
                                    if (currentStreamId.isNotEmpty()) {
                                        repository.sendComment(currentStreamId, newComment)
                                    } else {
                                        commentsList = commentsList + newComment
                                    }
                                    commentText = ""
                                }
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                val newComment = LiveComment(
                                    senderName = "أنا (VIP)",
                                    message = commentText,
                                    isVip = true
                                )
                                coroutineScope.launch {
                                    if (currentStreamId.isNotEmpty()) {
                                        repository.sendComment(currentStreamId, newComment)
                                    } else {
                                        commentsList = commentsList + newComment
                                    }
                                    commentText = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("live_send_comment_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Like / Floating Heart Button
                    IconButton(
                        onClick = {
                            floatingHeartsCount++
                            likesCount++
                            coroutineScope.launch {
                                if (currentStreamId.isNotEmpty()) {
                                    repository.sendLike(currentStreamId)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "إعجاب", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatFloatingHearts(
    triggerCount: Int,
    modifier: Modifier = Modifier
) {
    if (triggerCount <= 0) return

    val transition = rememberInfiniteTransition()
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -250f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier
                .offset(y = offsetY.dp)
                .scale(1.2f)
                .size(32.dp)
        )
    }
}
