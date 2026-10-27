package com.vinit.pane

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/** Telecom-bound call service. Android supplies the Call objects it may control. */
class PaneInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        PaneCallController.attach(call, this)
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
