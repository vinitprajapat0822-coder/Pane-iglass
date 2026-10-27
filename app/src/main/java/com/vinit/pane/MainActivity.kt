package com.vinit.pane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF0D111B)
private val Panel = Color(0xCC1B2333)
private val Line = Color(0xFF39465E)
private val Ink = Color(0xFFF0F3FF)
private val Muted = Color(0xFFADB8D0)
private val Accent = Color(0xFFB9C5FF)
private val CallGreen = Color(0xFF39D98A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PaneApp()
            }
        }
    }
}

private enum class PaneTab(val label: String) {
    Favorites("Favorites"), Recents("Recents"), Contacts("Contacts"), Keypad("Keypad")
}

private data class Person(val name: String, val number: String, val initials: String)

private val demoPeople = listOf(
    Person("Aarav Sharma", "+91 98765 43210", "AS"),
    Person("Priya Verma", "+91 91234 56780", "PV"),
    Person("Kabir Singh", "+91 99887 76655", "KS"),
    Person("Ananya Mehta", "+91 90123 45678", "AM")
)

@Composable
private fun PaneApp() {
    var selected by remember { mutableStateOf(PaneTab.Favorites) }
    var query by remember { mutableStateOf("") }
    var dialed by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF19243A), Night, Color(0xFF101827))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PANE", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text(
                            when (selected) {
                                PaneTab.Favorites -> "Your people"
                                PaneTab.Recents -> "Recent calls"
                                PaneTab.Contacts -> "Contacts"
                                PaneTab.Keypad -> "Keypad"
                            },
                            color = Ink, fontSize = 29.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(Color(0xFF26334A))
                            .border(1.dp, Line, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("V", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Panel).border(1.dp, Line.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search people or numbers", color = Muted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Accent
                        )
                    )
                }

                Spacer(Modifier.height(18.dp))

                when (selected) {
                    PaneTab.Keypad -> KeypadPanel(dialed = dialed, onDigit = { dialed += it }, onDelete = { if (dialed.isNotEmpty()) dialed = dialed.dropLast(1) })
                    else -> PeoplePanel(
                        tab = selected,
                        query = query,
                        onCallTap = { dialed = it }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Color(0xD91B2434))
                        .border(1.dp, Color(0xFF46536A), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaneTab.values().forEach { tab ->
                        val active = tab == selected
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (active) Color(0xFF344361) else Color.Transparent)
                                .clickable { selected = tab }
                                .padding(vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    PaneTab.Favorites -> Icons.Default.Favorite
                                    PaneTab.Recents -> Icons.Default.History
                                    PaneTab.Contacts -> Icons.Default.Contacts
                                    PaneTab.Keypad -> Icons.Default.Keyboard
                                },
                                contentDescription = tab.label,
                                tint = if (active) Accent else Muted,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(tab.label, color = if (active) Ink else Muted, fontSize = 10.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeoplePanel(tab: PaneTab, query: String, onCallTap: (String) -> Unit) {
    val people = when (tab) {
        PaneTab.Favorites -> demoPeople.take(3)
        PaneTab.Recents -> demoPeople.reversed()
        else -> demoPeople
    }.filter { it.name.contains(query, ignoreCase = true) || it.number.contains(query) }

    Column {
        Text(
            when (tab) {
                PaneTab.Favorites -> "FREQUENT CONTACTS"
                PaneTab.Recents -> "LATEST ACTIVITY · DEMO"
                PaneTab.Contacts -> "ALL CONTACTS · DEMO"
                PaneTab.Keypad -> ""
            },
            color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(people) { person ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Panel)
                        .border(1.dp, Line.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF536A95), Color(0xFF293750)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(person.initials, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(person.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(person.number, color = Muted, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF173C35))
                            .border(1.dp, CallGreen.copy(alpha = 0.45f), CircleShape)
                            .clickable { onCallTap(person.number) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Select number", tint = CallGreen)
                    }
                }
            }
        }
        if (people.isEmpty()) {
            Text("No matching contacts", color = Muted, modifier = Modifier.padding(18.dp))
        }
    }
}

@Composable
private fun KeypadPanel(dialed: String, onDigit: (String) -> Unit, onDelete: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(if (dialed.isBlank()) "Enter a number" else dialed, color = Ink, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.size(70.dp).clip(CircleShape).background(Panel)
                            .border(1.dp, Line, CircleShape).clickable { onDigit(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(52.dp))
            Box(
                modifier = Modifier.size(62.dp).clip(CircleShape).background(CallGreen)
                    .clickable { /* Native calling will be connected after permission and dialer-role flow. */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call (not connected yet)", tint = Night, modifier = Modifier.size(27.dp))
            }
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Panel).clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌫", color = Ink, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("UI prototype · calling not connected yet", color = Muted, fontSize = 11.sp)
    }
}
