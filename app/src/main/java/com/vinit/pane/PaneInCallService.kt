package com.vinit.pane

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/** Telecom-bound call service. Android supplies the Call objects it may control. */
class PaneInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        PaneCallController.attach(call, this)

        // When Pane is the approved default phone app, reject a number explicitly placed
        // in the user's local block list before presenting the incoming-call screen.
        val number = call.details.handle?.schemeSpecificPart.orEmpty()
        val normalized = number.filter { it.isDigit() }
        val blocked = getSharedPreferences("pane_settings", MODE_PRIVATE)
            .getStringSet("blocked_numbers", emptySet()).orEmpty()
            .any { candidate ->
                val stored = candidate.filter { it.isDigit() }
                stored.isNotBlank() && normalized.isNotBlank() &&
                    (stored == normalized || stored.takeLast(10) == normalized.takeLast(10))
            }
        if (call.state == Call.STATE_RINGING && blocked) {
            runCatching { call.reject(false, null) }
            return
        }

        runCatching {
            startActivity(Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }

    override fun onCallRemoved(call: Call) {
        PaneCallController.detach(call)
        super.onCallRemoved(call)
    }
}
