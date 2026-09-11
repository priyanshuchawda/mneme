package com.mneme.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Optional, user-enabled bridge for context from the foreground app. */
class RetenqoAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != null) lastPackage = event.packageName.toString()
    }

    override fun onInterrupt() = Unit

    companion object {
        @Volatile var lastPackage: String? = null
            private set
    }
}
