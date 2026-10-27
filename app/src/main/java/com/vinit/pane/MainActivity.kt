package com.vinit.pane

import android.Manifest
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Night = Color(0xFF0D111B)
private val Panel = Color(0xCC1B2333)
private val Line = Color(0xFF39465E)
private val Ink = Color(0xFFF0F3FF)
private val Muted = Color(0xFFADB8D0)
private val Accent = Color(0xFFB9C5FF)
private val CallGreen = Color(0xFF39D98A)

private enum class PaneTab(val label: String) {
    Favorites("Favorites"), Recents("Recents"), Contacts("Contacts"), Keypad("Keypad")
}

private data class Person(
    val id: String,
    val name: String,
    val number: String,
    val starred: Boolean = false,
    val type: String = ""
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

    fun readContacts(): List<Person> {
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

    fun readRecents(): List<Person> {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return emptyList()
        val result = mutableListOf<Person>()
        val projection = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION
        )
        runCatching {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
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
                    result += Person(cursor.getString(idCol).orEmpty(), name, number, type = type)
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

@Composable
private fun PaneApp(activity: MainActivity) {
    var selected by remember { mutableStateOf(PaneTab.Favorites) }
    var query by remember { mutableStateOf("") }
    var dialed by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<Person>()) }
    var recents by remember { mutableStateOf(emptyList<Person>()) }
    var permissionState by remember { mutableStateOf(false) }
    var defaultDialer by remember { mutableStateOf(activity.isDefaultDialer()) }

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
        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG)
            .filterNot(activity::hasPermission)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else refresh()
    }

    Surface(Modifier.fillMaxSize(), color = Night) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF19243A), Night, Color(0xFF101827))))) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("P A N E", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when (selected) {
                                PaneTab.Favorites -> "Your people"
                                PaneTab.Recents -> "Recent calls"
                                PaneTab.Contacts -> "Contacts"
                                PaneTab.Keypad -> "Keypad"
                            }, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("AURORA GLASS", color = Muted, fontSize = 10.sp)
                }

                Spacer(Modifier.height(12.dp))
                if (!defaultDialer) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF27354B))
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
                            onCall = { activity.openDialer(dialed) }
                        )
                        PaneTab.Contacts -> PeoplePanel(
                            title = "PHONE CONTACTS",
                            people = contacts.filter { it.name.contains(query, true) || it.number.contains(query, true) },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CONTACTS)) "Contacts permission is needed to show phone contacts." else "No matching contacts.",
                            onCall = activity::openDialer
                        )
                        PaneTab.Favorites -> PeoplePanel(
                            title = "STARRED CONTACTS",
                            people = contacts.filter { it.starred && (it.name.contains(query, true) || it.number.contains(query, true)) },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CONTACTS)) "Allow Contacts permission to load favorites." else "No starred contacts found. Star contacts in your phone's Contacts app.",
                            onCall = activity::openDialer
                        )
                        PaneTab.Recents -> PeoplePanel(
                            title = "CALL LOG · LATEST FIRST",
                            people = recents.filter { it.name.contains(query, true) || it.number.contains(query, true) },
                            emptyMessage = if (!activity.hasPermission(Manifest.permission.READ_CALL_LOG)) "Call-log permission is needed to show recent calls." else "No recent calls found.",
                            onCall = activity::openDialer,
                            showType = true
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xD91B2434))
                        .border(1.dp, Color(0xFF46536A), RoundedCornerShape(24.dp)).padding(7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaneTab.values().forEach { tab ->
                        val active = tab == selected
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (active) Color(0xFF344361) else Color.Transparent)
                                .clickable { selected = tab }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                when (tab) {
                                    PaneTab.Favorites -> Icons.Default.Favorite
                                    PaneTab.Recents -> Icons.Default.History
                                    PaneTab.Contacts -> Icons.Default.Contacts
                                    PaneTab.Keypad -> Icons.Default.Keyboard
                                }, contentDescription = tab.label,
                                tint = if (active) Accent else Muted, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(tab.label, color = if (active) Ink else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeoplePanel(
    title: String,
    people: List<Person>,
    emptyMessage: String,
    onCall: (String) -> Unit,
    showType: Boolean = false
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
                            .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(19.dp)).padding(12.dp),
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
                                .border(1.dp, CallGreen.copy(alpha = 0.5f), CircleShape).clickable { onCall(person.number) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Call, contentDescription = "Open phone dialer", tint = CallGreen) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadPanel(dialed: String, onDigit: (String) -> Unit, onDelete: () -> Unit, onCall: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (dialed.isBlank()) "Enter a number" else dialed, color = Ink, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(Panel).border(1.dp, Line, CircleShape)
                            .clickable { onDigit(key) }, contentAlignment = Alignment.Center
                    ) { Text(key, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { Text(" ", color = Muted) }
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(CallGreen).clickable { onCall() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Call, contentDescription = "Open system dialer", tint = Night, modifier = Modifier.size(26.dp)) }
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(Panel).clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) { Text("⌫", color = Ink, fontSize = 22.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Opens Android phone dialer for confirmation", color = Muted, fontSize = 11.sp)
    }
}
