package com.vinit.pane

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

/**
 * Helpers for requesting Android's official default phone-app role.
 * The user must approve the system dialog; this code never silently changes the role.
 */
object DialerRoleHelper {
    fun isDefaultDialer(activity: Activity): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            activity.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage == activity.packageName
        }
    }.getOrDefault(false)

    fun createRequestIntent(activity: Activity): Intent? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles?.isRoleAvailable(RoleManager.ROLE_DIALER) == true &&
                !roles.isRoleHeld(RoleManager.ROLE_DIALER)) {
                roles.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else null
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
        }
    }.getOrNull()
}
