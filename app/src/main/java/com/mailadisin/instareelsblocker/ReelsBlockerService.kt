package com.mailadisin.instareelsblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences

    // Reels-related labels Instagram uses for the bottom nav tab (varies by app version/locale)
    private val reelsKeywords = listOf("reels", "reel")

    override fun onServiceConnected() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = arrayOf(INSTAGRAM_PACKAGE)
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isBlockerEnabled()) return
        if (event.packageName != INSTAGRAM_PACKAGE) return

        val root = rootInActiveWindow ?: return
        try {
            if (isReelsTabActive(root)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                Toast.makeText(this, "Reels blocked 🚫", Toast.LENGTH_SHORT).show()
            }
        } finally {
            root.recycle()
        }
    }

    private fun isReelsTabActive(root: AccessibilityNodeInfo): Boolean {
        // Walk the tree looking for a selected bottom-nav node whose description matches "reels"
        return findSelectedReelsNode(root)
    }

    private fun findSelectedReelsNode(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isReelsLabel = reelsKeywords.any { keyword ->
            desc.contains(keyword) || text.contains(keyword)
        }

        if (isReelsLabel && (node.isSelected || node.isChecked || node.isFocused)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findSelectedReelsNode(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun isBlockerEnabled(): Boolean =
        prefs.getBoolean(KEY_ENABLED, true)

    override fun onInterrupt() = Unit

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val PREFS_NAME = "reels_blocker_prefs"
        const val KEY_ENABLED = "blocker_enabled"
    }
}
