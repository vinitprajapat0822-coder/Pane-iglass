package com.vinit.pane

import android.Manifest
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
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
private val LocalPaneLanguage = compositionLocalOf { "System default" }

private val HindiLabels = mapOf(
    "Favourites" to "पसंदीदा",
    "Your people" to "आपके लोग",
    "Recent calls" to "हाल की कॉल",
    "Contacts" to "संपर्क",
    "Keypad" to "डायल पैड",
    "Settings" to "सेटिंग्स",
    "PERSONALISATION" to "निजीकरण",
    "TOOLS" to "टूल्स",
    "GENERAL" to "सामान्य",
    "OTHER" to "अन्य",
    "Call Backgrounds" to "कॉल बैकग्राउंड",
    "Dialer Themes" to "डायलर थीम",
    "Call Buttons" to "कॉल बटन",
    "Call Announcer" to "कॉलर घोषणा",
    "Flash On Call" to "कॉल पर फ्लैश",
    "Fake Call" to "नकली कॉल",
    "Call Back Screen" to "कॉल-बैक स्क्रीन",
    "Contact Photo on Call" to "कॉल पर संपर्क फोटो",
    "Enable DTMF Tone" to "DTMF टोन",
    "SIM Preference" to "SIM प्राथमिकता",
    "Accessibility" to "सुलभता",
    "Default Launch Screen" to "शुरुआती स्क्रीन",
    "Voicemail" to "वॉइसमेल",
    "Sound & Vibration" to "ध्वनि और कंपन",
    "Speed Dial" to "स्पीड डायल",
    "Incoming Call Style" to "इनकमिंग कॉल शैली",
    "Call Blocker" to "कॉल ब्लॉकर",
    "Quick Response" to "त्वरित जवाब",
    "Language" to "भाषा",
    "Call Settings" to "कॉल सेटिंग्स",
    "Help & FAQ" to "सहायता और FAQ",
    "Rate App" to "ऐप रेट करें",
    "Privacy Policy" to "गोपनीयता नीति",
    "All" to "सभी",
    "Missed" to "मिस्ड",
    "Edit" to "संपादित करें",
    "Done" to "हो गया",
    "CALL LOG · LATEST FIRST" to "कॉल सूची · नई कॉल पहले",
    "PHONE CONTACTS" to "फोन संपर्क",
    "STARRED CONTACTS" to "पसंदीदा संपर्क",
    "Search names or numbers" to "नाम या नंबर खोजें",
    "Enter a number" to "नंबर दर्ज करें",
    "No recent calls found." to "हाल की कॉल नहीं हैं।",
    "No matching contacts." to "मिलते-जुलते संपर्क नहीं मिले।"
)

@Composable
private fun localizedLabel(text: String): String =
    if (LocalPaneLanguage.current == "हिन्दी") HindiLabels[text] ?: text else text

@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = localizedLabel(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        style = style
    )
}
private val Night: Color @Composable get() = LocalPanePalette.current.background
private val Panel: Color @Composable get() = LocalPanePalette.current.panel
private val Line: Color @Composable get() = LocalPanePalette.current.line
private val Ink: Color @Composable get() = LocalPanePalette.current.ink
private val Muted: Color @Composable get() = LocalPanePalette.current.muted
private val Accent: Color @Composable get() = LocalPanePalette.current.accent
private val CallGreen: Color @Composable get() = LocalPanePalette.current.callGreen

private enum class PaneTab(val label: String) {
    Favorites("Favourites"), Recents("Recents"), Contacts("Contacts"), Keypad("Keypad"), Settings("Settings")
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
    private val dtmfTone = ToneGenerator(AudioManager.STREAM_DTMF, 82)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_DTMF
        setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.12.sp,
                        lineHeight = 20.sp
                    )
                ) { PaneApp(this) }
            }
        }
    }

    override fun onDestroy() {
        runCatching { dtmfTone.release() }
        super.onDestroy()
    }

    fun playDtmf(digit: String) {
        if (!getSharedPreferences("pane_settings", Context.MODE_PRIVATE).getBoolean("Enable DTMF Tone", true)) return
        val tone = when (digit) {
            "0" -> ToneGenerator.TONE_DTMF_0
            "1" -> ToneGenerator.TONE_DTMF_1
            "2" -> ToneGenerator.TONE_DTMF_2
            "3" -> ToneGenerator.TONE_DTMF_3
            "4" -> ToneGenerator.TONE_DTMF_4
            "5" -> ToneGenerator.TONE_DTMF_5
            "6" -> ToneGenerator.TONE_DTMF_6
            "7" -> ToneGenerator.TONE_DTMF_7
            "8" -> ToneGenerator.TONE_DTMF_8
            "9" -> ToneGenerator.TONE_DTMF_9
            "*" -> ToneGenerator.TONE_DTMF_S
            "#" -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        runCatching { dtmfTone.stopTone(); dtmfTone.startTone(tone, 130) }
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
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", clean, null))
            // If Pane is the default dialer, an implicit ACTION_DIAL may reopen Pane itself.
            if (isDefaultDialer()) {
                val externalDialer = packageManager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY
                ).firstOrNull { it.activityInfo.packageName != packageName }
                if (externalDialer == null) {
                    android.widget.Toast.makeText(
                        this, "No other phone dialer app was found on this device.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return
                }
                intent.component = ComponentName(externalDialer.activityInfo.packageName, externalDialer.activityInfo.name)
            }
            val prefs = getSharedPreferences("pane_settings", Context.MODE_PRIVATE)
            val preferredId = prefs.getString("preferred_phone_account_id", null)
            val preferredComponent = prefs.getString("preferred_phone_account_component", null)
            val telecom = getSystemService(TelecomManager::class.java)
            val preferred = telecom?.callCapablePhoneAccounts?.firstOrNull { handle ->
                handle.id == preferredId && handle.componentName.flattenToString() == preferredComponent
            }
            if (preferred != null) intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, preferred)
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaneApp(activity: MainActivity) {
    val prefs = remember(activity) { activity.getSharedPreferences("pane_settings", Context.MODE_PRIVATE) }
    var selected by remember(prefs) {
        mutableStateOf(runCatching { PaneTab.valueOf(prefs.getString("default_launch_screen", "Favorites") ?: "Favorites") }.getOrDefault(PaneTab.Favorites))
    }
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
    var themeChoice by remember(prefs) { mutableStateOf(prefs.getString("dialer_theme", "System") ?: "System") }
    var languageChoice by remember(prefs) { mutableStateOf(prefs.getString("app_language", "System default") ?: "System default") }
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

    CompositionLocalProvider(LocalPanePalette provides palette, LocalPaneLanguage provides languageChoice) {
    Surface(Modifier.fillMaxSize(), color = Night) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (useDarkPalette) listOf(Color(0xFF19243A), Night, Color(0xFF101827))
            else listOf(Color(0xFFE8EDF7), Night, Color(0xFFF8FAFD))
        ))) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
                .blur(if (glassTarget != null) 8.dp else 0.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(Panel)
                                .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(CallGreen))
                            Spacer(Modifier.width(7.dp))
                            Text("PANE  /  SMART DIALER", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.15.sp)
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            when (selected) {
                                PaneTab.Favorites -> "Your people"
                                PaneTab.Recents -> "Recent calls"
                                PaneTab.Contacts -> "Contacts"
                                PaneTab.Keypad -> "Keypad"
                                PaneTab.Settings -> "Settings"
                            }, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.55).sp, lineHeight = 34.sp
                        )
                    }
                    Column(
                        Modifier.clip(RoundedCornerShape(18.dp)).background(Panel)
                            .border(1.dp, Line.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LOCAL", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("•", color = CallGreen, fontSize = 17.sp, lineHeight = 15.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (!defaultDialer) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)
                            .border(1.dp, Line, RoundedCornerShape(16.dp))
                            .combinedClickable(onClick = { activity.requestDefaultDialer() }, onLongClick = { showGlass("Set Pane as default phone app") }).padding(12.dp),
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
                            speedDials = (1..5).mapNotNull { slot -> prefs.getString("speed_dial_$slot", null)?.let { slot.toString() to it } },
                            onSpeedDial = activity::openDialer,
                            onDigit = { dialed += it; activity.playDtmf(it) },
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
                            language = languageChoice,
                            onLanguageChange = { languageChoice = it; prefs.edit().putString("app_language", it).apply() },
                            onGlassPress = showGlass
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().shadow(22.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (useDarkPalette) Color(0xB51A2740) else Color(0xD9FFFFFF))
                        .border(1.dp, Color.White.copy(alpha = if (useDarkPalette) 0.18f else 0.82f), RoundedCornerShape(30.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaneTab.values().forEach { tab ->
                        val active = tab == selected
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                                .background(if (active) Accent.copy(alpha = if (useDarkPalette) 0.28f else 0.16f) else Color.Transparent)
                                .combinedClickable(onClick = { selected = tab; prefs.edit().putString("default_launch_screen", tab.name).apply() }, onLongClick = { showGlass(tab.label) }).padding(vertical = 8.dp),
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
                            .background(Brush.verticalGradient(listOf(Color(0xF2FFFFFF), Color(0xE61B2434), Color(0xEA101827))))
                            .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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


private fun settingGlyph(label: String): String = when (label) {
    "Call Backgrounds" -> "◌"
    "Dialer Themes" -> "✧"
    "Call Buttons" -> "⇄"
    "Call Announcer" -> "♫"
    "Flash On Call" -> "ϟ"
    "Fake Call" -> "☎"
    "Call Back Screen" -> "↩"
    "Contact Photo on Call" -> "◎"
    "Enable DTMF Tone" -> "♪"
    "SIM Preference" -> "▥"
    "Accessibility" -> "◉"
    "Default Launch Screen" -> "⌂"
    "Voicemail" -> "▣"
    "Sound & Vibration" -> "≋"
    "Speed Dial" -> "1"
    "Incoming Call Style" -> "▱"
    "Call Blocker" -> "⊘"
    "Quick Response" -> "↗"
    "Language" -> "文"
    "Call Settings" -> "⚙"
    "Help & FAQ" -> "?"
    "Rate App" -> "★"
    "Privacy Policy" -> "⌑"
    else -> "·"
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
    language: String,
    onLanguageChange: (String) -> Unit,
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
    val states = remember(prefs) { mutableStateMapOf<String, Boolean>().apply { switchRows.forEach { this[it] = prefs.getBoolean(it, it == "Enable DTMF Tone") } } }
    var selected by remember { mutableStateOf<String?>(null) }
    var speedDialNumber by remember { mutableStateOf("") }
    var blockNumber by remember { mutableStateOf("") }
    var blockedNumbers by remember { mutableStateOf(prefs.getStringSet("blocked_numbers", emptySet()).orEmpty().toSet()) }
    var callbackEnabled by remember(selected) { mutableStateOf(prefs.getBoolean("Call Back Screen", true)) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            states["Flash On Call"] = true
            prefs.edit().putBoolean("Flash On Call", true).apply()
        }
    }
    val imageBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            prefs.edit().putString("call_background_uri", uri.toString()).putString("call_background_type", "image").apply()
            selected = null
        }
    }
    val videoBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            prefs.edit().putString("call_background_uri", uri.toString()).putString("call_background_type", "video").apply()
            selected = null
        }
    }
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
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel)
                            .border(1.dp, Line.copy(alpha = 0.92f), RoundedCornerShape(20.dp)).padding(horizontal = 13.dp)) {
                            group.second.forEachIndexed { rowIndex, row ->
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
                                    Box(
                                        Modifier.size(35.dp).clip(RoundedCornerShape(12.dp))
                                            .background(Accent.copy(alpha = if (theme == "Light") 0.11f else 0.15f))
                                            .border(1.dp, Accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(settingGlyph(row), color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(11.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(row, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.sp)
                                        val subtitle = when (row) { "Dialer Themes" -> "Current: " + theme; "Call Buttons" -> "Current: " + buttons; "Incoming Call Style" -> incoming; else -> null }
                                        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp)
                                    }
                                    if (row in switchRows) {
                                        val enabled = states[row] == true
                                        Box(Modifier.width(38.dp).height(23.dp).clip(CircleShape).background(if (enabled) CallGreen else Color(0xFF46536A)).padding(3.dp), contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart) { Box(Modifier.size(17.dp).clip(CircleShape).background(Color.White)) }
                                    } else Text("›", color = Muted, fontSize = 23.sp)
                                }
                                if (rowIndex < group.second.lastIndex) {
                                    Spacer(Modifier.padding(start = 46.dp).fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.36f)))
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
                                    .combinedClickable(
                                        onClick = { onThemeChange(option); selected = null },
                                        onLongClick = { onGlassPress("Dialer theme: $option") }
                                    ).padding(8.dp),
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
                    "Call Buttons" -> listOf("Classic dual buttons", "Slide to answer").forEach { option -> Text((if (buttons == option) "✓ " else "") + option, color = Ink, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onButtonsChange(option); selected = null }, onLongClick = { onGlassPress("Call button style: $option") }).padding(12.dp)) }
                    "Incoming Call Style" -> listOf("Banner", "Fullscreen").forEach { option -> Text((if (incoming == option) "✓ " else "") + option, color = Ink, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onIncomingChange(option); selected = null }, onLongClick = { onGlassPress("Incoming call style: $option") }).padding(12.dp)) }
                    "Call Backgrounds" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Saved background: " + if (prefs.getString("call_background_uri", null) == null) "None" else prefs.getString("call_background_type", "image")!!.replaceFirstChar { it.uppercase() }, color = Muted, fontSize = 12.sp)
                        Text("CHOOSE PHOTO", color = Accent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { imageBackgroundPicker.launch(arrayOf("image/*")) }, onLongClick = { onGlassPress("Choose call background photo") }).padding(vertical = 12.dp))
                        Text("CHOOSE VIDEO", color = Accent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { videoBackgroundPicker.launch(arrayOf("video/*")) }, onLongClick = { onGlassPress("Choose call background video") }).padding(vertical = 12.dp))
                        Text("Background applies to the incoming/active call screen.", color = Muted, fontSize = 11.sp)
                    }
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
                    "Default Launch Screen" -> listOf("Favourites", "Recents", "Contacts", "Keypad", "Settings").forEach { option ->
                        val tabName = when (option) { "Favourites" -> "Favorites"; else -> option }
                        Text((if (prefs.getString("default_launch_screen", "Favorites") == tabName) "✓  " else "    ") + option, color = Ink,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { prefs.edit().putString("default_launch_screen", tabName).apply(); selected = null },
                                onLongClick = { onGlassPress("Launch screen: $option") }
                            ).padding(vertical = 12.dp))
                    }
                    "SIM Preference" -> {
                        Text("Choose the SIM/account preferred when opening Android's system dialer.", color = Muted, fontSize = 12.sp)
                        val telecom = context.getSystemService(TelecomManager::class.java)
                        val accounts = telecom?.callCapablePhoneAccounts.orEmpty()
                        val preferredId = prefs.getString("preferred_phone_account_id", null)
                        if (accounts.isEmpty()) Text("Android did not report a call-capable SIM account.", color = Muted, fontSize = 12.sp)
                        accounts.forEach { handle ->
                            val label = telecom?.getPhoneAccount(handle)?.label?.toString().orEmpty().ifBlank { handle.id }
                            Text((if (handle.id == preferredId) "✓  " else "    ") + label, color = Ink,
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    onClick = {
                                        prefs.edit().putString("preferred_phone_account_id", handle.id)
                                            .putString("preferred_phone_account_component", handle.componentName.flattenToString()).apply()
                                        selected = null
                                    }, onLongClick = { onGlassPress("Preferred SIM: $label") }
                                ).padding(vertical = 12.dp))
                        }
                        Text("Android or the carrier dialer may still show its own SIM confirmation.", color = Muted, fontSize = 11.sp)
                    }
                    "Accessibility" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Open Android accessibility settings for screen readers, magnification and other assistive options.", color = Muted, fontSize = 12.sp)
                        Text("OPEN ACCESSIBILITY SETTINGS  ↗", color = Accent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }.padding(vertical = 12.dp))
                    }
                    "Call Settings", "Voicemail" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (selected == "Voicemail") "Voicemail setup varies by SIM operator. Open system phone settings to configure the number and alerts." else "Open Android phone settings. Available options depend on the carrier and installed phone service.", color = Muted, fontSize = 12.sp)
                        Text("OPEN PHONE SETTINGS  ↗", color = Accent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { runCatching { context.startActivity(Intent("android.settings.CALL_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }.padding(vertical = 12.dp))
                    }
                    "Sound & Vibration" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ringer volume, vibration patterns and call audio are controlled by Android and your carrier.", color = Muted, fontSize = 12.sp)
                        Text("OPEN SOUND SETTINGS  ↗", color = Accent, fontWeight = FontWeight.Bold,
                            modifier = Modifier.combinedClickable(
                                onClick = { runCatching { context.startActivity(Intent("android.settings.SOUND_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                                onLongClick = { onGlassPress("Open system sound settings") }
                            ).padding(vertical = 12.dp))
                    }
                    "Call Back Screen" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (callbackEnabled) "✓  Callback shortcut enabled" else "○  Callback shortcut disabled", color = Ink,
                            modifier = Modifier.fillMaxWidth().clickable {
                                callbackEnabled = !callbackEnabled
                                prefs.edit().putBoolean("Call Back Screen", callbackEnabled).apply()
                            }.padding(vertical = 10.dp))
                        Text("This option saves locally; callback actions still open Android's dialer for confirmation.", color = Muted, fontSize = 11.sp)
                    }
                    "Speed Dial" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter a number, then select a slot to save it.", color = Muted, fontSize = 12.sp)
                        BasicTextField(value = speedDialNumber, onValueChange = { speedDialNumber = it.take(40) }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Night).padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                            decorationBox = { inner -> if (speedDialNumber.isBlank()) Text("Phone number", color = Muted, fontSize = 13.sp); inner() })
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { slot ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Accent.copy(alpha = 0.15f))
                                    .combinedClickable(onClick = {
                                        if (speedDialNumber.isNotBlank()) {
                                            prefs.edit().putString("speed_dial_$slot", speedDialNumber.trim()).apply()
                                            speedDialNumber = ""; selected = null
                                        }
                                    }, onLongClick = { onGlassPress("Speed dial slot $slot") }).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                                    Text("$slot", color = Ink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Stored locally. System-dialer quick shortcuts are controlled by Android.", color = Muted, fontSize = 11.sp)
                    }
                    "Call Blocker" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Manage numbers in Pane's local block list. Automatic rejection is only possible while Pane is the active default phone app.", color = Muted, fontSize = 12.sp)
                        BasicTextField(value = blockNumber, onValueChange = { blockNumber = it.take(40) }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Night).padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                            decorationBox = { inner -> if (blockNumber.isBlank()) Text("Number to block", color = Muted, fontSize = 13.sp); inner() })
                        Text("ADD TO BLOCK LIST", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            val number = blockNumber.filter { it.isDigit() || it == '+' }
                            if (number.isNotBlank()) {
                                blockedNumbers = blockedNumbers + number
                                prefs.edit().putStringSet("blocked_numbers", blockedNumbers).apply()
                                blockNumber = ""
                            }
                        }.padding(vertical = 8.dp))
                        if (blockedNumbers.isEmpty()) Text("No numbers added yet.", color = Muted, fontSize = 12.sp)
                        blockedNumbers.forEach { number ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(number, color = Ink, modifier = Modifier.weight(1f))
                                Text("REMOVE", color = Color(0xFFFF7A86), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        blockedNumbers = blockedNumbers - number
                                        prefs.edit().putStringSet("blocked_numbers", blockedNumbers).apply()
                                    }.padding(6.dp))
                            }
                        }
                    }
                    "Quick Response" -> listOf("Can't talk right now.", "I'll call you back soon.", "Please message me.", "I'm busy at the moment.").forEach { message ->
                        Text((if (prefs.getString("quick_response", "") == message) "✓  " else "    ") + message, color = Ink,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { prefs.edit().putString("quick_response", message).apply(); selected = null },
                                onLongClick = { onGlassPress("Quick response selected") }
                            ).padding(vertical = 12.dp))
                    }
                    "Language" -> listOf("System default", "English", "हिन्दी").forEach { option ->
                        Text((if (language == option) "✓  " else "    ") + option, color = Ink,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onLanguageChange(option); selected = null },
                                onLongClick = { onGlassPress("Preferred language: $option") }
                            ).padding(vertical = 12.dp))
                    }
                    "Help & FAQ" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contacts and call history are read locally after Android permissions are granted. Pane opens the Android dialer for outgoing calls. Default-phone privileges must be approved in the system prompt.", color = Muted, fontSize = 12.sp)
                        Text("No account, subscription, advertising SDK or backend is included.", color = Ink, fontSize = 12.sp)
                    }
                    "Rate App" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("The Play Store listing will be available after publication.", color = Muted, fontSize = 12.sp)
                        Text("OPEN STORE PAGE  ↗", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=\${context.packageName}"))
                            runCatching { context.startActivity(market) }.onFailure {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=\${context.packageName}"))) }
                            }
                        }.padding(vertical = 12.dp))
                    }
                    "Privacy Policy" -> Text("Pane has no account backend. Contacts and call history are accessed locally after permission. Settings are stored on device. Calls are handed to Android's system dialer for confirmation.", color = Muted)
                    "Flash On Call" -> Text("The flashlight blinks while an incoming call is ringing when camera permission and compatible flash hardware are available.", color = Muted)
                    else -> Text("This setting is listed but its system-level behaviour still needs implementation or device verification.", color = Muted)
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
                            .background(if (filter == option) Accent.copy(alpha = 0.19f) else Color.Transparent)
                            .combinedClickable(
                                onClick = { onFilterChange(option) },
                                onLongClick = { onGlassPress("Recent calls filter: $option") }
                            ).padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, color = if (filter == option) Accent else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(if (editing) "Done" else "Edit", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.combinedClickable(
                    onClick = { onEditingChange(!editing) },
                    onLongClick = { onGlassPress(if (editing) "Finish editing recents" else "Edit recent calls") }
                ).padding(8.dp))
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
                        ) { Text(person.initials, color = Color.White, fontWeight = FontWeight.SemiBold) }
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
                                        .combinedClickable(
                                            onClick = { onGlassPress("Call details\n${person.name}\n${person.number}\n${person.type} · ${person.timestamp}\n${person.simLabel}") },
                                            onLongClick = { onGlassPress("Call details · ${person.name}") }
                                        ),
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
                        ) { Text(person.initials, color = Color.White, fontWeight = FontWeight.SemiBold) }
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
private fun KeypadPanel(
    dialed: String,
    speedDials: List<Pair<String, String>>,
    onSpeedDial: (String) -> Unit,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onGlassPress: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (dialed.isBlank()) "Enter a number" else dialed, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (speedDials.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                speedDials.take(5).forEach { (slot, number) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(Panel)
                            .border(1.dp, Line, RoundedCornerShape(13.dp))
                            .combinedClickable(onClick = { onSpeedDial(number) }, onLongClick = { onGlassPress("Speed dial slot $slot") })
                            .padding(vertical = 8.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(slot, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(number.takeLast(4), color = Muted, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.size(64.dp).shadow(5.dp, CircleShape).clip(CircleShape).background(Panel).border(1.dp, Line.copy(alpha = 0.85f), CircleShape)
                            .combinedClickable(onClick = { onDigit(key) }, onLongClick = { onGlassPress("Key $key") }), contentAlignment = Alignment.Center
                    ) { Text(key, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold) }
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
