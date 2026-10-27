package com.vinit.pane

import android.Manifest
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PanePalette(
    val background: Color,
    val panel: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val callGreen: Color
)
private val DarkPalette = PanePalette(
    Color(0xFF0D111B), Color(0xCC1B2333), Color(0xFF39465E),
    Color(0xFFF0F3FF), Color(0xFFADB8D0), Color(0xFFB9C5FF), Color(0xFF39D98A)
)
private val LightPalette = PanePalette(
    Color(0xFFF3F6FC), Color(0xF2FFFFFF), Color(0xFFD4DDEC),
    Color(0xFF172033), Color(0xFF63718A), Color(0xFF5368BD), Color(0xFF16865D)
)
private val LocalPanePalette = compositionLocalOf { DarkPalette }
private val Night: Color @Composable get() = LocalPanePalette.current.background
private val Panel: Color @Composable get() = LocalPanePalette.current.panel
private val Line: Color @Composable get() = LocalPanePalette.current.line
private val Ink: Color @Composable get() = LocalPanePalette.current.ink
private val Muted: Color @Composable get() = LocalPanePalette.current.muted
private val Accent: Color @Composable get() = LocalPanePalette.current.accent
private val CallGreen: Color @Composable get() = LocalPanePalette.current.callGreen

private enum class PaneTab(val label: String) {
    Favorites("Favorites"), Recents("Recents"), Contacts("Contacts"), Keypad("Keypad"), Settings("Settings")
}

internal data class Person(
    val id: String,
    val name: String,
    val number: String,
    val starred: Boolean = false,
    val type: String = "",
    val timestamp: String = "",
    val simLabel: String = ""
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "#" }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PaneApp(this) } }
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    fun isDefaultDialer(): Boolean = DialerRoleHelper.isDefaultDialer(this)

    fun requestDefaultDialer() {
        DialerRoleHelper.createRequestIntent(this)?.let { startActivity(it) }
    }

    internal fun readContacts(): List<Person> {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return emptyList()
        val result = mutableListOf<Person>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.Contacts.STARRED
        )
        runCatching {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberCol).orEmpty()
                    if (number.isNotBlank()) result += Person(
                        cursor.getString(idCol).orEmpty(),
                        cursor.getString(nameCol).orEmpty().ifBlank { number },
                        number,
                        cursor.getInt(starCol) == 1
                    )
                }
            }
        }
        return result.distinctBy { it.id + "|" + it.number }
    }

    internal fun readRecents(): List<Person> {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return emptyList()
        val result = mutableListOf<Person>()
        val projection = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val telecom = getSystemService(TelecomManager::class.java)
        val phoneAccountLabels: Map<String, String> = runCatching {
            telecom.getCallCapablePhoneAccounts().associate { handle ->
                handle.id to (telecom.getPhoneAccount(handle)?.label?.toString() ?: "")
            }
        }.getOrDefault(emptyMap())
        runCatching {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val accountIdCol = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberCol).orEmpty()
                    val name = cursor.getString(nameCol).orEmpty().ifBlank { number.ifBlank { "Unknown caller" } }
                    val type = when (cursor.getInt(typeCol)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else -> "Call"
                    }
                    val timestamp = runCatching { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(cursor.getLong(dateCol))) }.getOrDefault("")
                    val accountId = if (accountIdCol >= 0) cursor.getString(accountIdCol).orEmpty() else ""
                    val simLabel = phoneAccountLabels[accountId].orEmpty()
                    result += Person(cursor.getString(idCol).orEmpty(), name, number, type = type, timestamp = timestamp, simLabel = simLabel)
                }
            }
        }
        return result
    }

    fun openDialer(number: String) {
        val clean = number.trim()
        if (clean.isEmpty()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", clean, null)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaneApp(activity: MainActivity) {
    var selected by remember { mutableStateOf(PaneTab.Favorites) }
    var query by remember { mutableStateOf("") }
    var dialed by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<Person>()) }
    var recents by remember { mutableStateOf(emptyList<Person>()) }
    var recentFilter by remember { mutableStateOf("All") }
    var recentsEditing by remember { mutableStateOf(false) }
    var glassTarget by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val showGlass: (String) -> Unit = { label ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        glassTarget = label
    }
    var permissionState by remember { mutableStateOf(false) }
    var defaultDialer by remember { mutableStateOf(activity.isDefaultDialer()) }
    val prefs = remember(activity) { activity.getSharedPreferences("pane_settings", Context.MODE_PRIVATE) }
    var themeChoice by remember(prefs) { mutableStateOf(prefs.getString("dialer_theme", "System") ?: "System") }
    var buttonsChoice by remember(prefs) { mutableStateOf(prefs.getString("call_buttons", "Classic dual buttons") ?: "Classic dual buttons") }
    var incomingChoice by remember(prefs) { mutableStateOf(prefs.getString("incoming_style", "Fullscreen") ?: "Fullscreen") }
    val useDarkPalette = when (themeChoice) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val palette = if (useDarkPalette) DarkPalette else LightPalette

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                defaultDialer = activity.isDefaultDialer()
                contacts = activity.readContacts()
                recents = activity.readRecents()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        contacts = activity.readContacts()
        recents = activity.readRecents()
        permissionState = true
    }

    fun refresh() {
        contacts = activity.readContacts()
        recents = activity.readRecents()
        permissionState = true
    }

    LaunchedEffect(Unit) {
        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE)
            .filterNot(activity::hasPermission)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else refresh()
    }

    CompositionLocalProvider(LocalPanePalette provides palette) {
    Surface(Modifier.fillMaxSize(), color = Night) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (useDarkPalette) listOf(Color(0xFF19243A), Night, Color(0xFF101827))
            else listOf(Color(0xFFE8EDF7), Night, Color(0xFFF8FAFD))
        ))) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
                .blur(if (glassTarget != null) 8.dp else 0.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("P A N E", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when (selected) {
                                PaneTab.Favorites -> "Your people"
                                PaneTab.Recents -> "Recent calls"
                                PaneTab.Contacts -> "Contacts"
                                PaneTab.Keypad -> "Keypad"
                                PaneTab.Settings -> "Settings"
                            }, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("AURORA GLASS", color = Muted, fontSize = 10.sp)
                }

                Spacer(Modifier.height(12.dp))
                if (!defaultDialer) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)
                            .border(1.dp, Line, RoundedCornerShape(16.dp))
                            .clickable { activity.requestDefaultDialer() }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Set Pane as default phone app", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Android will ask you to approve this change", color = Muted, fontSize = 11.sp)
                        }
                        Text("SET UP", color = Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)
                        .border(1.dp, Line, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search names or numbers", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(14.dp))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selected) {
                        PaneTab.Keypad -> KeypadPanel(
                            dialed = dialed,
                            onDigit = { dialed += it },
                            onDelete = { if (dialed.isNotEmpty()) dialed = dialed.dropLast(1) },
                            onCall = { activity.openDialer(dialed) },
                            onGlassPress = showGlass
                        )
                        PaneTab.Contacts -> PeoplePanel(
                            title = "PHONE CONTACTS",
                            people = contacts.filter { it.name.contains(query, true) || it.number.contains(query, true) },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CONTACTS)) "Contacts permission is needed to show phone contacts." else "No matching contacts.",
                            onCall = activity::openDialer,
                            onGlassPress = showGlass
                        )
                        PaneTab.Favorites -> PeoplePanel(
                            title = "STARRED CONTACTS",
                            people = contacts.filter { it.starred && (it.name.contains(query, true) || it.number.contains(query, true)) },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CONTACTS)) "Allow Contacts permission to load favorites." else "No starred contacts found. Star contacts in your phone's Contacts app.",
                            onCall = activity::openDialer,
                            onGlassPress = showGlass
                        )
                        PaneTab.Recents -> RecentsPanel(
                            people = recents.filter { it.name.contains(query, true) || it.number.contains(query, true) }
                                .filter { recentFilter == "All" || it.type == "Missed" },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CALL_LOG)) "Call-log permission is needed to show recent calls." else "No recent calls found.",
                            onCall = activity::openDialer,
                            filter = recentFilter,
                            onFilterChange = { recentFilter = it },
                            editing = recentsEditing,
                            onEditingChange = { recentsEditing = it },
                            onGlassPress = showGlass
                        )
                        PaneTab.Settings -> SettingsPanel(
                            theme = themeChoice,
                            onThemeChange = { themeChoice = it; prefs.edit().putString("dialer_theme", it).apply() },
                            buttons = buttonsChoice,
                            onButtonsChange = { buttonsChoice = it; prefs.edit().putString("call_buttons", it).apply() },
                            incoming = incomingChoice,
                            onIncomingChange = { incomingChoice = it; prefs.edit().putString("incoming_style", it).apply() },
                            onGlassPress = showGlass
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Panel)
                        .border(1.dp, Line, RoundedCornerShape(24.dp)).padding(7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaneTab.values().forEach { tab ->
                        val active = tab == selected
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (active) Accent.copy(alpha = if (useDarkPalette) 0.22f else 0.14f) else Color.Transparent)
                                .combinedClickable(onClick = { selected = tab }, onLongClick = { showGlass(tab.label) }).padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                when (tab) {
                                    PaneTab.Favorites -> Icons.Default.Favorite
                                    PaneTab.Recents -> Icons.Default.History
                                    PaneTab.Contacts -> Icons.Default.Contacts
                                    PaneTab.Keypad -> Icons.Default.Keyboard
                                    PaneTab.Settings -> Icons.Default.Settings
                                }, contentDescription = tab.label,
                                tint = if (active) Accent else Muted, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(tab.label, color = if (active) Ink else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
            if (glassTarget != null) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f))
                        .clickable { glassTarget = null }
                )
                AnimatedVisibility(
                    visible = glassTarget != null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.78f, stiffness = 450f)) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f)) + fadeOut()
                ) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0xDD1B2434))))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Box(Modifier.width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.65f)).align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(18.dp))
                        Text(glassTarget ?: "Pane", color = Color(0xFF101827), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text("LIQUID GLASS · TOUCH FEEDBACK", color = Color(0xFF4F5F79), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Pane uses a frosted glass surface for this held item.", color = Color(0xFF26334B), fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("CLOSE", color = Color(0xFF5368BD), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).clickable { glassTarget = null }.padding(8.dp))
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsPanel(
    theme: String,
    onThemeChange: (String) -> Unit,
    buttons: String,
    onButtonsChange: (String) -> Unit,
    incoming: String,
    onIncomingChange: (String) -> Unit,
    onGlassPress: (String) -> Unit
) {
    val groups = listOf(
        "PERSONALISATION" to listOf("Call Backgrounds", "Dialer Themes", "Call Buttons"),
        "TOOLS" to listOf("Call Announcer", "Flash On Call", "Fake Call"),
        "GENERAL" to listOf("Call Back Screen", "Contact Photo on Call", "Enable DTMF Tone", "SIM Preference", "Accessibility", "Default Launch Screen", "Voicemail", "Sound & Vibration", "Speed Dial", "Incoming Call Style", "Call Blocker", "Quick Response", "Language", "Call Settings"),
        "OTHER" to listOf("Help & FAQ", "Rate App", "Privacy Policy")
    )
    val switchRows = setOf("Call Announcer", "Flash On Call", "Contact Photo on Call", "Enable DTMF Tone")
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("pane_settings", Context.MODE_PRIVATE) }
    val states = remember(prefs) { mutableStateMapOf<String, Boolean>().apply { switchRows.forEach { this[it] = prefs.getBoolean(it, false) } } }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            states["Flash On Call"] = true
            prefs.edit().putBoolean("Flash On Call", true).apply()
        }
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var fakeName by remember { mutableStateOf("") }
    var fakeNumber by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize()) {
        Text("Settings", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            groups.forEach { group ->
                item {
                    Column {
                        Text(group.first, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(7.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(horizontal = 13.dp)) {
                            group.second.forEach { row ->
                                Row(Modifier.fillMaxWidth().combinedClickable(
                                    onClick = {
                                        if (row in switchRows) {
                                            val enabled = !(states[row] ?: false)
                                            if (row == "Flash On Call" && enabled &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            } else {
                                                states[row] = enabled
                                                prefs.edit().putBoolean(row, enabled).apply()
                                            }
                                        } else selected = row
                                    },
                                    onLongClick = { onGlassPress(row) }
                                ).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(row, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        val subtitle = when (row) { "Dialer Themes" -> "Current: " + theme; "Call Buttons" -> "Current: " + buttons; "Incoming Call Style" -> incoming; else -> null }
                                        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp)
                                    }
                                    if (row in switchRows) {
                                        val enabled = states[row] == true
                                        Box(Modifier.width(38.dp).height(23.dp).clip(CircleShape).background(if (enabled) CallGreen else Color(0xFF46536A)).padding(3.dp), contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart) { Box(Modifier.size(17.dp).clip(CircleShape).background(Color.White)) }
                                    } else Text("›", color = Muted, fontSize = 23.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (selected != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { selected = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(18.dp)) {
                Text(selected ?: "", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                when (selected) {
                    "Dialer Themes" -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("System", "Dark", "Light").forEach { option ->
                            val previewLight = option == "Light" || (option == "System" && !isSystemInDarkTheme())
                            val previewBg = if (previewLight) Color(0xFFF3F6FC) else Color(0xFF101827)
                            val previewText = if (previewLight) Color(0xFF172033) else Color(0xFFF0F3FF)
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                                    .background(previewBg).border(1.dp, if (theme == option) Accent else Line, RoundedCornerShape(15.dp))
                                    .clickable { onThemeChange(option); selected = null }.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                                    Text(if (theme == option) "✓" else "", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(3) { Box(Modifier.size(11.dp).clip(CircleShape).background(if (previewLight) Color(0xFFD4DDEC) else Color(0xFF344361))) }
                                }
                                Spacer(Modifier.height(7.dp))
                                Text("123", color = previewText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(option, color = previewText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    "Call Buttons" -> listOf("Classic dual buttons", "Slide to answer").forEach { option -> Text((if (buttons == option) "✓ " else "") + option, color = Ink, modifier = Modifier.fillMaxWidth().clickable { onButtonsChange(option); selected = null }.padding(12.dp)) }
                    "Incoming Call Style" -> listOf("Banner", "Fullscreen").forEach { option -> Text((if (incoming == option) "✓ " else "") + option, color = Ink, modifier = Modifier.fillMaxWidth().clickable { onIncomingChange(option); selected = null }.padding(12.dp)) }
                    "Call Backgrounds" -> Text("Photo/video call background selection is not connected yet.", color = Muted)
                    "Fake Call" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Preview an incoming call. This does not place a real phone call.", color = Muted, fontSize = 12.sp)
                        BasicTextField(
                            value = fakeName, onValueChange = { fakeName = it }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111827)).padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                            decorationBox = { inner -> if (fakeName.isBlank()) Text("Caller name (optional)", color = Muted, fontSize = 13.sp); inner() }
                        )
                        BasicTextField(
                            value = fakeNumber, onValueChange = { fakeNumber = it }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111827)).padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                            decorationBox = { inner -> if (fakeNumber.isBlank()) Text("Phone number", color = Muted, fontSize = 13.sp); inner() }
                        )
                        Text(
                            "START SIMULATED CALL",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).clickable {
                                if (fakeNumber.isNotBlank()) {
                                    context.startActivity(Intent(context, InCallActivity::class.java).apply {
                                        putExtra("pane_fake_call", true)
                                        putExtra("pane_fake_name", fakeName.ifBlank { "Unknown caller" })
                                        putExtra("pane_fake_number", fakeNumber)
                                    })
                                    selected = null
                                }
                            }.padding(10.dp)
                        )
                    }
                    "Flash On Call" -> Text("Flashlight hardware integration is not connected yet.", color = Muted)
                    "Privacy Policy" -> Text("Pane has no account backend. Contacts and call log are accessed locally after permission.", color = Muted)
                    else -> Text("This option is listed. Device integration must be implemented before it can change system behavior.", color = Muted)
                }
                Spacer(Modifier.height(12.dp))
                Text("CLOSE", color = Accent, modifier = Modifier.align(Alignment.End).clickable { selected = null }.padding(8.dp))
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentsPanel(
    people: List<Person>,
    emptyMessage: String,
    onCall: (String) -> Unit,
    filter: String,
    onFilterChange: (String) -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onGlassPress: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel)
                    .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Missed").forEach { option ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (filter == option) Color(0xFF344361) else Color.Transparent)
                            .clickable { onFilterChange(option) }.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, color = if (filter == option) Ink else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(if (editing) "Done" else "Edit", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onEditingChange(!editing) }.padding(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("CALL LOG · LATEST FIRST", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(9.dp))
        if (people.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(people, key = { it.id + "|" + it.number + "|" + it.type }) { person ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)
                            .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                            .combinedClickable(onClick = {}, onLongClick = { onGlassPress(person.name) }).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(43.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF536A95), Color(0xFF293750)))),
                            contentAlignment = Alignment.Center
                        ) { Text(person.initials, color = Ink, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${when (person.type) { "Missed" -> "↙ Missed"; "Outgoing" -> "↗ Outgoing"; "Incoming" -> "↙ Incoming"; else -> person.type }} · ${person.number}",
                                color = if (person.type == "Missed") Color(0xFFFF8585) else Muted, fontSize = 11.sp)
                            Text(person.timestamp, color = Muted, fontSize = 10.sp)
                            if (person.simLabel.isNotBlank()) Text(person.simLabel, color = Accent, fontSize = 9.sp)
                        }
                        if (editing) {
                            Text("⋯", color = Accent, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(Panel).border(1.dp, Line, CircleShape)
                                        .clickable { onGlassPress("Call details\n${person.name}\n${person.number}\n${person.type} · ${person.timestamp}\n${person.simLabel}") },
                                    contentAlignment = Alignment.Center
                                ) { Text("i", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF173C35))
                                        .border(1.dp, CallGreen.copy(alpha = 0.5f), CircleShape)
                                        .combinedClickable(onClick = { onCall(person.number) }, onLongClick = { onGlassPress("Call ${person.name}") }),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Call, contentDescription = "Open phone dialer", tint = CallGreen) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PeoplePanel(
    title: String,
    people: List<Person>,
    emptyMessage: String,
    onCall: (String) -> Unit,
    showType: Boolean = false,
    onGlassPress: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))
        if (people.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(people, key = { it.id + "|" + it.number + "|" + it.type }) { person ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Panel)
                            .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(19.dp))
                            .combinedClickable(onClick = {}, onLongClick = { onGlassPress(person.name) }).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(45.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF536A95), Color(0xFF293750)))),
                            contentAlignment = Alignment.Center
                        ) { Text(person.initials, color = Ink, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (showType && person.type.isNotBlank()) "${person.number} · ${person.type}" else person.number,
                                color = Muted, fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier.size(39.dp).clip(CircleShape).background(Color(0xFF173C35))
                                .border(1.dp, CallGreen.copy(alpha = 0.5f), CircleShape)
                                .combinedClickable(onClick = { onCall(person.number) }, onLongClick = { onGlassPress("Call ${person.name}") }),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Call, contentDescription = "Open phone dialer", tint = CallGreen) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadPanel(dialed: String, onDigit: (String) -> Unit, onDelete: () -> Unit, onCall: () -> Unit, onGlassPress: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (dialed.isBlank()) "Enter a number" else dialed, color = Ink, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(Panel).border(1.dp, Line, CircleShape)
                            .combinedClickable(onClick = { onDigit(key) }, onLongClick = { onGlassPress("Key $key") }), contentAlignment = Alignment.Center
                    ) { Text(key, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { Text(" ", color = Muted) }
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(CallGreen)
                    .combinedClickable(onClick = onCall, onLongClick = { onGlassPress("Place call") }),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Call, contentDescription = "Open system dialer", tint = Night, modifier = Modifier.size(26.dp)) }
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(Panel)
                    .combinedClickable(onClick = onDelete, onLongClick = { onGlassPress("Delete last digit") }),
                contentAlignment = Alignment.Center
            ) { Text("⌫", color = Ink, fontSize = 22.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Opens Android phone dialer for confirmation", color = Muted, fontSize = 11.sp)
    }
}
