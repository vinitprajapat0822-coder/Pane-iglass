package com.vinit.pane

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared UI state; all call operations are delegated to Android Telecom. */
object PaneCallController {
    var call by mutableStateOf<Call?>(null)
        private set
    var state by mutableStateOf(Call.STATE_DISCONNECTED)
        private set
    var displayName by mutableStateOf("Phone call")
        private set
    var number by mutableStateOf("")
        private set
    var lastEndedName by mutableStateOf("")
        private set
    var lastEndedNumber by mutableStateOf("")
        private set
    var callEndSequence by mutableStateOf(0)
        private set
    var muted by mutableStateOf(false)
        private set
    var speaker by mutableStateOf(false)
        private set
    private var service: InCallService? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            this@PaneCallController.state = state
            if (state == Call.STATE_DISCONNECTED) clearIfCurrent(call)
        }
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateDetails(call)
        }
    }

    fun attach(newCall: Call, inCallService: InCallService) {
        call?.unregisterCallback(callback)
        service = inCallService
        call = newCall
        state = newCall.state
        updateDetails(newCall)
        newCall.registerCallback(callback)
    }

    fun detach(removed: Call) {
        removed.unregisterCallback(callback)
        clearIfCurrent(removed)
    }

    private fun updateDetails(target: Call) {
        val details = target.details
        number = details.handle?.schemeSpecificPart.orEmpty()
        displayName = details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: number.ifBlank { "Unknown caller" }
    }

    private fun clearIfCurrent(target: Call) {
        if (call === target) {
            lastEndedName = displayName
            lastEndedNumber = number
            callEndSequence += 1
            call = null
            state = Call.STATE_DISCONNECTED
            displayName = "Phone call"
            number = ""
            muted = false
            speaker = false
            service = null
        }
    }

    fun answer() {
        call?.takeIf { it.state == Call.STATE_RINGING }?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }
    fun reject() {
        call?.takeIf { it.state == Call.STATE_RINGING }?.reject(false, null)
    }
    fun end() {
        call?.disconnect()
    }
    fun toggleMute() {
        val next = !muted
        service?.setMuted(next)
        muted = next
    }
    fun toggleSpeaker() {
        val next = !speaker
        service?.setAudioRoute(if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        speaker = next
    }
}
