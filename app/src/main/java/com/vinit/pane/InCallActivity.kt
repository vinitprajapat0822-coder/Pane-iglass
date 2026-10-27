package com.vinit.pane

import android.telecom.Call
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CallBg = Color(0xFF0D111B)
private val CallCard = Color(0xFF1B2333)
private val CallText = Color(0xFFF0F3FF)
private val CallMuted = Color(0xFFADB8D0)
private val CallAccent = Color(0xFF39D98A)
private val CallRed = Color(0xFFFF667A)

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent { MaterialTheme { InCallScreen() } }
    }
}

@Composable
private fun InCallScreen() {
    val call by remember { derivedStateOf { PaneCallController.call } }
    val state by remember { derivedStateOf { PaneCallController.state } }
    val name by remember { derivedStateOf { PaneCallController.displayName } }
    val number by remember { derivedStateOf { PaneCallController.number } }
    val muted by remember { derivedStateOf { PaneCallController.muted } }
    val speaker by remember { derivedStateOf { PaneCallController.speaker } }
    val ringing = state == Call.STATE_RINGING
    val active = state == Call.STATE_ACTIVE
    val status = when (state) {
        Call.STATE_RINGING -> "INCOMING CALL"
        Call.STATE_DIALING -> "CALLING…"
        Call.STATE_CONNECTING -> "CONNECTING…"
        Call.STATE_ACTIVE -> "CONNECTED"
        Call.STATE_HOLDING -> "ON HOLD"
        Call.STATE_DISCONNECTING -> "ENDING…"
        else -> if (call == null) "NO ACTIVE CALL" else "CALL STATUS"
    }

    Surface(Modifier.fillMaxSize(), color = CallBg) {
        Box(Modifier.fillMaxSize().background(CallBg).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("P A N E", color = Color(0xFFB9C5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(42.dp))
                Box(Modifier.size(104.dp).clip(CircleShape).background(Color(0xFF293750)), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = CallText, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(22.dp))
                Text(name, color = CallText, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(number, color = CallMuted, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(status, color = CallMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(52.dp))
                if (active || state == Call.STATE_HOLDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CallAction(if (muted) "Unmute" else "Mute", if (muted) CallAccent else CallCard) { PaneCallController.toggleMute() }
                        CallAction(if (speaker) "Speaker on" else "Speaker", if (speaker) CallAccent else CallCard) { PaneCallController.toggleSpeaker() }
                    }
                    Spacer(Modifier.height(26.dp))
                    CallAction("End call", CallRed) { PaneCallController.end() }
                } else if (ringing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        CallAction("Decline", CallRed) { PaneCallController.reject() }
                        CallAction("Answer", CallAccent) { PaneCallController.answer() }
                    }
                } else if (call != null) {
                    CallAction("End call", CallRed) { PaneCallController.end() }
                } else {
                    Text("Call screen opens when Android Telecom delivers a call.", color = CallMuted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CallAction(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(color).clickable(onClick = onClick)
        .padding(horizontal = 22.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (color == CallAccent) CallBg else CallText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
