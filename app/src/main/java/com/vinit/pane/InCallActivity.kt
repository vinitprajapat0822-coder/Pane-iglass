package com.vinit.pane

import android.telecom.Call
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.speech.tts.TextToSpeech
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.roundToInt

private val CallBg = Color(0xFF0D111B)
private val CallCard = Color(0xFF1B2333)
private val CallText = Color(0xFFF0F3FF)
private val CallMuted = Color(0xFFADB8D0)
private val CallAccent = Color(0xFF39D98A)
private val CallRed = Color(0xFFFF667A)

private fun decodeCallImage(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1440
): androidx.compose.ui.graphics.ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setCallWindowMode(
            getSharedPreferences("pane_settings", Context.MODE_PRIVATE).getString("incoming_style", "Fullscreen") == "Banner" &&
                PaneCallController.state == Call.STATE_RINGING
        )
        setContent { MaterialTheme { androidx.compose.runtime.CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.12.sp)) { InCallScreen() } } }
    }

    fun setCallWindowMode(banner: Boolean) {
        runCatching {
            if (banner) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                val params = window.attributes
                params.width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
                params.height = (resources.displayMetrics.density * 450f).toInt()
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.y = (resources.displayMetrics.density * 18f).toInt()
                params.dimAmount = 0.45f
                window.attributes = params
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InCallScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var glassTarget by remember { mutableStateOf<String?>(null) }
    val showGlass: (String) -> Unit = { label ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        glassTarget = label
    }
    val prefs = remember(context) { context.getSharedPreferences("pane_settings", Context.MODE_PRIVATE) }
    val slideToAnswer = prefs.getString("call_buttons", "Classic dual buttons") == "Slide to answer"
    val announceCaller = prefs.getBoolean("Call Announcer", false)
    val flashOnCall = prefs.getBoolean("Flash On Call", false)
    val backgroundUri = prefs.getString("call_background_uri", null)
    val backgroundType = prefs.getString("call_background_type", "image") ?: "image"
    val showContactPhoto = prefs.getBoolean("Contact Photo on Call", false)
    val call by remember { derivedStateOf { PaneCallController.call } }
    val state by remember { derivedStateOf { PaneCallController.state } }
    val name by remember { derivedStateOf { PaneCallController.displayName } }
    val number by remember { derivedStateOf { PaneCallController.number } }
    val callEndSequence by remember { derivedStateOf { PaneCallController.callEndSequence } }
    val lastEndedName by remember { derivedStateOf { PaneCallController.lastEndedName } }
    val lastEndedNumber by remember { derivedStateOf { PaneCallController.lastEndedNumber } }
    val initialCallEndSequence = remember { PaneCallController.callEndSequence }
    var showPostCall by remember { mutableStateOf(false) }
    val hostActivity = context as? InCallActivity
    val fakeCall = remember(hostActivity) { hostActivity?.intent?.getBooleanExtra("pane_fake_call", false) == true }
    val fakeName = remember(hostActivity) { hostActivity?.intent?.getStringExtra("pane_fake_name").orEmpty().ifBlank { "Unknown caller" } }
    val fakeNumber = remember(hostActivity) { hostActivity?.intent?.getStringExtra("pane_fake_number").orEmpty() }
    val fakeAnswered = remember { mutableStateOf(false) }
    val displayName = if (fakeCall) fakeName else name
    val displayNumber = if (fakeCall) fakeNumber else number
    LaunchedEffect(callEndSequence, fakeCall) {
        if (!fakeCall && callEndSequence > initialCallEndSequence) {
            if (prefs.getBoolean("Call Back Screen", true) && lastEndedNumber.isNotBlank()) {
                showPostCall = true
                hostActivity?.setCallWindowMode(false)
            } else {
                hostActivity?.finish()
            }
        }
    }
    val callBackgroundBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, backgroundUri, backgroundType) {
        value = if (backgroundUri != null && backgroundType != "video") {
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeCallImage(context, Uri.parse(backgroundUri))
                }.getOrNull()
            }
        } else null
    }
    val contactPhotoUri by produceState<String?>(null, displayNumber, showContactPhoto, fakeCall) {
        value = if (showContactPhoto && !fakeCall && displayNumber.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val lookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(displayNumber))
                    context.contentResolver.query(lookup, arrayOf(ContactsContract.Contacts.PHOTO_URI), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull()
            }
        } else null
    }
    val contactPhotoBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, contactPhotoUri) {
        value = if (!contactPhotoUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeCallImage(context, Uri.parse(contactPhotoUri))
                }.getOrNull()
            }
        } else null
    }
    val muted by remember { derivedStateOf { PaneCallController.muted } }
    val speaker by remember { derivedStateOf { PaneCallController.speaker } }
    val ringing = state == Call.STATE_RINGING || (fakeCall && !fakeAnswered.value)
    val bannerMode = (prefs.getString("incoming_style", "Fullscreen") ?: "Fullscreen") == "Banner" && ringing
    LaunchedEffect(bannerMode) { hostActivity?.setCallWindowMode(bannerMode) }
    DisposableEffect(announceCaller, ringing, displayName, displayNumber) {
        var engine: TextToSpeech? = null
        if (announceCaller && ringing) {
            engine = TextToSpeech(context) { result ->
                if (result == TextToSpeech.SUCCESS) {
                    engine?.let { tts ->
                        tts.language = Locale.getDefault()
                        val caller = if (displayName.isBlank() || displayName == displayNumber) displayNumber else displayName
                        tts.speak("Incoming call from $caller", TextToSpeech.QUEUE_FLUSH, null, "pane-caller-announcement")
                    }
                }
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }
    LaunchedEffect(ringing, flashOnCall) {
        if (ringing && flashOnCall &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = runCatching {
                cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }.getOrNull()
            if (cameraId != null) {
                try {
                    while (isActive) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(450)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(450)
                    }
                } finally {
                    runCatching { cameraManager.setTorchMode(cameraId, false) }
                }
            }
        }
    }
    val active = state == Call.STATE_ACTIVE || (fakeCall && fakeAnswered.value)
    val status = if (fakeCall) {
        if (fakeAnswered.value) "SIMULATED CALL ACTIVE" else "SIMULATED INCOMING CALL"
    } else when (state) {
        Call.STATE_RINGING -> "INCOMING CALL"
        Call.STATE_DIALING -> "CALLING…"
        Call.STATE_CONNECTING -> "CONNECTING…"
        Call.STATE_ACTIVE -> "CONNECTED"
        Call.STATE_HOLDING -> "ON HOLD"
        Call.STATE_DISCONNECTING -> "ENDING…"
        else -> if (call == null) "NO ACTIVE CALL" else "CALL STATUS"
    }

    Surface(Modifier.fillMaxSize(), color = if (bannerMode) Color.Transparent else CallBg) {
        Box(
            Modifier.fillMaxSize().then(
                if (bannerMode) Modifier.clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xF21F2B40), Color(0xF50D111B))))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(28.dp))
                else Modifier.background(CallBg)
            )
        ) {
            Box(Modifier.fillMaxSize().blur(if (glassTarget != null) 8.dp else 0.dp)) {
            if (!bannerMode) {
                if (backgroundType == "video" && !backgroundUri.isNullOrBlank()) {
                    LoopingVideoBackground(backgroundUri)
                } else if (callBackgroundBitmap != null) {
                    Image(
                        bitmap = callBackgroundBitmap!!,
                        contentDescription = "Selected call background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xB90D111B), Color(0x8A0D111B), Color(0xF20D111B)))
                    )
                )
            }
            Box(Modifier.fillMaxSize().padding(if (bannerMode) 16.dp else 24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("P A N E", color = Color(0xFFB9C5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(if (bannerMode) 8.dp else 42.dp))
                Box(Modifier.size(if (bannerMode) 64.dp else 104.dp).clip(CircleShape).background(Color(0xFF293750)), contentAlignment = Alignment.Center) {
                    if (contactPhotoBitmap != null) {
                        Image(bitmap = contactPhotoBitmap!!, contentDescription = "Caller photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(displayName.take(1).uppercase(), color = CallText, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(if (bannerMode) 8.dp else 22.dp))
                Text(displayName, color = CallText, fontSize = if (bannerMode) 20.sp else 25.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                Text(displayNumber, color = CallMuted, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(status, color = CallMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(if (bannerMode) 14.dp else 52.dp))
                if (showPostCall) {
                    Text("CALL ENDED", color = CallAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(lastEndedName.ifBlank { "Last caller" }, color = CallText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                    Text(lastEndedNumber, color = CallMuted, fontSize = 15.sp)
                    Spacer(Modifier.height(26.dp))
                    CallAction("Call back", CallAccent, onLongPress = { showGlass("Call back") }) {
                        val numberToCall = lastEndedNumber
                        hostActivity?.finish()
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numberToCall, null))) }
                    }
                    Spacer(Modifier.height(10.dp))
                    CallAction("Close", CallCard, onLongPress = { showGlass("Close callback screen") }) { hostActivity?.finish() }
                } else if ((active || state == Call.STATE_HOLDING) && !fakeCall) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CallAction(if (muted) "Unmute" else "Mute", if (muted) CallAccent else CallCard, onLongPress = { showGlass(if (muted) "Unmute" else "Mute") }) { PaneCallController.toggleMute() }
                        CallAction(if (speaker) "Speaker on" else "Speaker", if (speaker) CallAccent else CallCard, onLongPress = { showGlass("Speaker control") }) { PaneCallController.toggleSpeaker() }
                    }
                    Spacer(Modifier.height(26.dp))
                    CallAction("End call", CallRed, onLongPress = { showGlass("End call") }) { PaneCallController.end() }
                } else if (fakeCall && fakeAnswered.value) {
                    CallAction("End simulated call", CallRed, onLongPress = { showGlass("End simulated call") }) { hostActivity?.finish() }
                } else if (ringing) {
                    if (slideToAnswer) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CallAction("Decline", CallRed, onLongPress = { showGlass("Decline call") }) { if (fakeCall) hostActivity?.finish() else { PaneCallController.reject(); hostActivity?.finish() } }
                            Spacer(Modifier.height(18.dp))
                            SlideToAnswer { if (fakeCall) fakeAnswered.value = true else PaneCallController.answer() }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            CallAction("Decline", CallRed, onLongPress = { showGlass("Decline call") }) { if (fakeCall) hostActivity?.finish() else { PaneCallController.reject(); hostActivity?.finish() } }
                            CallAction("Answer", CallAccent, onLongPress = { showGlass("Answer call") }) { if (fakeCall) fakeAnswered.value = true else PaneCallController.answer() }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    CallAction("Quick response", CallCard, onLongPress = { showGlass("Quick response") }) {
                        val message = prefs.getString("quick_response", "Can't talk right now.") ?: "Can't talk right now."
                        runCatching {
                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", displayNumber, null))
                                .putExtra("sms_body", message)
                            context.startActivity(smsIntent)
                        }
                        if (fakeCall) hostActivity?.finish() else { PaneCallController.reject(); hostActivity?.finish() }
                    }
                } else if (call != null) {
                    CallAction("End call", CallRed, onLongPress = { showGlass("End call") }) { PaneCallController.end() }
                } else {
                    Text("Call screen opens when Android Telecom delivers a call.", color = CallMuted, textAlign = TextAlign.Center)
                }
                }
            }
            }
            if (glassTarget != null) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { glassTarget = null })
                AnimatedVisibility(
                    visible = glassTarget != null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.78f, stiffness = 450f)) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f)) + fadeOut()
                ) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xF5FFFFFF), Color(0xE8202C42), Color(0xF0111723))))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Box(Modifier.width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)).align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        Text(glassTarget ?: "Pane", color = Color(0xFF101827), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text("LIQUID GLASS · HAPTIC FEEDBACK", color = Color(0xFF54627B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Spacer(Modifier.height(14.dp))
                        Text("Held control · translucent glass surface", color = Color(0xFF26334B), fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        Text("CLOSE", color = Color(0xFF5368BD), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).clickable { glassTarget = null }.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoopingVideoBackground(uri: String) {
    val context = LocalContext.current
    val view = remember(uri) {
        VideoView(context).apply {
            setVideoURI(Uri.parse(uri))
            setOnPreparedListener { player ->
                player.isLooping = true
                player.setVolume(0f, 0f)
                start()
            }
            setOnErrorListener { _, _, _ -> true }
        }
    }
    DisposableEffect(view) {
        onDispose { runCatching { view.stopPlayback() } }
    }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize(), update = { })
}

@Composable
private fun SlideToAnswer(onAnswer: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF173C35)).border(1.dp, CallAccent.copy(alpha = 0.55f), RoundedCornerShape(34.dp))
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxOffset = with(LocalDensity.current) { (maxWidth - 66.dp).toPx() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Slide to answer", color = CallText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(54.dp).clip(CircleShape).background(CallAccent)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(0f, maxOffset)) }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offset.value >= maxOffset * 0.78f) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAnswer()
                                    offset.snapTo(0f)
                                } else {
                                    offset.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 500f))
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Call, contentDescription = "Slide to answer", tint = CallBg, modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallAction(
    label: String,
    color: Color,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(color)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress?.invoke()
                }
            )
            .padding(horizontal = 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (color == CallAccent) CallBg else CallText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
